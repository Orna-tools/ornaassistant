package com.lloir.ornaassistant.utils

import android.util.Log
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Orna Stat Calculator - Based on EXACT CALC sheet formulas from ORNA STAT CALC.ods
 *
 * Core Formula: Final_Stat = ROUNDUP(ROUNDUP(Base_Stat + Level_Bonus) × Upgrade_Multiplier) × Quality_Multiplier)
 * Level_Bonus = MAX(1, ROUNDUP(Base_Stat ÷ 10 × Boss_Multiplier))
 * Boss_Multiplier = 1.25 for boss items, 1.0 for regular items
 *
 * Key features:
 * - Boss items get 25% level bonus multiplier
 * - Upgrade codes: 1-10, MF=11, DF=12, GF=13
 * - Quality bonuses: MF=+1%, DF=+2%, GF=+3%
 * - All calculations use ROUNDUP (ceil)
 * - Ward uses percentage-based calculation
 * 
 * This is a consolidated calculator that combines functionality from both
 * OrnaCalculator and EnhancedQualityCalculator.
 */
object OrnaCalculator {
    private const val TAG = "OrnaCalculator"

    // Upgrade level mappings from CALC sheet
    private val upgradeLevels = mapOf(
        "1" to 1, "2" to 2, "3" to 3, "4" to 4, "5" to 5,
        "6" to 6, "7" to 7, "8" to 8, "9" to 9, "10" to 10,
        "MF" to 11, "DF" to 12, "GF" to 13
    )

    // Celestial weapon adornment slots by level
    private val celestialWeaponSlots = listOf(1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 5)

    // Stats that don't get anguish bonuses
    private val anguishSkipSet = setOf("Ward", "Foresight")

    // Quality bonuses for special upgrades
    private val qualityBonuses = mapOf(
        "MF" to 0.01, "DF" to 0.02, "GF" to 0.03
    )

    // Rarity multipliers for different item rarities
    private val rarityMultipliers = mapOf(
        "Broken" to 0.5,
        "Poor" to 0.75,
        "Common" to 1.0,
        "Superior" to 1.25,
        "Famed" to 1.5,
        "Legendary" to 1.75,
        "Ornate" to 2.0
    )

    /**
     * Calculate final stat using the formula from the JavaScript implementation
     * Aligned with the game's actual mechanics
     */
    fun calculateFinalStat(
        baseStat: Int,
        isBoss: Boolean = false,
        upgradeLevel: String = "1",
        quality: Double = 1.0,
        isWard: Boolean = false,
        anguishLevel: Int = 0,
        statName: String = ""
    ): Double {
        if (baseStat <= 0) {
            return 0.0
        }

        // Convert upgrade level to numeric value (level)
        val level = upgradeLevels[upgradeLevel] ?: 1

        // Calculate stat delta based on base stat and boss status
        val divisor = if (isBoss) 8.0 else 10.0
        val negDivisor = if (isBoss) -600.0 else -75.0
        val actualDivisor = if (baseStat > 0) divisor else negDivisor
        val statDelta = ceil(baseStat / actualDivisor)

        // Apply level scaling (linear increase)
        val levelScaling = if (level == 1) 0.0 else level * statDelta
        val scaledBaseStat = baseStat + levelScaling

        // Calculate quality multiplier with bonus for special upgrades
        var qualityMultiplier = quality
        qualityBonuses[upgradeLevel]?.let { bonus ->
            qualityMultiplier += bonus
        }

        // Apply quality multiplier
        val qualityAdjustedStat = if (isWard) {
            // Ward uses percentage-based calculation
            ceil(scaledBaseStat * qualityMultiplier * 100) / 100
        } else {
            ceil(scaledBaseStat * qualityMultiplier)
        }

        // Apply anguish bonus if applicable (3% per level)
        val skipAnguish = isWard || (statName.isNotEmpty() && anguishSkipSet.contains(statName))
        val finalStat = if (anguishLevel > 0 && !skipAnguish) {
            floor(qualityAdjustedStat * (1 + 0.03 * anguishLevel))
        } else {
            qualityAdjustedStat
        }

        Log.d(TAG, "calculateFinalStat: base=$baseStat, boss=$isBoss, upgrade=$upgradeLevel, " +
                "quality=$quality, ward=$isWard, anguish=$anguishLevel -> result=$finalStat")

        return finalStat
    }

