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
    @SerializedName("quality") val quality: Any?, // Handle both String and Number types
    @SerializedName("stats") val stats: Map<String, StatInfo>,
    @SerializedName("name") val name: String? = null,
    @SerializedName("id") val id: Int? = null,
    @SerializedName("tier") val tier: Int? = null
)

data class StatInfo(
    @SerializedName("base") val base: Int? = null,
    @SerializedName("values") val values: List<Int> = emptyList(),
    // Some APIs return different field names
    @SerializedName("min") val min: Int? = null,
    @SerializedName("max") val max: Int? = null
)
