package com.lloir.ornaassistant.utils

import android.util.Log

/**
 * Enhanced utility for calculating item stats based on base stats, rarity, and quality percentage.
 * 
 * @deprecated This class is deprecated and will be removed in a future release.
 * Use [OrnaCalculator] instead, which provides the same functionality with improved implementation.
 */
@Deprecated("Use OrnaCalculator instead", ReplaceWith("OrnaCalculator"))
object EnhancedQualityCalculator {
    private const val TAG = "EnhancedQualityCalc"

    /**
     * Calculate final stat using PERFECT formula from CALC sheet
     * 
     * @param baseStat The base stat value
     * @param isBoss Is this a boss item (gets 25% level bonus)
     * @param upgradeLevel Upgrade level (1-10, MF, DF, GF)
     * @param quality Quality percentage (1.0 = 100%, 2.0 = 200%)
     * @param isWard Special handling for Ward stat
     * @return The calculated final stat value
     * 
     * @deprecated Use [OrnaCalculator.calculateFinalStat] instead
     */
    @Deprecated("Use OrnaCalculator.calculateFinalStat instead", 
                ReplaceWith("OrnaCalculator.calculateFinalStat(baseStat, isBoss, upgradeLevel, quality, isWard)", 
                "com.lloir.ornaassistant.utils.OrnaCalculator"))
    fun calculatePerfectStat(
        baseStat: Int,
        isBoss: Boolean = false,
        upgradeLevel: String = "1", 
        quality: Double = 1.0,
        isWard: Boolean = false
    ): Double {
        return OrnaCalculator.calculateFinalStat(baseStat, isBoss, upgradeLevel, quality, isWard)
    }

    /**
     * Calculate the final stat value based on base stat, rarity, quality percentage, and upgrade level.
     * 
     * @param baseStat The base stat value for the item (from baseitem.csv)
     * @param rarity The rarity of the item (Poor, Common, Superior, Famed, Legendary, Ornate)
     * @param qualityPercentage The quality percentage (1-100%)
     * @param upgradeLevel Optional upgrade level (null for no upgrades)
     * @return The calculated final stat value
     * 
     * @deprecated Use [OrnaCalculator.calculateStatWithRarity] instead
     */
    @Deprecated("Use OrnaCalculator.calculateStatWithRarity instead", 
                ReplaceWith("OrnaCalculator.calculateStatWithRarity(baseStat, rarity, qualityPercentage, upgradeLevel)", 
                "com.lloir.ornaassistant.utils.OrnaCalculator"))
    fun calculateFinalStat(
        baseStat: Int,
        rarity: String,
        qualityPercentage: Double,
        upgradeLevel: String? = null
    ): Int {
        return OrnaCalculator.calculateStatWithRarity(baseStat, rarity, qualityPercentage, upgradeLevel)
    }

    /**
     * Calculate all possible stat values for an item (all rarities, qualities, and upgrades).
     * 
     * @param baseStat The base stat value for the item (from baseitem.csv)
     * @return Map of rarity to map of quality to map of upgrade level to stat value
     * 
     * @deprecated Use [OrnaCalculator.calculateAllPossibleStats] instead
     */
    @Deprecated("Use OrnaCalculator.calculateAllPossibleStats instead", 
                ReplaceWith("OrnaCalculator.calculateAllPossibleStats(baseStat)", 
                "com.lloir.ornaassistant.utils.OrnaCalculator"))
    fun calculateAllPossibleStats(baseStat: Int): Map<String, Map<Int, Map<String, Int>>> {
        return OrnaCalculator.calculateAllPossibleStats(baseStat)
    }

    /**
     * Estimate the quality percentage of an item based on its actual stat and expected base stat.
     * 
     * @param actualStat The actual stat value of the item
     * @param baseStat The base stat value for the item (from baseitem.csv)
     * @param rarity The rarity of the item
     * @param upgradeLevel Optional upgrade level (null for no upgrades)
     * @return Estimated quality percentage (0-100)
     * 
     * @deprecated Use [OrnaCalculator.estimateQualityWithRarity] instead
     */
    @Deprecated("Use OrnaCalculator.estimateQualityWithRarity instead", 
                ReplaceWith("OrnaCalculator.estimateQualityWithRarity(actualStat, baseStat, rarity, upgradeLevel)", 
                "com.lloir.ornaassistant.utils.OrnaCalculator"))
    fun estimateQualityPercentage(
        actualStat: Int,
        baseStat: Int,
        rarity: String,
        upgradeLevel: String? = null
    ): Double {
        return OrnaCalculator.estimateQualityWithRarity(actualStat, baseStat, rarity, upgradeLevel)
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
     * 
     * @deprecated Use [OrnaCalculator.calculateItemStats] instead
     */
    @Deprecated("Use OrnaCalculator.calculateItemStats instead")
    fun calculateFinalStatsWithAdornments(
        baseStats: Map<String, Int>,
        rarity: String,
        qualityPercentage: Double,
        upgradeLevel: String? = null,
        adornments: Map<String, Int> = emptyMap()
    ): Map<String, Int> {
        // Convert quality percentage to decimal
        val quality = qualityPercentage / 100.0

        // Get rarity multiplier
        val rarityMultiplier = ItemUtils.RARITY_MULTIPLIERS[rarity] ?: 1.0

        // Calculate effective quality
        val effectiveQuality = quality * rarityMultiplier

        // Use OrnaCalculator to calculate item stats
        val result = OrnaCalculator.calculateItemStats(
            baseStats = baseStats,
            isBoss = false, // Legacy method doesn't know if boss
            upgradeLevel = upgradeLevel ?: "1",
            quality = effectiveQuality,
            adornments = adornments
        )

        // Convert Double values to Int for backward compatibility
        return result.mapValues { it.value.toInt() }
    }
}
