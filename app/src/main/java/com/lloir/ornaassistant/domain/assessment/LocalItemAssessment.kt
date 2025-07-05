package com.lloir.ornaassistant.domain.assessment

import android.util.Log
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.repository.ItemAssessmentRepository
import com.lloir.ornaassistant.utils.EnhancedQualityCalculator
import com.lloir.ornaassistant.utils.QualityCalculator
import com.lloir.ornaassistant.domain.assessment.EnhancedItemDatabase

data class ItemBaseStats(
    val name: String,
    val isBossItem: Boolean,
    val baseStats: Map<String, Int>, // Base stats at level 1
    val tier: Int
)

/**
 * Simple database of item information
 * This is a placeholder that could be expanded with actual item data
 */
object ItemDatabase {
    private val knownItems = listOf<ItemBaseStats>(
        // This would be populated with known items and their base stats
        // For now, it's empty as we're using heuristics
    )

    /**
     * Find an item by partial name match
     */
    fun findItemByPartialName(itemName: String): ItemBaseStats? {
        return knownItems.firstOrNull { 
            itemName.contains(it.name, ignoreCase = true) || 
            it.name.contains(itemName, ignoreCase = true) 
        }
    }

    /**
     * Estimate base stats for an item of a given tier
     */
    fun estimateBaseStatsForTier(tier: Int, isBossItem: Boolean): Map<String, Int> {
        // Base multiplier increases with tier
        val baseMultiplier = when {
            tier <= 1 -> 1
            tier <= 3 -> 2
            tier <= 5 -> 3
            tier <= 7 -> 4
            else -> 5
        }

        // Boss items have higher base stats
        val bossMultiplier = if (isBossItem) 1.5 else 1.0

        // Return estimated base stats for common attributes
        return mapOf(
            "Att" to (10 * tier * baseMultiplier * bossMultiplier).toInt(),
            "Mag" to (10 * tier * baseMultiplier * bossMultiplier).toInt(),
            "Def" to (8 * tier * baseMultiplier * bossMultiplier).toInt(),
            "Res" to (8 * tier * baseMultiplier * bossMultiplier).toInt(),
            "HP" to (20 * tier * baseMultiplier * bossMultiplier).toInt(),
            "Mana" to (10 * tier * baseMultiplier * bossMultiplier).toInt(),
            "Dex" to (5 * tier * baseMultiplier * bossMultiplier).toInt(),
            "Ward" to (2 * tier * baseMultiplier * bossMultiplier).toInt(),
            "Crit" to (1 * tier * baseMultiplier * bossMultiplier).toInt()
        )
    }
}

