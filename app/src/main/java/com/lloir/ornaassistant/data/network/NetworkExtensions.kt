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
        attack = this["Att"] ?: this["Attack"],
        magic = this["Mag"] ?: this["Magic"],
        defense = this["Def"] ?: this["Defense"],
        resistance = this["Res"] ?: this["Resistance"],
        dexterity = this["Dex"] ?: this["Dexterity"],
        hp = this["HP"],
        mana = this["Mana"],
        ward = this["Ward"],
        crit = this["Crit"]
    )
}

fun AssessmentResponseDto.toAssessmentResult(): AssessmentResult {
    return try {
        // Log the raw response for debugging
        Log.d(TAG, "Raw assessment response: quality=$quality, stats=$stats")
        
        // Parse quality from string to double
        val qualityValue = quality.toDoubleOrNull() ?: 0.0
        Log.d(TAG, "Parsed quality: $qualityValue from string: $quality")

        // If quality is 0, it means the assessment failed
        if (qualityValue == 0.0) {
            Log.w(TAG, "Assessment failed - quality is 0. Response: $this")
            AssessmentResult(
                quality = 0.0,
                stats = emptyMap(),
                materials = listOf(0, 0, 0, 0)
            )
        }

        // Convert stats to the expected format
        val parsedStats = mutableMapOf<String, List<String>>()

        stats.forEach { (statName, statInfo) ->
            // The API returns values array with upgrade values
            // Index 0 is base, indices 1-9 are 1★ to 9★, index 10 is 10★
            // Then MF, DF, GF values follow
            val values = if (statInfo.values.size >= 14) {
                listOf(
                    statInfo.values[10].toString(), // 10★
                    statInfo.values[11].toString(), // MF
                    statInfo.values[12].toString(), // DF  
                    statInfo.values[13].toString()  // GF
                )
            } else if (statInfo.values.size >= 10) {
                // Standard values array without forging
                // API returns values for levels 1-10
                listOf(
                    statInfo.values[9].toString(), // Level 10 value (10★)
                    "0", // MF not provided
                    "0", // DF not provided
                    "0"  // GF not provided
                )
            } else {
                // Fallback for shorter arrays
                val lastIndex = statInfo.values.size - 1
                val maxValue = if (lastIndex >= 0) statInfo.values[lastIndex].toString() else "0"
                listOf(
                    maxValue, // Use last available value
                    "0", "0", "0" // MF, DF, GF not provided
                )
            }
            
            // Log the actual values for debugging
            Log.d(TAG, "Stat $statName - base: ${statInfo.base}, values count: ${statInfo.values.size}")
            Log.d(TAG, "  Values: ${statInfo.values.joinToString(", ")}")

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

        AssessmentResult(
            quality = qualityValue,
            stats = parsedStats,
            materials = listOf(
                135, // Base materials for 10★
                (300 * qualityValue).toInt(), // MF materials
                (666 * qualityValue).toInt(), // DF materials
                0 // GF materials (usually 0 or special calculation)
            )
        )

    } catch (e: Exception) {
        Log.e(TAG, "Error parsing assessment response", e)
        // Return empty result on error
        AssessmentResult(
            quality = 0.0,
            stats = emptyMap(),
            materials = listOf(0, 0, 0, 0)
        )
    }
}