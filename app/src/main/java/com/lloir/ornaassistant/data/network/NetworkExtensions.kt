package com.lloir.ornaassistant.data.network

import android.util.Log
import com.lloir.ornaassistant.data.network.dto.AssessmentRequestDto
import com.lloir.ornaassistant.data.network.dto.AssessmentResponseDto
import com.lloir.ornaassistant.domain.model.AssessmentResult

private const val TAG = "NetworkExtensions"

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
    return try {
        // Parse quality from string to double
        val qualityValue = quality.toDoubleOrNull() ?: 0.0
        Log.d(TAG, "Parsed quality: $qualityValue from string: $quality")

        // Convert stats to the expected format
        val parsedStats = mutableMapOf<String, List<String>>()

        stats.forEach { (statName, statInfo) ->
            // Get base stats (index 0) and 10★, MF, DF, GF values (indices 9, 10, 11, 12)
            // Use >= for array bounds checking to avoid index errors
            val baseValue = if (statInfo.values.isNotEmpty()) statInfo.values[0].toString() else "0"
            val tenStarValue = if (statInfo.values.size >= 10) statInfo.values[9].toString() else "0"
            val mfValue = if (statInfo.values.size >= 11) statInfo.values[10].toString() else "0"
            val dfValue = if (statInfo.values.size >= 12) statInfo.values[11].toString() else "0"
            val gfValue = if (statInfo.values.size >= 13) statInfo.values[12].toString() else "0"

            // Create a list with base value and upgrade values for better comparison
            val values = listOf(
                baseValue,
                tenStarValue,
                mfValue,
                dfValue,
                gfValue
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
            Log.d(TAG, "Parsed stat $capitalizedStatName: $values")
        }

        // Calculate GF materials based on quality
        val gfMaterials = if (qualityValue >= 0.9) {
            // High quality items (90%+) get GF materials
            (1000 * qualityValue).toInt()
        } else {
            // Lower quality items don't get GF materials
            0
        }

        AssessmentResult(
            quality = qualityValue,
            stats = parsedStats,
            materials = listOf(
                135, // Base materials for 10★
                (300 * qualityValue).toInt(), // MF materials
                (666 * qualityValue).toInt(), // DF materials
                gfMaterials // GF materials calculation
            )
        )

    } catch (e: Exception) {
        Log.e(TAG, "Error parsing assessment response", e)
        // Better fallback logic with more detailed error handling
        try {
            // Try to extract at least the quality if possible
            val fallbackQuality = quality.toDoubleOrNull() ?: 0.0

            // Create minimal stats map with zeros
            val fallbackStats = mapOf(
                "Att" to listOf("0", "0", "0", "0", "0"),
                "Mag" to listOf("0", "0", "0", "0", "0"),
                "Def" to listOf("0", "0", "0", "0", "0"),
                "Res" to listOf("0", "0", "0", "0", "0")
            )

            Log.w(TAG, "Using fallback assessment result with quality: $fallbackQuality")

            AssessmentResult(
                quality = fallbackQuality,
                stats = fallbackStats,
                materials = listOf(
                    135,
                    (300 * fallbackQuality).toInt(),
                    (666 * fallbackQuality).toInt(),
                    0
                )
            )
        } catch (fallbackError: Exception) {
            Log.e(TAG, "Fallback error handling also failed", fallbackError)
            // Last resort empty result
            AssessmentResult(
                quality = 0.0,
                stats = emptyMap(),
                materials = listOf(0, 0, 0, 0)
            )
        }
    }
}
