package com.lloir.ornaassistant.utils

import android.util.Log

/**
 * Enhanced utility for calculating item stats based on base stats, rarity, and quality percentage.
 * 
 * This implementation is based on community observations and testing of how Orna might calculate
 * item stats. The exact formulas used by the game are proprietary and not publicly disclosed.
 */
object EnhancedQualityCalculator {
    private const val TAG = "EnhancedQualityCalc"

    /**
     * Rarity multipliers for different item rarities.
     * These values are estimates based on community observations.
     */
    private val RARITY_MULTIPLIERS = mapOf(
        "Broken" to 0.5,    // Estimated
        "Poor" to 0.75,     // Estimated
        "Common" to 1.0,    // Base multiplier
        "Superior" to 1.2,  // Estimated
        "Famed" to 1.4,     // Estimated
        "Legendary" to 1.7, // Estimated
        "Ornate" to 2.0     // Estimated
    )

    /**
     * Blacksmith upgrade multipliers.
     * These values are estimates based on community observations.
     */
    private val BLACKSMITH_MULTIPLIERS = mapOf(
        "Level1" to 1.05,   // +5% stats
        "Level2" to 1.10,   // +10% stats
        "Level3" to 1.15,   // +15% stats
        "Level4" to 1.20,   // +20% stats
        "Level5" to 1.25,   // +25% stats
        "Level6" to 1.30,   // +30% stats
        "Level7" to 1.35,   // +35% stats
        "Level8" to 1.40,   // +40% stats
        "Level9" to 1.45,   // +45% stats
        "Level10" to 1.50,  // +50% stats
        "MF" to 1.70,       // Masterforged: +70% stats
        "DF" to 2.0,        // Demonforged: +100% stats
        "GF" to 2.5         // Godforged: +150% stats
    )

    /**
     * Calculate the final stat value based on base stat, rarity, quality percentage, and upgrade level.
     * 
     * @param baseStat The base stat value for the item (from baseitem.csv)
     * @param rarity The rarity of the item (Poor, Common, Superior, Famed, Legendary, Ornate)
     * @param qualityPercentage The quality percentage (1-100%)
     * @param upgradeLevel Optional upgrade level (null for no upgrades)
     * @return The calculated final stat value
     */
    fun calculateFinalStat(
        baseStat: Int,
        rarity: String,
        qualityPercentage: Double,
        upgradeLevel: String? = null
    ): Int {
        // Handle edge cases
        if (baseStat <= 0) {
            Log.w(TAG, "Invalid base stat for calculation: $baseStat")
            return 0
        }

        // Ensure quality percentage is within valid range (allow up to 200% for exceptional items)
        val validQualityPercentage = qualityPercentage.coerceIn(1.0, 200.0)

        // Step 1: Apply rarity multiplier
        val rarityMultiplier = RARITY_MULTIPLIERS[rarity] ?: 1.0
        val statAfterRarity = baseStat * rarityMultiplier

        // Step 2: Apply quality percentage
        val qualityMultiplier = validQualityPercentage / 100.0
        val statAfterQuality = statAfterRarity * qualityMultiplier

        // Step 3: Apply blacksmith upgrade multiplier if applicable
        val finalStat = if (upgradeLevel != null) {
            val upgradeMultiplier = BLACKSMITH_MULTIPLIERS[upgradeLevel] ?: 1.0
            statAfterQuality * upgradeMultiplier
        } else {
            statAfterQuality
        }

        Log.d(TAG, "Stat calculation: base=$baseStat, rarity=$rarity (x$rarityMultiplier), " +
                "quality=$validQualityPercentage% (x$qualityMultiplier), " +
                "upgrade=$upgradeLevel -> final=${finalStat.toInt()}")

        return finalStat.toInt()
    }

    /**
     * Calculate all possible stat values for an item (all rarities, qualities, and upgrades).
     * 
     * @param baseStat The base stat value for the item (from baseitem.csv)
     * @return Map of rarity to map of quality to map of upgrade level to stat value
     */
    fun calculateAllPossibleStats(baseStat: Int): Map<String, Map<Int, Map<String, Int>>> {
        val result = mutableMapOf<String, MutableMap<Int, MutableMap<String, Int>>>()

        // For each rarity
        for (rarity in RARITY_MULTIPLIERS.keys) {
            val qualityMap = mutableMapOf<Int, MutableMap<String, Int>>()

            // For each quality percentage (100%, 95%, 90%, 85%, 80%)
            for (quality in listOf(100, 95, 90, 85, 80)) {
                val upgradeMap = mutableMapOf<String, Int>()

                // Base stat (no upgrades)
                upgradeMap["Base"] = calculateFinalStat(baseStat, rarity, quality.toDouble())

                // For each upgrade level
                for (upgrade in BLACKSMITH_MULTIPLIERS.keys) {
                    upgradeMap[upgrade] = calculateFinalStat(
                        baseStat, rarity, quality.toDouble(), upgrade
                    )
                }

                qualityMap[quality] = upgradeMap
            }

            result[rarity] = qualityMap
        }

        return result
    }

