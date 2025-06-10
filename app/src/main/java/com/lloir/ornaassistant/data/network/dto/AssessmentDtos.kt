package com.lloir.ornaassistant.data.network.dto

import com.google.gson.annotations.SerializedName

data class AssessmentRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("level") val level: Int,
    @SerializedName("attack") val attack: Int? = null,
    @SerializedName("magic") val magic: Int? = null,
    @SerializedName("defense") val defense: Int? = null,
    @SerializedName("resistance") val resistance: Int? = null,
    @SerializedName("dexterity") val dexterity: Int? = null,
    @SerializedName("hp") val hp: Int? = null,
    @SerializedName("mana") val mana: Int? = null,
    @SerializedName("ward") val ward: Int? = null
)

data class AssessmentResponseDto(
    @SerializedName("quality") val quality: Double,
    @SerializedName("stats") val stats: String // JSON string that needs parsing
)

// FILE: data/network/NetworkExtensions.kt
package com.lloir.ornaassistant.data.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lloir.ornaassistant.data.network.dto.AssessmentRequestDto
import com.lloir.ornaassistant.data.network.dto.AssessmentResponseDto
import com.lloir.ornaassistant.domain.model.AssessmentResult
import org.json.JSONObject

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
    // Parse the stats JSON string
    val statsMap = try {
        val jsonObject = JSONObject(stats)
        val parsedStats = mutableMapOf<String, List<String>>()

        // Parse each stat from the JSON
        jsonObject.keys().forEach { statName ->
            val statObject = jsonObject.getJSONObject(statName)
            val valuesArray = statObject.getJSONArray("values")

            // Extract the 10★, MF, DF, GF values (indices 9, 10, 11, 12)
            val values = listOf(
                if (valuesArray.length() > 9) valuesArray.getString(9) else "0",
                if (valuesArray.length() > 10) valuesArray.getString(10) else "0",
                if (valuesArray.length() > 11) valuesArray.getString(11) else "0",
                if (valuesArray.length() > 12) valuesArray.getString(12) else "0"
            )

            parsedStats[statName.replaceFirstChar { it.uppercase() }] = values
        }

        parsedStats
    } catch (e: Exception) {
        emptyMap<String, List<String>>()
    }

    return AssessmentResult(
        quality = quality,
        stats = statsMap,
        materials = listOf(
            135, // Base materials for 10★
            (300 * quality).toInt(), // MF materials
            (666 * quality).toInt(), // DF materials
            0 // GF materials (usually 0 or special calculation)
        )
    )
}