    /**
     * Calculate stats for an entire item with all stats
     */
    fun calculateItemStats(
        baseStats: Map<String, Int>,
        isBoss: Boolean = false,
        upgradeLevel: String = "1",
        quality: Double = 1.0,
        adornments: Map<String, Int> = emptyMap(),
        anguishLevel: Int = 0
    ): Map<String, Double> {
        val finalStats = mutableMapOf<String, Double>()

        // Process each base stat
        baseStats.forEach { (statName, baseStat) ->
            if (baseStat > 0) {
                val isWard = statName.equals("Ward", ignoreCase = true)

                val calculatedStat = calculateFinalStat(
                    baseStat = baseStat,
                    isBoss = isBoss,
                    upgradeLevel = upgradeLevel,
                    quality = quality,
                    isWard = isWard,
                    anguishLevel = anguishLevel,
                    statName = statName
                )

                // Add adornment bonus if present
                val adornmentBonus = adornments[statName] ?: 0
                finalStats[statName] = calculatedStat + adornmentBonus
            }
        }

        // Add adornment-only stats
        adornments.forEach { (statName, bonus) ->
            if (!baseStats.containsKey(statName) && bonus > 0) {
                finalStats[statName] = bonus.toDouble()
            }
        }

        return finalStats
    }

    /**
     * Estimate quality percentage from actual vs expected stats
     * Used for reverse engineering quality from known item stats
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
        val estimatedQuality = (actualStat / expectedAt100) - qualityBonus

        // Clamp between reasonable bounds (1% to 300%)
        return max(0.01, min(3.0, estimatedQuality))
    }

    /**
     * Calculate quality based on multiple stats (used for assessment)
     * Returns average quality across all relevant stats
     */
    fun calculateQualityFromStats(
        actualStats: Map<String, Int>,
        expectedStats: Map<String, Int>,
        relevantStats: Set<String> = emptySet()
    ): Pair<Double, Map<String, Double>> {
        var totalQuality = 0.0
        var validStats = 0
        val statQualities = mutableMapOf<String, Double>()

        actualStats.forEach { (statName, actualValue) ->
            // Skip if not a relevant stat (when specified)
            if (relevantStats.isNotEmpty() && statName !in relevantStats) {
                Log.d(TAG, "Skipping $statName - not in relevant stats")
                return@forEach
            }

            val expectedValue = expectedStats[statName] ?: 0

            // Calculate quality for this stat
            val quality = when {
                expectedValue > 0 && actualValue > 0 -> {
                    // Normal positive stats
                    actualValue.toDouble() / expectedValue.toDouble()
                }
                expectedValue < 0 && actualValue < 0 -> {
                    // Both negative (cursed stats) - closer to 0 is better
                    val expectedDistance = kotlin.math.abs(expectedValue)
                    val actualDistance = kotlin.math.abs(actualValue)
                    expectedDistance.toDouble() / actualDistance.toDouble()
                }
                expectedValue < 0 && actualValue >= 0 -> {
                    // Expected negative but actual is positive/zero - exceptional quality!
                    2.0 // 200% quality
                }
                else -> {
                    // Skip stats where calculation doesn't make sense
                    Log.d(TAG, "Skipping stat $statName: expected=$expectedValue, actual=$actualValue")
                    return@forEach
                }
            }

            if (quality > 0) {
                statQualities[statName] = quality
                totalQuality += quality
                validStats++

                Log.d(TAG, "Quality for $statName: actual=$actualValue, expected=$expectedValue, quality=${quality * 100}%")
            }
        }

        // Calculate average quality
        val overallQuality = if (validStats > 0) {
            totalQuality / validStats
        } else {
            1.0 // Default to 100% if no valid stats
        }

        Log.d(TAG, "Overall quality: ${overallQuality * 100}% (from $validStats stats)")
        return Pair(overallQuality, statQualities)
    }

    /**
     * Calculate final stat with rarity and quality percentage
     * This method provides compatibility with the rarity-based system
     */
    fun calculateStatWithRarity(
        baseStat: Int,
        rarity: String,
        qualityPercentage: Double,
        upgradeLevel: String? = null
    ): Int {
        val rarityMultiplier = rarityMultipliers[rarity] ?: 1.0

        // Convert quality percentage to decimal
        val quality = qualityPercentage / 100.0

        // Calculate effective quality including rarity
        val effectiveQuality = quality * rarityMultiplier

        // Use the PERFECT formula with effective quality
        val result = calculateFinalStat(
            baseStat = baseStat,
            isBoss = false, // Legacy method doesn't know if boss
            upgradeLevel = upgradeLevel ?: "1",
            quality = effectiveQuality,
            isWard = false
        )

        return result.toInt()
    }

