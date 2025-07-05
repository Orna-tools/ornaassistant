package com.lloir.ornaassistant.utils

import android.util.Log

/**
 * Utility for calculating item quality based on stats
 */
object QualityCalculator {
    private const val TAG = "QualityCalculator"

    /**
     * Calculate quality percentage based on actual vs expected stats
     * @param actualStats Map of actual stat values
     * @param expectedStats Map of expected stat values
     * @return Pair of (overall quality percentage, map of individual stat qualities)
     */
    fun calculateQuality(
        actualStats: Map<String, Int>,
        expectedStats: Map<String, Int>
    ): Pair<Double, Map<String, Double>> {
        var totalQuality = 0.0
        var validStats = 0
        val statQualities = mutableMapOf<String, Double>()
        
        actualStats.forEach { (statName, actualValue) ->
            val expectedValue = expectedStats[statName] ?: 0
            
            // Handle different stat types
            val quality = when {
                expectedValue > 0 && actualValue > 0 -> {
                    // Normal positive stats
                    (actualValue.toDouble() / expectedValue.toDouble()) * 100.0
                }
                expectedValue < 0 && actualValue < 0 -> {
                    // Both negative (cursed stats) - closer to 0 is better
                    val expectedDistance = kotlin.math.abs(expectedValue)
                    val actualDistance = kotlin.math.abs(actualValue)
                    (expectedDistance.toDouble() / actualDistance.toDouble()) * 100.0
                }
                expectedValue < 0 && actualValue >= 0 -> {
                    // Expected negative but actual is positive/zero - exceptional quality!
                    200.0 // Cap at 200% for this case
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
                
                Log.d(TAG, "Quality for $statName: actual=$actualValue, expected=$expectedValue, quality=$quality%")
            }
        }
        
        // Calculate average quality
        val overallQuality = if (validStats > 0) {
            totalQuality / validStats
        } else {
            0.0
        }
        
        Log.d(TAG, "Overall quality: $overallQuality% (from $validStats stats)")
        return Pair(overallQuality / 100.0, statQualities.mapValues { it.value / 100.0 })
    }
}