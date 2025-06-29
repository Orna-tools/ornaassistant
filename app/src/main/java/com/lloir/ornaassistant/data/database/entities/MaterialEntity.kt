package com.lloir.ornaassistant.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Database entity for materials in the game.
 * 
 * This entity stores information about materials that can be tracked by the user.
 */
@Entity(tableName = "materials")
data class MaterialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val currentQuantity: Int = 0,
    val targetQuantity: Int? = null,
    val isTracked: Boolean = false,
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)