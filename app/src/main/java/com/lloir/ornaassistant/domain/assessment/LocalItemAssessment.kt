package com.lloir.ornaassistant.domain.assessment

import android.util.Log
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.repository.ItemAssessmentRepository

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
            "Attack" to (10 * tier * baseMultiplier * bossMultiplier).toInt(),
            "Magic" to (10 * tier * baseMultiplier * bossMultiplier).toInt(),
            "Defense" to (8 * tier * baseMultiplier * bossMultiplier).toInt(),
            "Resistance" to (8 * tier * baseMultiplier * bossMultiplier).toInt(),
            "HP" to (20 * tier * baseMultiplier * bossMultiplier).toInt(),
            "Mana" to (10 * tier * baseMultiplier * bossMultiplier).toInt(),
            "Dexterity" to (5 * tier * baseMultiplier * bossMultiplier).toInt(),
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
    }

    /**
     * Calculate base stat from current level and stat value
     */
    private fun calculateBaseStat(currentStat: Int, currentLevel: Int, isBossItem: Boolean): Int {
        val growthRate = if (isBossItem) BOSS_GROWTH else STANDARD_GROWTH
        val levelMultiplier = 1.0 + (growthRate * (currentLevel - 1))
        return (currentStat / levelMultiplier).toInt()
    }

    /**
     * Calculate stat at specific level from base stat
     */
    private fun calculateStatAtLevel(baseStat: Int, targetLevel: Int, isBossItem: Boolean): Int {
        val growthRate = if (isBossItem) BOSS_GROWTH else STANDARD_GROWTH
        val levelMultiplier = 1.0 + (growthRate * (targetLevel - 1))
        return (baseStat * levelMultiplier).toInt()
    }

    /**
     * Calculate quality percentage based on actual vs expected base stats
     */
    private fun calculateQuality(actualBaseStat: Int, expectedBaseStat: Int): Double {
        return if (expectedBaseStat > 0) {
            actualBaseStat.toDouble() / expectedBaseStat.toDouble()
        } else {
            1.0
        }
    }

    /**
     * Assess item locally without API call
     */
    fun assessItemLocally(
        itemName: String,
        level: Int,
        attributes: Map<String, Int>
    ): AssessmentResult {

        Log.d(TAG, "Starting local assessment for: $itemName (level $level)")
        Log.d(TAG, "Attributes: $attributes")

        // Try to find item in database
        val knownItem = ItemDatabase.findItemByPartialName(itemName)
        val isBossItem = knownItem?.isBossItem ?: detectBossItem(itemName)
        val tier = knownItem?.tier ?: estimateTierFromName(itemName)

        Log.d(TAG, "Item lookup: known=${knownItem != null}, boss=$isBossItem, tier=$tier")

        val assessedStats = mutableMapOf<String, List<String>>()
        var totalQuality = 0.0
        var statCount = 0

        // Process each stat
        attributes.forEach { (statName, currentValue) ->
            if (currentValue > 0) {
                // Calculate base stat from current level
                val actualBaseStat = calculateBaseStat(currentValue, level, isBossItem)

                // Get expected base stat for quality calculation
                val expectedBaseStat = getExpectedBaseStat(knownItem, statName, tier, isBossItem)

                // Calculate quality for this stat
                val statQuality = calculateQuality(actualBaseStat, expectedBaseStat)

                // Calculate forge level stats using actual base stat
                val tenStarStat = calculateStatAtLevel(actualBaseStat, 10, isBossItem)
                val mfStat = calculateForgeLevel(tenStarStat, "MF")
                val dfStat = calculateForgeLevel(tenStarStat, "DF")
                val gfStat = calculateForgeLevel(tenStarStat, "GF")

                assessedStats[statName] = listOf(
                    tenStarStat.toString(),
                    mfStat.toString(),
                    dfStat.toString(),
                    gfStat.toString()
                )

                totalQuality += statQuality
                statCount++

                Log.d(TAG, "Stat $statName: current=$currentValue, base=$actualBaseStat, expected=$expectedBaseStat, quality=$statQuality")
            }
        }

        // Average quality across all stats
        val finalQuality = if (statCount > 0) totalQuality / statCount else 1.0

        // Calculate material requirements
        val materials = listOf(
            TEN_STAR_MATERIALS,
            (MF_MATERIAL_BASE * finalQuality).toInt(),
            (DF_MATERIAL_BASE * finalQuality).toInt(),
            0 // GF materials vary significantly
        )

        Log.d(TAG, "Local assessment complete. Quality: ${String.format("%.3f", finalQuality)}")

        return AssessmentResult(
            quality = finalQuality,
            stats = assessedStats,
            materials = materials
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

}

/**
 * Extension function to integrate with existing repository
 */
suspend fun ItemAssessmentRepository.assessItemLocally(
    itemName: String,
    level: Int,
    attributes: Map<String, Int>
): AssessmentResult {
    val localAssessment = LocalItemAssessment()
    return localAssessment.assessItemLocally(itemName, level, attributes)
}
