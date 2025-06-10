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
