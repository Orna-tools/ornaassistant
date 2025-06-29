package com.lloir.ornaassistant.data.network

import android.util.Log
import com.lloir.ornaassistant.data.network.dto.AssessmentRequestDto
import com.lloir.ornaassistant.data.network.dto.AssessmentResponseDto
import com.lloir.ornaassistant.domain.model.AssessmentResult

private const val TAG = "NetworkExtensions"

// Extension function to check if assessment is valid (quality > 0)
fun AssessmentResponseDto.hasValidAssessment(): Boolean {
    return quality.toDoubleOrNull()?.let { it > 0.0 } ?: false
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
    )
}

fun AssessmentResponseDto.toAssessmentResult(): AssessmentResult {
    // Check if this is a valid assessment (quality > 0)
    if (!hasValidAssessment()) {
        Log.w(TAG, "Invalid assessment detected (quality=$quality) - using fallback")
        return createFallbackAssessment(name ?: "Unknown Item")
    }

    return try {
        // Parse quality from string to double
        val qualityValue = quality.toDoubleOrNull() ?: 0.0
        Log.d(TAG, "Parsed quality: $qualityValue from string: $quality")

        // Convert stats to the expected format
        val parsedStats = mutableMapOf<String, List<String>>()

        stats.forEach { (statName, statInfo) ->
            // Handle different stat types - some have bonuses, others have final values
            // When quality is 0, all stats are bonuses to add to base value
            val isBonus = when {
                qualityValue == 0.0 -> true  // All stats are bonuses when quality is 0
                statName.lowercase() in listOf("attack", "magic") -> true  // These stats always return bonuses
                else -> false  // Others (dex, def, res, etc.) return final values when quality > 0
            }

            Log.d(TAG, "Processing $statName: base=${statInfo.base}, values size=${statInfo.values.size}, isBonus=$isBonus")

            val baseValue = statInfo.base
            val tenStarValue = if (statInfo.values.size >= 11) {
                if (isBonus) baseValue + statInfo.values[10] else statInfo.values[10]
            } else {
                baseValue
            }
            val mfValue = if (statInfo.values.size >= 12) {
                if (isBonus) baseValue + statInfo.values[11] else statInfo.values[11]
            } else {
                baseValue
            }
            val dfValue = if (statInfo.values.size >= 13) {
                if (isBonus) baseValue + statInfo.values[12] else statInfo.values[12]
            } else {
                baseValue
            }

            val values = listOf(
                baseValue.toString(),     // Base stat value
                tenStarValue.toString(),  // 10★ value
                mfValue.toString(),       // MF value  
                dfValue.toString()        // DF value
            )

            // Capitalize stat name to match expected format
            val capitalizedStatName = when (statName.lowercase()) {
                "magic" -> "Mag"
                "attack" -> "Att"
                "defense" -> "Def"
                "resistance" -> "Res"
                "dexterity" -> "Dex"
                "crit" -> "Crit"
                "ward" -> "Ward"
                "mana" -> "Mana"
                "hp" -> "HP"
                else -> statName.replaceFirstChar { it.uppercase() }
            }

            parsedStats[capitalizedStatName] = values
            Log.d(TAG, "Final parsed stat $capitalizedStatName: $values")
        }

        AssessmentResult(
            quality = qualityValue,
            stats = parsedStats,
            materials = listOf(
                135, // Base materials for 10★
                if (qualityValue > 0) (300 * qualityValue).toInt() else 0, // MF materials
                if (qualityValue > 0) (666 * qualityValue).toInt() else 0, // DF materials
                if (qualityValue >= 1.5) (1200 * qualityValue).toInt() else 0 // GF materials for high quality
            ),
            assessmentFailed = (qualityValue == 0.0)
        )

    } catch (e: Exception) {
        Log.e(TAG, "Error parsing assessment response", e)
        // Better fallback logic with more detailed error handling
        try {
            // Try to extract at least the quality if possible
            val fallbackQuality = quality.toDoubleOrNull() ?: 0.0
            val isFailedAssessment = fallbackQuality == 0.0

            // Create minimal stats map with zeros
            val fallbackStats = mapOf(
                "Att" to listOf("0", "0", "0", "0"),
                "Mag" to listOf("0", "0", "0", "0"),
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
