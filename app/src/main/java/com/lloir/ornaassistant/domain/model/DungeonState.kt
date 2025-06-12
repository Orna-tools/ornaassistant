package com.lloir.ornaassistant.domain.model

data class DungeonState(
    val dungeonName: String = "",
    val mode: String = "",
    val floorNumber: Int = 0,
    val hasEntered: Boolean = false,
    val totalFloors: Int? = null,
    val victoryScreenHandledForFloor: Boolean = false,
    val isBetweenFloorLoot: Boolean = false,
    val isDungeonComplete: Boolean = false,

)