package com.lloir.ornaassistant.assess

data class AssessResult(
    val quality: Double,
    val stats: Map<String, StatSeries>,
    val tier: Int? = null,
    val itemType: String? = null,
    val itemName: String? = null,
    val materials: List<Material>? = null,
    val exact: Boolean = true
)

data class StatSeries(
    val base: Int,
    val values: List<Int>
)

data class Material(
    val name: String,
    val id: Int
)