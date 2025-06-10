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
            // Get the 10★, MF, DF, GF values (indices 9, 10, 11, 12)
            val values = listOf(
                if (statInfo.values.size > 9) statInfo.values[9].toString() else "0",
                if (statInfo.values.size > 10) statInfo.values[10].toString() else "0",
                if (statInfo.values.size > 11) statInfo.values[11].toString() else "0",
                if (statInfo.values.size > 12) statInfo.values[12].toString() else "0"
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