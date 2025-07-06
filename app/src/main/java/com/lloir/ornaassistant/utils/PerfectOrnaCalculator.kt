/**
 * Perfect Orna Stat Calculator - Based on exact CALC sheet formulas
 * 
 * This replaces the estimated calculations with the EXACT formulas
 * extracted from the ORNA STAT CALC.ods file.
 * 
 * Key discoveries:
 * - Boss items get 25% level bonus multiplier
 * - Upgrade codes: MF=11, DF=12, GF=13
 * - Quality bonuses: MF=+1%, DF=+2%, GF=+3%
 * - All calculations use ROUNDUP (ceil)
 * - Ward uses percentage-based calculation
 */

package com.lloir.ornaassistant.utils

import android.util.Log
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object PerfectOrnaCalculator {
    private const val TAG = "PerfectOrnaCalculator"

    // Core formula: Final_Stat = ROUNDUP(ROUNDUP(Base_Stat + Level_Bonus) × Upgrade_Multiplier) × Quality_Multiplier)
    // Level_Bonus = MAX(1, ROUNDUP(Base_Stat ÷ 10 × Boss_Multiplier))
    // Boss_Multiplier = 1.25 for boss items, 1.0 for regular items

    private val upgradeLevels = mapOf(
        "1" to 1, "2" to 2, "3" to 3, "4" to 4, "5" to 5,
        "6" to 6, "7" to 7, "8" to 8, "9" to 9, "10" to 10,
        "MF" to 11, "DF" to 12, "GF" to 13
    )

    private val qualityBonuses = mapOf(
        "MF" to 0.01, "DF" to 0.02, "GF" to 0.03
    )

    /**
     * Calculate final stat using EXACT formula from CALC sheet
     */
    fun calculateFinalStat(
        baseStat: Int,
        isBoss: Boolean = false,
        upgradeLevel: String = "1",
        quality: Double = 1.0,
        isWard: Boolean = false
    ): Double {
        // Handle zero base stats
        if (baseStat <= 0) {
            return 0.0
        }

        // Convert upgrade level to numeric
        val numericUpgrade = upgradeLevels[upgradeLevel] ?: 1

        // Calculate level bonus - extracted from the CALC formula
        val bossMultiplier = if (isBoss) 1.25 else 1.0
        val levelBonus = max(1.0, ceil(baseStat / 10.0 * bossMultiplier))

        // Enhanced base stat (base + level bonus)
        val enhancedBaseStat = baseStat + levelBonus

        // Apply upgrade multiplier
        val statWithUpgrade = ceil(enhancedBaseStat * numericUpgrade)

        // Calculate quality multiplier
        var qualityMultiplier = quality
        qualityBonuses[upgradeLevel]?.let { bonus ->
            qualityMultiplier += bonus
        }

        // Ward uses different calculation (percentage based)
        if (isWard) {
            val wardResult = ceil(enhancedBaseStat * numericUpgrade * qualityMultiplier * 100) / 100
            return wardResult
        }

        // Apply quality multiplier and final round up
        val finalStat = ceil(statWithUpgrade * qualityMultiplier)

        Log.d(TAG, "calculateFinalStat: base=$baseStat, boss=$isBoss, upgrade=$upgradeLevel, " +
                "quality=$quality, ward=$isWard -> result=$finalStat")

        return finalStat
    }

    /**
     * Calculate stats for an entire item
     */
    fun calculateItemStats(
        baseStats: Map<String, Int>,
        isBoss: Boolean = false,
        upgradeLevel: String = "1",
        quality: Double = 1.0,
        adornments: Map<String, Int> = emptyMap()
    ): Map<String, Double> {
        val finalStats = mutableMapOf<String, Double>()

        // List of all possible stats
        val allStats = listOf("atk", "mag", "def", "res", "hp", "mana", "dex", "ward", "crit")

        // Calculate each stat
        allStats.forEach { stat ->
            val baseStat = baseStats[stat] ?: 0
            val isWard = stat == "ward"

            if (baseStat > 0) {
                val calculatedStat = calculateFinalStat(
                    baseStat = baseStat,
                    isBoss = isBoss,
                    upgradeLevel = upgradeLevel,
                    quality = quality,
                    isWard = isWard
                )

                // Add adornment bonus
                val adornmentBonus = adornments[stat] ?: 0
                finalStats[stat] = calculatedStat + adornmentBonus

            } else if (adornments.containsKey(stat)) {
                // Adornment-only stat
                finalStats[stat] = (adornments[stat] ?: 0).toDouble()
            }
        }

        return finalStats
    }

    /**
     * Estimate quality percentage from actual vs expected stats
     */
    fun estimateQuality(
        actualStat: Double,
        baseStat: Int,
        isBoss: Boolean = false,
        upgradeLevel: String = "1",
        isWard: Boolean = false
    ): Double {
        if (baseStat <= 0 || actualStat <= 0) {
            return 1.0
        }

        // Calculate what the stat would be at 100% quality
        val expectedAt100 = calculateFinalStat(baseStat, isBoss, upgradeLevel, 1.0, isWard)

        if (expectedAt100 <= 0) {
            return 1.0
        }

        // Account for quality bonus if using special upgrade
        val qualityBonus = qualityBonuses[upgradeLevel] ?: 0.0

        // Reverse engineer the quality
        val estimatedQuality = actualStat / expectedAt100 - qualityBonus

        // Clamp between reasonable bounds
        return max(0.01, min(3.0, estimatedQuality))
    }

}