    /**
     * Calculate all possible stat values for an item (all rarities, qualities, and upgrades).
     * Useful for comparing potential item outcomes.
     * 
     * @param baseStat The base stat value for the item
     * @return Map of rarity to map of quality to map of upgrade level to stat value
     */
    fun calculateAllPossibleStats(baseStat: Int): Map<String, Map<Int, Map<String, Int>>> {
        val result = mutableMapOf<String, MutableMap<Int, MutableMap<String, Int>>>()

        // For each rarity
        for (rarity in rarityMultipliers.keys) {
            val qualityMap = mutableMapOf<Int, MutableMap<String, Int>>()

            // For each quality percentage (100%, 95%, 90%, 85%, 80%)
            for (quality in listOf(100, 95, 90, 85, 80)) {
                val upgradeMap = mutableMapOf<String, Int>()

                // Base stat (no upgrades)
                upgradeMap["Base"] = calculateStatWithRarity(baseStat, rarity, quality.toDouble())

                // For each upgrade level
                for (upgrade in upgradeLevels.keys) {
                    upgradeMap[upgrade] = calculateStatWithRarity(
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
     * Estimate quality percentage based on rarity
     * 
     * @param actualStat The actual stat value of the item
     * @param baseStat The base stat value for the item
     * @param rarity The rarity of the item
     * @param upgradeLevel Optional upgrade level (null for no upgrades)
     * @return Estimated quality percentage (0-100)
     */
    fun estimateQualityWithRarity(
        actualStat: Int,
        baseStat: Int,
        rarity: String,
        upgradeLevel: String? = null
    ): Double {
        // Handle edge cases
        if (baseStat <= 0 || actualStat <= 0) {
            return 100.0 // Default to 100% for invalid stats
        }

        // Get rarity multiplier
        val rarityMultiplier = rarityMultipliers[rarity] ?: 1.0

        // Calculate expected stat at 100% quality with given rarity and upgrade
        val expectedStatAt100 = calculateStatWithRarity(
            baseStat = baseStat,
            rarity = rarity,
            qualityPercentage = 100.0,
            upgradeLevel = upgradeLevel
        )

        if (expectedStatAt100 <= 0) {
            return 100.0
        }

        // Calculate quality percentage
        val qualityPercentage = (actualStat.toDouble() / expectedStatAt100) * 100.0

        // Cap quality percentage between 1% and 200%
        return qualityPercentage.coerceIn(1.0, 200.0)
    }

    /**
     * Legacy alias for calculateStatWithRarity
     */
    fun calculateLegacyStat(
        baseStat: Int,
        rarity: String,
        qualityPercentage: Double,
        upgradeLevel: String? = null
    ): Int {
        return calculateStatWithRarity(baseStat, rarity, qualityPercentage, upgradeLevel)
    }

    /**
     * Calculate adornment slots based on quality, celestial weapon status, and anguish level
     * 
     * @param baseSlots The base number of adornment slots
     * @param quality The item quality (as a decimal, e.g., 1.87 for 187%)
     * @param isCelestialWeapon Whether the item is a celestial weapon
     * @param isTwoHanded Whether the item is a two-handed weapon
     * @param level The item level (1-10, 11=MF, 12=DF, 13=GF)
     * @param anguishLevel The anguish level (0 for no anguish)
     * @return The number of adornment slots
     */
    fun calculateAdornmentSlots(
        baseSlots: Int,
        quality: Double,
        isCelestialWeapon: Boolean = false,
        isTwoHanded: Boolean = false,
        level: Int = 1,
        anguishLevel: Int = 0
    ): Int {
        // Celestial weapons have special slot progression
        if (isCelestialWeapon) {
            val index = (level - 1).coerceIn(0, celestialWeaponSlots.size - 1)
            return celestialWeaponSlots[index] + if (isTwoHanded) 1 else 0
        }

        // Anguished items always get +4 slots
        if (anguishLevel > 0) {
            return baseSlots + 4
        }

        // Calculate additional slots based on quality
        val additionalSlots = when {
            quality > 2.0 -> -1  // Invalid quality
            quality >= 1.7 -> 2  // 170%+ quality
            quality > 1.0 -> 1   // 100%+ quality
            quality > 0.7 -> 0   // 70%+ quality
            else -> -1           // Below 70% quality
        }

        // Apply level-based bonuses
        return when {
            level > 12 -> baseSlots + 4  // GF
            level > 10 -> baseSlots + 3  // MF, DF
            level > 0 -> baseSlots + additionalSlots.coerceAtLeast(-baseSlots)  // Normal levels
            else -> baseSlots  // Default
        }
    }
}
