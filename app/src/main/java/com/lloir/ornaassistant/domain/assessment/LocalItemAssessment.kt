package com.lloir.ornaassistant.domain.assessment

import android.util.Log
import kotlin.math.abs
import kotlin.math.pow
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.repository.ItemAssessmentRepository
import com.lloir.ornaassistant.utils.OrnaCalculator

/**
 * Data class for item base stats from database
 */
data class ItemBaseStats(
    val name: String,
    val isBossItem: Boolean,
    val baseStats: Map<String, Int>, // Base stats at level 1
    val tier: Int
)

/**
 * Local item assessment using PERFECT formulas from ORNA STAT CALC.ods
 *
 * This implementation uses the exact formulas extracted from the official
 * calculator spreadsheet for accurate item quality assessment.
 */
class LocalItemAssessment(
    private val itemDatabase: com.lloir.ornaassistant.domain.repository.ItemDatabase
) {
    companion object {
        private const val TAG = "LocalItemAssessment"

        // Material costs for forging
        private const val TEN_STAR_MATERIALS = 135
        private const val MF_MATERIAL_BASE = 300
        private const val DF_MATERIAL_BASE = 666

        // Item rarities
        private val RARITIES = listOf(
            "Broken", "Poor", "Common", "Superior", "Famed", "Legendary", "Ornate"
        )

        // Celestial weapon adornment slots by level (1-20)
        private val CELESTIAL_WEAPON_SLOTS = listOf(1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 5)
    }

    /**
     * Assess item locally using PERFECT formulas
     */
    fun assessItemLocally(
        itemName: String,
        level: Int,
        attributes: Map<String, Int>,
        adornmentValues: Map<String, Int> = emptyMap(),
        originalItemName: String = itemName,
        anguishLevel: Int = 0,
        isCelestialWeapon: Boolean = false,
        isTwoHanded: Boolean = false,
        isOffHand: Boolean = false
    ): AssessmentResult {

        Log.d(TAG, "Starting local assessment for: $itemName (level $level)")
        Log.d(TAG, "Attributes: $attributes")

        // Try to find item in database
        val knownItem = itemDatabase.findItemByPartialName(itemName)
        val isBossItem = knownItem?.isBossItem ?: detectBossItem(itemName)
        val tier = knownItem?.tier ?: estimateTierFromName(itemName)

        // Extract rarity and upgrade level from item name
        val rarity = extractRarityFromName(originalItemName)
        val upgradeLevel = extractUpgradeLevelFromName(originalItemName)

        Log.d(TAG, "Item properties: boss=$isBossItem, tier=$tier, rarity=$rarity, upgrade=$upgradeLevel")

        val assessedStats = mutableMapOf<String, List<String>>()
        var totalQuality = 0.0
        var statCount = 0

        // Determine item type and relevant stats for quality calculation
        val itemType = determineItemType(itemName, attributes)
        val relevantStats = getRelevantStatsForType(itemType)

        Log.d(TAG, "Item type: $itemType, relevant stats: $relevantStats")

        // Process each attribute
        attributes.forEach { (statName, currentValue) ->
            if (currentValue != 0) {
                // Remove adornment values to get base item stat
                val adornmentValue = adornmentValues[statName] ?: 0
                val baseItemStat = currentValue - adornmentValue

                // Get expected base stat from database or estimate
                val expectedBaseStat = knownItem?.baseStats?.get(statName)
                    ?: estimateBaseStatForTier(statName, tier, isBossItem)

                if (expectedBaseStat > 0) {
                    // Calculate quality using PERFECT formula
                    // For level 1 items, quality = actual / expected
                    // For higher levels, we need to reverse the level scaling
                    val quality = if (level == 1) {
                        baseItemStat.toDouble() / expectedBaseStat.toDouble()
                    } else {
                        // Use OrnaCalculator to reverse engineer quality
                        OrnaCalculator.estimateQuality(
                            actualStat = baseItemStat.toDouble(),
                            baseStat = expectedBaseStat,
                            isBoss = isBossItem,
                            upgradeLevel = "1", // Base calculation
                            isWard = statName.equals("Ward", ignoreCase = true)
                        )
                    }

                    // Calculate projected stats at different upgrade levels
                    val stats10 = OrnaCalculator.calculateFinalStat(expectedBaseStat, isBossItem, "10", quality)
                    val statsMF = OrnaCalculator.calculateFinalStat(expectedBaseStat, isBossItem, "MF", quality)
                    val statsDF = OrnaCalculator.calculateFinalStat(expectedBaseStat, isBossItem, "DF", quality)
                    val statsGF = OrnaCalculator.calculateFinalStat(expectedBaseStat, isBossItem, "GF", quality)

                    // Apply anguish bonuses if applicable
                    val final10 = applyAnguishBonus(stats10, anguishLevel)
                    val finalMF = applyAnguishBonus(statsMF, anguishLevel)
                    val finalDF = applyAnguishBonus(statsDF, anguishLevel)
                    val finalGF = applyAnguishBonus(statsGF, anguishLevel)

                    assessedStats[statName] = listOf(
                        final10.toInt().toString(),
                        finalMF.toInt().toString(),
                        finalDF.toInt().toString(),
                        finalGF.toInt().toString()
                    )

                    // Include in quality calculation if it's a relevant stat
                    if (statName in relevantStats) {
                        totalQuality += quality
                        statCount++
                        Log.d(TAG, "Including $statName in quality: ${quality * 100}%")
                    }
                }
            }
        }

        // Calculate average quality
        val finalQuality = if (statCount > 0) totalQuality / statCount else 1.0

        Log.d(TAG, "Final quality: ${String.format("%.2f", finalQuality * 100)}%")

        // Calculate material requirements
        val materials = listOf(
            TEN_STAR_MATERIALS,
            (MF_MATERIAL_BASE * finalQuality).toInt(),
            (DF_MATERIAL_BASE * finalQuality).toInt(),
            0 // GF materials vary
        )

        // Calculate adornment slots
        calculateAdornmentSlots(
            assessedStats, finalQuality, anguishLevel,
            isCelestialWeapon, isTwoHanded, isOffHand, level
        )

        return AssessmentResult(
            quality = finalQuality,
            stats = assessedStats,
            materials = materials,
            anguishLevel = anguishLevel
        )
    }

    /**
     * Determine item type based on name and stats
     */
    private fun determineItemType(itemName: String, attributes: Map<String, Int>): ItemType {
        val att = attributes["Att"] ?: 0
        val mag = attributes["Mag"] ?: 0
        val def = attributes["Def"] ?: 0
        val res = attributes["Res"] ?: 0

        // Check name patterns first
        if (detectShield(itemName)) return ItemType.SHIELD
        if (itemName.contains("staff", ignoreCase = true) ||
            itemName.contains("wand", ignoreCase = true) ||
            itemName.contains("tome", ignoreCase = true)) {
            return ItemType.MAGIC_WEAPON
        }

        // Check stats
        return when {
            mag > att && mag > 0 -> ItemType.MAGIC_WEAPON
            att > 0 && att > (def + res) -> ItemType.PHYSICAL_WEAPON
            (def > 0 || res > 0) && att == 0 && mag == 0 -> ItemType.ARMOR
            else -> ItemType.ACCESSORY
        }
    }

    /**
     * Get relevant stats for quality calculation based on item type
     */
    private fun getRelevantStatsForType(itemType: ItemType): Set<String> {
        return when (itemType) {
            ItemType.PHYSICAL_WEAPON -> setOf("Att", "Dex", "Crit")
            ItemType.MAGIC_WEAPON -> setOf("Mag", "Mana")
            ItemType.ARMOR, ItemType.SHIELD -> setOf("Def", "Res", "HP")
            ItemType.ACCESSORY -> setOf("HP", "Mana", "Ward") // Use all stats present
        }
    }

    /**
     * Apply anguish level bonus (3% per level)
     */
    private fun applyAnguishBonus(stat: Double, anguishLevel: Int): Double {
        if (anguishLevel <= 0) return stat
        return stat * (1 + 0.03 * anguishLevel)
    }

    /**
     * Calculate adornment slots based on quality and item type
     */
    private fun calculateAdornmentSlots(
        assessedStats: MutableMap<String, List<String>>,
        quality: Double,
        anguishLevel: Int,
        isCelestialWeapon: Boolean,
        isTwoHanded: Boolean,
        isOffHand: Boolean,
        level: Int
    ) {
        when {
            isCelestialWeapon -> {
                val slots = getAdornmentSlotsForCelestial(level.coerceAtMost(20), isTwoHanded)
                assessedStats["adornment_slots"] = List(4) { slots.toString() }
            }
            anguishLevel > 0 -> {
                // Anguish items always get 5 slots (1 base + 4 additional)
                assessedStats["adornment_slots"] = List(4) { "5" }
            }
            isOffHand -> {
                // Off-hand items have fixed 1 slot
                assessedStats["adornment_slots"] = List(4) { "1" }
            }
            else -> {
                // Regular items get slots based on quality
                val baseSlots = 1
                val additionalSlots = getAdditionalSlots(quality)
                val level10Slots = baseSlots + additionalSlots

                assessedStats["adornment_slots"] = listOf(
                    level10Slots.toString(),
                    "4", // MF always gets 4 slots (1 base + 3)
                    "5", // DF always gets 5 slots (1 base + 4)
                    "5"  // GF always gets 5 slots (1 base + 4)
                )
            }
        }
    }

    /**
     * Get adornment slots for celestial weapon based on level
     */
    private fun getAdornmentSlotsForCelestial(level: Int, isTwoHanded: Boolean): Int {
        val baseSlots = if (level in 1..20) CELESTIAL_WEAPON_SLOTS[level - 1] else 5
        return if (isTwoHanded) baseSlots + 1 else baseSlots
    }

    /**
     * Get additional adornment slots based on quality
     */
    private fun getAdditionalSlots(quality: Double): Int {
        val qualityPercent = quality * 100
        return when {
            qualityPercent >= 170 -> 2
            qualityPercent >= 100 -> 1
            qualityPercent >= 70 -> 0
            else -> -1
        }
    }

    /**
     * Estimate base stat for a given tier when not in database
     */
    private fun estimateBaseStatForTier(statName: String, tier: Int, isBossItem: Boolean): Int {
        val bossMultiplier = if (isBossItem) 1.5 else 1.0
        val tierMultiplier = tier * 0.5 + 0.5 // Scales from 1x at T1 to 5.5x at T10

        val baseStat = when (statName) {
            "Att", "Mag" -> 20.0 * tierMultiplier * bossMultiplier
            "Def", "Res" -> 15.0 * tierMultiplier * bossMultiplier
            "HP" -> 50.0 * tierMultiplier * bossMultiplier
            "Mana" -> 30.0 * tierMultiplier * bossMultiplier
            "Dex" -> 10.0 * tierMultiplier * bossMultiplier
            "Ward" -> 5.0 * tierMultiplier * bossMultiplier
            "Crit" -> 3.0 * tierMultiplier * bossMultiplier
            else -> 0.0
        }

        return baseStat.toInt()
    }

    /**
     * Detect if item is a shield
     */
    private fun detectShield(itemName: String): Boolean {
        val shieldKeywords = listOf(
            "shield", "buckler", "targe", "aegis", "ward", "barrier"
        )
        return shieldKeywords.any { itemName.contains(it, ignoreCase = true) }
    }

    /**
     * Detect if item is a boss item based on name patterns
     * Delegates to the standardized ItemUtils implementation
     */
    private fun detectBossItem(itemName: String): Boolean {
        return com.lloir.ornaassistant.utils.ItemUtils.detectBossItem(itemName)
    }

    /**
     * Estimate item tier from name
     */
    private fun estimateTierFromName(itemName: String): Int {
        return when {
            itemName.contains("wooden", ignoreCase = true) ||
                    itemName.contains("crude", ignoreCase = true) -> 1

            itemName.contains("iron", ignoreCase = true) ||
                    itemName.contains("bronze", ignoreCase = true) -> 2

            itemName.contains("steel", ignoreCase = true) ||
                    itemName.contains("silver", ignoreCase = true) -> 3

            itemName.contains("mithril", ignoreCase = true) ||
                    itemName.contains("gold", ignoreCase = true) -> 4

            itemName.contains("adamantine", ignoreCase = true) ||
                    itemName.contains("dragon", ignoreCase = true) -> 5

            itemName.contains("ornate", ignoreCase = true) ||
                    itemName.contains("masterforged", ignoreCase = true) -> 6

            itemName.contains("demonforged", ignoreCase = true) ||
                    itemName.contains("godforged", ignoreCase = true) -> 7

            itemName.contains("arisen", ignoreCase = true) ||
                    itemName.contains("world", ignoreCase = true) -> 10

            else -> 5 // Default to tier 5
        }
    }

    /**
     * Extract rarity from item name
     */
    private fun extractRarityFromName(itemName: String): String {
        // Check for exact matches at the start
        for (rarity in RARITIES) {
            if (itemName.startsWith(rarity, ignoreCase = true)) {
                return rarity
            }
        }

        // Check for upgrade prefixes which imply Ornate
        val upgradeKeywords = listOf("Masterforged", "Demonforged", "Godforged")
        for (upgrade in upgradeKeywords) {
            if (itemName.startsWith(upgrade, ignoreCase = true)) {
                return "Ornate"
            }
        }

        return "Common" // Default
    }

    /**
     * Extract upgrade level from item name
     */
    private fun extractUpgradeLevelFromName(itemName: String): String? {
        return when {
            itemName.startsWith("Masterforged", ignoreCase = true) -> "MF"
            itemName.startsWith("Demonforged", ignoreCase = true) -> "DF"
            itemName.startsWith("Godforged", ignoreCase = true) -> "GF"
            else -> null
        }
    }

    /**
     * Item type enumeration
     */
    private enum class ItemType {
        PHYSICAL_WEAPON,
        MAGIC_WEAPON,
        ARMOR,
        SHIELD,
        ACCESSORY
    }
}

/**
 * Extension function to integrate with existing repository
 */
suspend fun ItemAssessmentRepository.assessItemLocally(
    itemName: String,
    level: Int,
    attributes: Map<String, Int>,
    originalItemName: String = itemName,
    adornmentValues: Map<String, Int> = emptyMap(),
    anguishLevel: Int = 0,
    isCelestialWeapon: Boolean = false,
    isTwoHanded: Boolean = false,
    isOffHand: Boolean = false,
    itemDatabase: com.lloir.ornaassistant.domain.repository.ItemDatabase
): AssessmentResult {
    val localAssessment = LocalItemAssessment(itemDatabase)
    return localAssessment.assessItemLocally(
        itemName = itemName,
        level = level,
        attributes = attributes,
        adornmentValues = adornmentValues,
        originalItemName = originalItemName,
        anguishLevel = anguishLevel,
        isCelestialWeapon = isCelestialWeapon,
        isTwoHanded = isTwoHanded,
        isOffHand = isOffHand
    )
}