class LocalItemAssessment {
    companion object {
        private const val TAG = "LocalItemAssessment"

        // Growth rates per level
        private const val STANDARD_GROWTH = 0.10  // 10%
        private const val BOSS_GROWTH = 0.125     // 12.5%

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
     * Calculate base stat from current level and stat value
     */
    private fun calculateBaseStat(currentStat: Int, currentLevel: Int, isBossItem: Boolean, statName: String = ""): Int {
        // Crit doesn't scale with level in Orna
        if (statName == "Crit") {
            return currentStat
        }

        val growthRate = if (isBossItem) BOSS_GROWTH else STANDARD_GROWTH
        val levelMultiplier = 1.0 + (growthRate * (currentLevel - 1))
        return (currentStat / levelMultiplier).toInt()
    }

    /**
     * Calculate stat at specific level from base stat
     */
    private fun calculateStatAtLevel(baseStat: Int, targetLevel: Int, isBossItem: Boolean, statName: String = ""): Int {
        // Crit doesn't scale with level in Orna
        if (statName == "Crit") {
            return baseStat
        }

        val growthRate = if (isBossItem) BOSS_GROWTH else STANDARD_GROWTH
        val levelMultiplier = 1.0 + (growthRate * (targetLevel - 1))
        return (baseStat * levelMultiplier).toInt()
    }

    /**
     * Calculate quality percentage based on actual vs expected base stats
     * This is a wrapper around the QualityCalculator for single stat calculation
     */
    private fun calculateQuality(actualBaseStat: Int, expectedBaseStat: Int): Double {
        val actualStats = mapOf("stat" to actualBaseStat)
        val expectedStats = mapOf("stat" to expectedBaseStat)

        val (quality, _) = QualityCalculator.calculateQuality(actualStats, expectedStats)
        return quality
    }

    /**
     * Assess item locally without API call
     * 
     * Uses the enhanced calculation model based on community observations:
     * 1. Base stats are multiplied by rarity multipliers
     * 2. Quality percentage is applied to the rarity-adjusted stats
     * 3. Blacksmith upgrade multipliers are applied to the quality-adjusted stats
     * 4. Adornment bonuses are added to the final stats (not implemented yet)
     */
    fun assessItemLocally(
        itemName: String,
        level: Int,
        attributes: Map<String, Int>,
        anguishLevel: Int = 0,
        isCelestialWeapon: Boolean = false,
        isTwoHanded: Boolean = false,
        isOffHand: Boolean = false
    ): AssessmentResult {

        Log.d(TAG, "Starting local assessment for: $itemName (level $level)")
        Log.d(TAG, "Attributes: $attributes")
        if (anguishLevel > 0) {
            Log.d(TAG, "Anguish level: $anguishLevel")
        }
        if (isCelestialWeapon) {
            Log.d(TAG, "Item is a Celestial Weapon" + if (isTwoHanded) " (two-handed)" else "")
        }
        if (isOffHand) {
            Log.d(TAG, "Item is an off-hand item")
        }

        // Try to find item in database
        val knownItem = EnhancedItemDatabase.findItemByPartialName(itemName)
        val isBossItem = knownItem?.isBossItem ?: detectBossItem(itemName)
        val tier = knownItem?.tier ?: estimateTierFromName(itemName)

        // Extract rarity and upgrade level from item name
        val rarity = extractRarityFromName(itemName)
        val upgradeLevel = extractUpgradeLevelFromName(itemName)

        Log.d(TAG, "Item lookup: known=${knownItem != null}, boss=$isBossItem, tier=$tier")
        Log.d(TAG, "Item properties: rarity=$rarity, upgradeLevel=$upgradeLevel")

        val assessedStats = mutableMapOf<String, List<String>>()
        var totalQuality = 0.0
        var statCount = 0

        // Determine item type based on attributes
        val isMagicWeapon = (attributes["Mag"] ?: 0) > (attributes["Att"] ?: 0) || 
                           (attributes.containsKey("Mana") && !attributes.containsKey("Att"))
        val isDefensiveItem = (attributes["Def"] ?: 0) > 0 || (attributes["Res"] ?: 0) > 0 || 
                             (attributes.containsKey("HP") && (attributes["HP"] ?: 0) > 0 && 
                              (attributes["Att"] ?: 0) == 0 && (attributes["Mag"] ?: 0) == 0)

        // Define relevant stats for each item type
        val relevantStats = when {
            isMagicWeapon -> setOf("Mag", "Mana", "Ward")
            isDefensiveItem -> setOf("Def", "Res", "Ward", "HP")
            else -> setOf("Att", "Dex", "Ward", "Crit")
        }

        // Log item type and relevant stats
        val itemType = when {
            isMagicWeapon -> "Magic Weapon"
            isDefensiveItem -> "Defensive Item"
            else -> "Physical Weapon"
        }
        Log.d(TAG, "Item type detected: $itemType")
        Log.d(TAG, "Relevant stats for quality calculation: $relevantStats")

        // Process each stat
        attributes.forEach { (statName, currentValue) ->
            if (currentValue > 0) {
                // Calculate base stat from current level
                val actualBaseStat = calculateBaseStat(currentValue, level, isBossItem, statName)

                // Get expected base stat for quality calculation
                val expectedBaseStat = getExpectedBaseStat(knownItem, statName, tier, isBossItem)

                // Calculate quality percentage using the enhanced calculator
                val qualityPercentage = EnhancedQualityCalculator.estimateQualityPercentage(
                    actualStat = currentValue,
                    baseStat = expectedBaseStat,
                    rarity = rarity,
                    upgradeLevel = upgradeLevel
                )

                // Cap quality percentage at 200% (allow exceptional items to show higher quality)
                val cappedQualityPercentage = minOf(qualityPercentage, 200.0)

                // Calculate stats at different upgrade levels using the enhanced calculator
                val tenStarBaseStat = calculateStatAtLevel(expectedBaseStat, 10, isBossItem, statName)

                // Calculate base stats without anguish
                val tenStarStatBase = EnhancedQualityCalculator.calculateFinalStat(
                    baseStat = tenStarBaseStat,
                    rarity = rarity,
                    qualityPercentage = cappedQualityPercentage,
                    upgradeLevel = null
                )

                val mfStatBase = EnhancedQualityCalculator.calculateFinalStat(
                    baseStat = tenStarBaseStat,
                    rarity = rarity,
                    qualityPercentage = cappedQualityPercentage,
                    upgradeLevel = "MF"
                )

                val dfStatBase = EnhancedQualityCalculator.calculateFinalStat(
                    baseStat = tenStarBaseStat,
                    rarity = rarity,
                    qualityPercentage = cappedQualityPercentage,
                    upgradeLevel = "DF"
                )

                val gfStatBase = EnhancedQualityCalculator.calculateFinalStat(
                    baseStat = tenStarBaseStat,
                    rarity = rarity,
                    qualityPercentage = cappedQualityPercentage,
                    upgradeLevel = "GF"
                )

                // Apply anguish bonuses if applicable
                val tenStarStat = calculateStatWithAnguish(tenStarStatBase, anguishLevel)
                val mfStat = calculateStatWithAnguish(mfStatBase, anguishLevel)
                val dfStat = calculateStatWithAnguish(dfStatBase, anguishLevel)
                val gfStat = calculateStatWithAnguish(gfStatBase, anguishLevel)

                assessedStats[statName] = listOf(
                    tenStarStat.toString(),
                    mfStat.toString(),
                    dfStat.toString(),
                    gfStat.toString()
                )

                // Only include relevant stats in quality calculation
                if (statName in relevantStats) {
                    totalQuality += (cappedQualityPercentage / 100.0)
                    statCount++
                }

                Log.d(TAG, "Stat $statName: current=$currentValue, base=$actualBaseStat, expected=$expectedBaseStat, quality=${cappedQualityPercentage}%")
                Log.d(TAG, "Projected stats: 10★=$tenStarStat, MF=$mfStat, DF=$dfStat, GF=$gfStat")
            }
        }

        // Average quality across all stats
        var finalQuality = if (statCount > 0) totalQuality / statCount else 1.0

        // Cap quality at 191% for non-boss items
        if (!isBossItem && finalQuality > 1.91) {
            finalQuality = 1.91
            Log.d(TAG, "Quality capped at 191% for non-boss item")
        }

        // Log which stats were included in the quality calculation
        Log.d(TAG, "Quality calculation included $statCount relevant stats with a total quality of ${String.format("%.2f", totalQuality)}")
        Log.d(TAG, "Final quality: ${String.format("%.2f", finalQuality * 100)}%")

        // Calculate material requirements
        val materials = listOf(
            TEN_STAR_MATERIALS,
            (MF_MATERIAL_BASE * finalQuality).toInt(),
            (DF_MATERIAL_BASE * finalQuality).toInt(),
            0 // GF materials vary significantly
        )

        Log.d(TAG, "Local assessment complete. Quality: ${String.format("%.3f", finalQuality)}")

        // Calculate adornment slots
        if (isCelestialWeapon) {
            // Celestial weapons have special adornment slot patterns
            val adornmentSlots = getAdornmentSlotsForCelestial(level.coerceAtMost(20), isTwoHanded)
            val adornmentSlotsList = List(4) { adornmentSlots.toString() }
            assessedStats["adornment_slots"] = adornmentSlotsList
            Log.d(TAG, "Celestial weapon adornment slots: $adornmentSlots")
        } else {
            // Regular items
            val baseSlots = 1 // Default base slot

            if (isOffHand) {
                // Off-hand items have fixed slots
                val slotsList = List(4) { baseSlots.toString() }
                assessedStats["adornment_slots"] = slotsList
                Log.d(TAG, "Off-hand item adornment slots: $baseSlots")
            } else {
                // Regular items get additional slots based on quality
                val additionalSlots = getAdditionalSlots(finalQuality)

                if (anguishLevel > 0) {
                    // Anguish items always get 4 additional slots
                    val totalSlots = baseSlots + 4
                    val slotsList = List(4) { totalSlots.toString() }
                    assessedStats["adornment_slots"] = slotsList
                    Log.d(TAG, "Anguish item adornment slots: $totalSlots")
                } else {
                    // Normal items get slots based on quality and level
                    val level10Slots = baseSlots + additionalSlots
                    val mfSlots = baseSlots + 3
                    val dfSlots = baseSlots + 4
                    val gfSlots = baseSlots + 4

                    val slotsList = listOf(
                        level10Slots.toString(),
                        mfSlots.toString(),
                        dfSlots.toString(),
                        gfSlots.toString()
                    )

                    assessedStats["adornment_slots"] = slotsList
                    Log.d(TAG, "Regular item adornment slots: 10★=$level10Slots, MF=$mfSlots, DF=$dfSlots, GF=$gfSlots")
                }
            }
        }

        return AssessmentResult(
            quality = finalQuality,
            stats = assessedStats,
            materials = materials,
            anguishLevel = anguishLevel
        )
    }

    /**
     * Calculate stat value at specific forge level
     */
    private fun calculateForgeLevel(tenStarStat: Int, forgeType: String): Int {
        return when (forgeType) {
            "MF" -> (tenStarStat * 1.2).toInt()  // 20% increase
            "DF" -> (tenStarStat * 1.35).toInt() // 35% increase
            "GF" -> (tenStarStat * 1.5).toInt()  // 50% increase
            else -> tenStarStat
        }
    }

    /**
     * Calculate stat with anguish level bonus
     * Anguish adds 3% per level to stats
     */
    private fun calculateStatWithAnguish(stat: Int, anguishLevel: Int): Int {
        if (anguishLevel <= 0) return stat
        return stat + (stat * 0.03f * anguishLevel).toInt()
    }

    /**
     * Get adornment slots for celestial weapon based on level
     * Celestial weapons have a special pattern of adornment slots that changes with level
     */
    private fun getAdornmentSlotsForCelestial(level: Int, isTwoHanded: Boolean): Int {
        val baseSlots = if (level <= 20) CELESTIAL_WEAPON_SLOTS[level - 1] else 5
        return if (isTwoHanded) baseSlots + 1 else baseSlots
    }

    /**
     * Get additional adornment slots based on quality
     * 170%+ quality: +2 slots
     * 100-170% quality: +1 slot
     * 70-100% quality: +0 slots
     * <70% quality: -1 slot
     */
    private fun getAdditionalSlots(quality: Double): Int {
        val qualityPercent = quality * 100
        return when {
            qualityPercent >= 170 -> 2
            qualityPercent > 100 -> 1
            qualityPercent > 70 -> 0
            else -> -1
        }
    }

    /**
     * Get expected base stat for an item
     */
    private fun getExpectedBaseStat(knownItem: ItemBaseStats?, statName: String, tier: Int, isBossItem: Boolean): Int {
        // If we have the item in database, use its known base stats
        knownItem?.baseStats?.get(statName)?.let { return it }

        // Otherwise estimate based on tier
        val estimatedStats = ItemDatabase.estimateBaseStatsForTier(tier, isBossItem)
        return estimatedStats[statName] ?: 0
    }

    /**
     * Detect if item is a boss item based on name patterns
     */
    private fun detectBossItem(itemName: String): Boolean {
        val bossKeywords = listOf(
            "boss", "raid", "world", "kingdom", "arisen", "nothren", "apollyon",
            "frostforged", "shadowforged", "crimson", "gilded", "ancient",
            "legendary", "mythic", "divine", "cursed", "blessed"
        )

        return bossKeywords.any { keyword ->
            itemName.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * Estimate item tier from name patterns
     */
    private fun estimateTierFromName(itemName: String): Int {
        // Tier keywords in roughly ascending order
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

            else -> 5 // Default to tier 5 for unknown items
        }
    }

    /**
     * Extract rarity from item name
     * Returns "Common" if no rarity prefix is found
     */
    private fun extractRarityFromName(itemName: String): String {
        // First check for exact matches with known rarities
        for (rarity in RARITIES) {
            if (itemName.startsWith(rarity, ignoreCase = true)) {
                Log.d(TAG, "Found rarity prefix in item name: $rarity")
                return rarity
            }
        }

        // Check for upgrade levels which imply Ornate
        val upgradeKeywords = listOf("Masterforged", "Demonforged", "Godforged")
        for (upgrade in upgradeKeywords) {
            if (itemName.startsWith(upgrade, ignoreCase = true)) {
                Log.d(TAG, "Found upgrade prefix in item name: $upgrade, assuming Ornate rarity")
                return "Ornate"
            }
        }

        // Check for partial matches or misspellings
        for (rarity in RARITIES) {
            if (itemName.contains(rarity, ignoreCase = true)) {
                Log.d(TAG, "Found partial rarity match in item name: $rarity")
                return rarity
            }
        }

        Log.d(TAG, "No rarity found in item name, defaulting to Common")
        return "Common" // Default rarity if no prefix is found
    }

    /**
     * Extract upgrade level from item name
     * Returns null if no upgrade level is found
     */
    private fun extractUpgradeLevelFromName(itemName: String): String? {
        // Check for exact matches at the start of the name
        if (itemName.startsWith("Masterforged", ignoreCase = true)) {
            Log.d(TAG, "Found Masterforged upgrade in item name")
            return "MF"
        }
        if (itemName.startsWith("Demonforged", ignoreCase = true)) {
            Log.d(TAG, "Found Demonforged upgrade in item name")
            return "DF"
        }
        if (itemName.startsWith("Godforged", ignoreCase = true)) {
            Log.d(TAG, "Found Godforged upgrade in item name")
            return "GF"
        }

        // Check for partial matches or abbreviations
        val lowerName = itemName.lowercase()
        if (lowerName.contains("masterforged") || lowerName.contains("mf")) {
            Log.d(TAG, "Found Masterforged reference in item name")
            return "MF"
        }
        if (lowerName.contains("demonforged") || lowerName.contains("df")) {
            Log.d(TAG, "Found Demonforged reference in item name")
            return "DF"
        }
        if (lowerName.contains("godforged") || lowerName.contains("gf")) {
            Log.d(TAG, "Found Godforged reference in item name")
            return "GF"
        }

        Log.d(TAG, "No upgrade level found in item name")
        return null
    }

}

/**
 * Extension function to integrate with existing repository
 */
suspend fun ItemAssessmentRepository.assessItemLocally(
    itemName: String,
    level: Int,
    attributes: Map<String, Int>,
    anguishLevel: Int = 0,
    isCelestialWeapon: Boolean = false,
    isTwoHanded: Boolean = false,
    isOffHand: Boolean = false
): AssessmentResult {
    val localAssessment = LocalItemAssessment()
    return localAssessment.assessItemLocally(
        itemName = itemName,
        level = level,
        attributes = attributes,
        anguishLevel = anguishLevel,
        isCelestialWeapon = isCelestialWeapon,
        isTwoHanded = isTwoHanded,
        isOffHand = isOffHand
    )
}