    /**
     * Estimate the quality percentage of an item based on its actual stat and expected base stat.
     * 
     * @param actualStat The actual stat value of the item
     * @param baseStat The base stat value for the item (from baseitem.csv)
     * @param rarity The rarity of the item
     * @param upgradeLevel Optional upgrade level (null for no upgrades)
     * @return Estimated quality percentage (0-100)
     */
    fun estimateQualityPercentage(
        actualStat: Int,
        baseStat: Int,
        rarity: String,
        upgradeLevel: String? = null
    ): Double {
        // Handle edge cases
        if (baseStat <= 0 || actualStat <= 0) {
            Log.w(TAG, "Invalid stats for quality calculation: actual=$actualStat, base=$baseStat")
            return 100.0 // Default to 100% for invalid stats
        }

        // Step 1: Get rarity multiplier
        val rarityMultiplier = RARITY_MULTIPLIERS[rarity] ?: 1.0

        // Step 2: Get upgrade multiplier if applicable
        val upgradeMultiplier = if (upgradeLevel != null) {
            BLACKSMITH_MULTIPLIERS[upgradeLevel] ?: 1.0
        } else {
            1.0
        }

        // Step 3: Calculate expected stat at 100% quality with given rarity and upgrade
        val expectedStatAt100Percent = baseStat * rarityMultiplier * upgradeMultiplier

        // Step 4: Calculate quality percentage
        val qualityPercentage = (actualStat.toDouble() / expectedStatAt100Percent) * 100.0

        // Cap quality percentage between 1% and 200% (allow exceptional items to show higher quality)
        val cappedQualityPercentage = qualityPercentage.coerceIn(1.0, 200.0)

        Log.d(TAG, "Quality estimation: actual=$actualStat, base=$baseStat, " +
                "rarity=$rarity (x$rarityMultiplier), upgrade=$upgradeLevel (x$upgradeMultiplier) " +
                "-> quality=${cappedQualityPercentage.toInt()}%")

        return cappedQualityPercentage
    }

    /**
     * Calculate the final stats for an item with adornments.
     * 
     * @param baseStats Map of base stat names to values
     * @param rarity The rarity of the item
     * @param qualityPercentage The quality percentage (1-100%)
     * @param upgradeLevel Optional upgrade level (null for no upgrades)
     * @param adornments Map of stat names to adornment bonus values
     * @return Map of stat names to final values
     */
    fun calculateFinalStatsWithAdornments(
        baseStats: Map<String, Int>,
        rarity: String,
        qualityPercentage: Double,
        upgradeLevel: String? = null,
        adornments: Map<String, Int> = emptyMap()
    ): Map<String, Int> {
        // Handle edge case of empty base stats
        if (baseStats.isEmpty() && adornments.isEmpty()) {
            Log.w(TAG, "No stats provided for calculation")
            return emptyMap()
        }

        // Ensure quality percentage is within valid range (allow up to 200% for exceptional items)
        val validQualityPercentage = qualityPercentage.coerceIn(1.0, 200.0)

        val finalStats = mutableMapOf<String, Int>()

        // Calculate final stats for each base stat
        for ((statName, baseStat) in baseStats) {
            if (baseStat <= 0) {
                Log.w(TAG, "Skipping invalid base stat for $statName: $baseStat")
                continue
            }

            val finalStatBeforeAdornments = calculateFinalStat(
                baseStat, rarity, validQualityPercentage, upgradeLevel
            )

            // Add adornment bonus if applicable
            val adornmentBonus = adornments[statName] ?: 0
            finalStats[statName] = finalStatBeforeAdornments + adornmentBonus

            Log.d(TAG, "Final stat for $statName: base=$baseStat, final=${finalStats[statName]} (with adornment bonus of $adornmentBonus)")
        }

        // Add any adornment stats that weren't in the base stats
        for ((statName, bonus) in adornments) {
            if (!baseStats.containsKey(statName)) {
                finalStats[statName] = bonus
                Log.d(TAG, "Added adornment-only stat for $statName: $bonus")
            }
        }

        return finalStats
    }
}
