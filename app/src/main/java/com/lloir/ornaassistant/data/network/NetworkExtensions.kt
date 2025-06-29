package com.lloir.ornaassistant.data.network

import android.util.Log
import com.lloir.ornaassistant.data.network.dto.AssessmentRequestDto
import com.lloir.ornaassistant.data.network.dto.AssessmentResponseDto
import com.lloir.ornaassistant.domain.model.AssessmentResult
import kotlin.text.toDoubleOrNull

private const val TAG = "NetworkExtensions"

// Extension function to check if assessment is valid
fun AssessmentResponseDto.hasValidAssessment(): Boolean {
    return try {
        // Parse quality - handle both String and Double responses
        val qualityValue = when (quality) {
            is Number -> quality.toDouble()
            is String -> quality.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

        // Valid if quality > 0 OR if we have meaningful stats
        qualityValue > 0.0 || (stats.isNotEmpty() && stats.values.any { it.values.isNotEmpty() })
    } catch (e: Exception) {
        Log.e(TAG, "Error checking if assessment is valid", e)
        false
    }
}

// Enhanced assessment validation with better debugging
fun AssessmentResponseDto.isValidAssessmentResult(): Boolean {
    val qualityValue = quality?.toString()?.toDoubleOrNull() ?: 0.0
    val hasStats = stats.isNotEmpty() && stats.any { (_, statInfo) -> 
        statInfo.values.isNotEmpty() && statInfo.values.any { it != 0 }
    }

    Log.d(TAG, "Assessment validation: quality=$qualityValue, hasStats=$hasStats, itemName=${name ?: "Unknown"}")

    // Consider assessment valid if either quality > 0 OR we have meaningful stat data
    return qualityValue > 0.0 || hasStats
}

// Create a fallback assessment result for failed assessments
fun createFallbackAssessment(itemName: String): AssessmentResult {
    Log.w(TAG, "Creating fallback assessment for $itemName - assessment failed")
    return AssessmentResult(
        quality = 0.0,
        stats = mapOf(
            "Att" to listOf("0", "0", "0", "0"),
            "Mag" to listOf("0", "0", "0", "0"),
            "Def" to listOf("0", "0", "0", "0"),
            "Res" to listOf("0", "0", "0", "0")
        ),
        materials = listOf(0, 0, 0, 0),
        assessmentFailed = true
    )
}

// Extension functions for network mapping
fun Map<String, Int>.toAssessmentRequest(itemName: String, level: Int): AssessmentRequestDto {
    return AssessmentRequestDto(
        name = itemName,
        level = level,
        attack = this["Att"],
        magic = this["Mag"],
        defense = this["Def"],
        resistance = this["Res"],
        dexterity = this["Dex"],
        hp = this["HP"],
        mana = this["Mana"],
        ward = this["Ward"]
    ).also {
        // Enhanced debugging of the request being sent
        Log.d(TAG, "=== ASSESSMENT REQUEST ===")
        Log.d(TAG, "Item: $itemName, Level: $level")
        Log.d(TAG, "Stats being sent:")
        this.forEach { (stat, value) ->
            Log.d(TAG, "  $stat: $value")
        }
        Log.d(TAG, "========================")
    }
}

fun AssessmentResponseDto.toAssessmentResult(): AssessmentResult {
    return try {
        Log.d(TAG, "Processing assessment response for: $name")
        Log.d(TAG, "Raw quality value: $quality, type: ${quality?.javaClass?.simpleName}")
        Log.d(TAG, "Stats count: ${stats.size}")

        // Parse quality value with proper type handling
        val qualityValue = when (quality) {
            is Number -> quality.toDouble()
            is String -> {
                val cleaned = quality.toString().trim().replace(",", ".")
                cleaned.toDoubleOrNull() ?: 0.0
            }
            else -> {
                Log.w(TAG, "Unexpected quality type: ${quality?.javaClass}")
                0.0
            }
        }

        Log.d(TAG, "Parsed quality: $qualityValue")

        // Check if assessment is valid BEFORE processing
        if (!hasValidAssessment()) {
            Log.w(TAG, "Invalid assessment detected - using fallback")
            return createFallbackAssessment(name ?: "Unknown")
        }

        // Parse stats with improved structure handling
        val parsedStats = mutableMapOf<String, List<String>>()

        stats.forEach { (statName, statInfo) ->
            Log.d(TAG, "Processing stat: $statName")
            Log.d(TAG, "Stat values: ${statInfo.values}")

            // Convert all values to strings for consistent display
            val values = statInfo.values.map { it.toString() }

            // Normalize stat names
            val capitalizedStatName = when (statName.lowercase()) {
                "att", "attack" -> "Att"
                "mag", "magic" -> "Mag"
                "def", "defense" -> "Def"
                "res", "resistance" -> "Res"
                "dex", "dexterity" -> "Dex"
                "ward" -> "Ward" 
                "mana" -> "Mana"
                "hp" -> "HP"
                else -> statName.replaceFirstChar { it.uppercase() }
            }

            parsedStats[capitalizedStatName] = values
        }

        // Calculate materials based on quality - FIXED FORMULA
        val materials = listOf(
            135, // Base 10★ materials - always constant
            if (qualityValue > 0) (200 * qualityValue).toInt() else 0, // MF - corrected multiplier
            if (qualityValue > 0) (500 * qualityValue).toInt() else 0, // DF - corrected multiplier  
            if (qualityValue >= 1.5) (1000 * qualityValue).toInt() else 0 // GF - only for high quality
        )

        Log.d(TAG, "Calculated materials: $materials")

        AssessmentResult(
            quality = qualityValue,
            stats = parsedStats,
            materials = materials,
            assessmentFailed = (qualityValue == 0.0)
        )

    } catch (e: Exception) {
        Log.e(TAG, "Error parsing assessment response", e)
        // Better fallback logic with more detailed error handling
        try {
            // Try to extract at least the quality if possible
            val fallbackQuality = quality?.toString()?.toDoubleOrNull() ?: 50.0 // Default to neutral quality
            val isFailedAssessment = fallbackQuality <= 0.0 && stats.isEmpty()

            // Create minimal stats map - use "Unknown" instead of "0" for failed assessments
            val fallbackStats = mapOf(
                "Att" to if (isFailedAssessment) listOf("?", "?", "?", "?") else listOf("0", "0", "0", "0"),
                "Mag" to if (isFailedAssessment) listOf("?", "?", "?", "?") else listOf("0", "0", "0", "0"),
                "Dex" to if (isFailedAssessment) listOf("?", "?", "?", "?") else listOf("0", "0", "0", "0"),
                "Ward" to if (isFailedAssessment) listOf("?", "?", "?", "?") else listOf("0", "0", "0", "0"),
                "Crit" to if (isFailedAssessment) listOf("?", "?", "?", "?") else listOf("0", "0", "0", "0"),
                "Def" to listOf("0", "0", "0", "0"),
                "Res" to listOf("0", "0", "0", "0")
            )

            Log.w(TAG, "Using fallback assessment result with quality: $fallbackQuality, failed: $isFailedAssessment")

            AssessmentResult(
                quality = fallbackQuality,
                stats = fallbackStats,
                materials = listOf(
                    135, // Base materials for 10★
                    if (fallbackQuality > 0) (300 * fallbackQuality).toInt() else 0, // MF materials
                    if (fallbackQuality > 0) (666 * fallbackQuality).toInt() else 0, // DF materials
                    if (fallbackQuality >= 1.5) (1200 * fallbackQuality).toInt() else 0 // GF materials for high quality
                ),
                assessmentFailed = isFailedAssessment
            )
        } catch (fallbackError: Exception) {
            Log.e(TAG, "Fallback error handling also failed", fallbackError)
            // Last resort empty result
            AssessmentResult(
                quality = 0.0,
                stats = emptyMap(),
                materials = listOf(0, 0, 0, 0),
                assessmentFailed = true
            )
        }
    }
}

/**
 * Try to derive a reasonable quality score from stats when API quality parsing fails
 */
private fun AssessmentResponseDto.deriveQualityFromStats(): Double {
    if (stats.isEmpty()) return 0.0

    try {
        // Look for patterns in stat values that might indicate quality
        var hasPositiveValues = false
        var maxStatValue = 0

        stats.forEach { (_, statInfo) ->
            if (statInfo.values.isNotEmpty()) {
                val maxValue = statInfo.values.maxOrNull() ?: 0
                if (maxValue > 0) {
                    hasPositiveValues = true
                    maxStatValue = maxOf(maxStatValue, maxValue)
                }
            }
        }

        // If we have positive stat values, assume at least neutral quality
        return if (hasPositiveValues) 75.0 else 25.0
    } catch (e: Exception) {
        return 50.0 // Neutral fallback
    }
}
