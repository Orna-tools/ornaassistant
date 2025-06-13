package com.lloir.ornaassistant.data.network.dto

import com.google.gson.annotations.SerializedName

data class AssessmentRequestDto(
    @SerializedName("item") val name: String,
    @SerializedName("level") val level: Int,
    // Only send non-null stats to the API
    @SerializedName("att") val attack: Int? = null,
    @SerializedName("mag") val magic: Int? = null,
    @SerializedName("def") val defense: Int? = null,
    @SerializedName("res") val resistance: Int? = null,
    @SerializedName("dex") val dexterity: Int? = null,
    @SerializedName("hp") val hp: Int? = null,
    @SerializedName("mana") val mana: Int? = null,
    @SerializedName("ward") val ward: Int? = null,
    @SerializedName("crit") val crit: Int? = null
)

data class AssessmentResponseDto(
    @SerializedName("quality") val quality: String, // Changed to String since API returns "1.7"
    @SerializedName("stats") val stats: Map<String, StatInfo>, // Changed to proper object structure
    @SerializedName("name") val name: String? = null,
    @SerializedName("id") val id: Int? = null,
    @SerializedName("tier") val tier: Int? = null
)

data class StatInfo(
    @SerializedName("base") val base: Int,
    @SerializedName("values") val values: List<Int>
)