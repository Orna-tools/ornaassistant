package com.lloir.ornaassistant.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

/**
 * Domain model for materials in the game.
 * 
 * This class represents a material that can be tracked by the user.
 * It includes the current quantity, target quantity, and tracking status.
 */
@Parcelize
data class Material(
    val id: Long = 0,
    val name: String,
    val currentQuantity: Int = 0,
    val targetQuantity: Int? = null,
    val isTracked: Boolean = false,
    val lastUpdated: LocalDateTime = LocalDateTime.now()
) : Parcelable {
    /**
     * Checks if the material has reached its target quantity.
     */
    fun hasReachedTarget(): Boolean {
        return targetQuantity != null && currentQuantity >= targetQuantity
    }
    
    /**
     * Calculates the progress percentage towards the target.
     */
    fun progressPercentage(): Int {
        if (targetQuantity == null || targetQuantity <= 0) return 0
        val percentage = (currentQuantity.toFloat() / targetQuantity.toFloat()) * 100
        return percentage.toInt().coerceIn(0, 100)
    }
    
    /**
     * Returns the remaining quantity needed to reach the target.
     */
    fun remainingQuantity(): Int {
        if (targetQuantity == null) return 0
        return (targetQuantity - currentQuantity).coerceAtLeast(0)
    }
}