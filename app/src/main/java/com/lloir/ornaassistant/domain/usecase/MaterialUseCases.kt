package com.lloir.ornaassistant.domain.usecase

import com.lloir.ornaassistant.domain.model.Material
import com.lloir.ornaassistant.domain.repository.MaterialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to get all materials.
 */
@Singleton
class GetAllMaterialsUseCase @Inject constructor(
    private val materialRepository: MaterialRepository
) {
    operator fun invoke(): Flow<List<Material>> {
        return materialRepository.getAllMaterials()
    }
}

/**
 * Use case to get tracked materials.
 */
@Singleton
class GetTrackedMaterialsUseCase @Inject constructor(
    private val materialRepository: MaterialRepository
) {
    operator fun invoke(): Flow<List<Material>> {
        return materialRepository.getTrackedMaterials()
    }
}

/**
 * Use case to track a material with a target quantity.
 */
@Singleton
class TrackMaterialUseCase @Inject constructor(
    private val materialRepository: MaterialRepository
) {
    suspend operator fun invoke(materialId: Long, targetQuantity: Int) {
        materialRepository.updateMaterialTarget(materialId, targetQuantity)
    }
}

/**
 * Use case to stop tracking a material.
 */
@Singleton
class StopTrackingMaterialUseCase @Inject constructor(
    private val materialRepository: MaterialRepository
) {
    suspend operator fun invoke(materialId: Long) {
        materialRepository.updateMaterialTracking(materialId, false)
    }
}

/**
 * Use case to update the quantity of a material.
 */
@Singleton
class UpdateMaterialQuantityUseCase @Inject constructor(
    private val materialRepository: MaterialRepository
) {
    suspend operator fun invoke(materialId: Long, currentQuantity: Int) {
        materialRepository.updateMaterialQuantity(materialId, currentQuantity)
    }
}

/**
 * Use case to check if any tracked materials have reached their targets.
 * Returns a list of materials that have reached their targets.
 */
@Singleton
class CheckMaterialTargetsUseCase @Inject constructor(
    private val materialRepository: MaterialRepository
) {
    suspend operator fun invoke(): List<Material> {
        val trackedMaterials = materialRepository.getTrackedMaterials().first()
        return trackedMaterials.filter { material -> 
            material.targetQuantity != null && material.currentQuantity >= material.targetQuantity
        }
    }
}

/**
 * Use case to get a material by name, or create it if it doesn't exist.
 */
@Singleton
class GetOrCreateMaterialUseCase @Inject constructor(
    private val materialRepository: MaterialRepository
) {
    suspend operator fun invoke(name: String, quantity: Int = 0): Material {
        val existingMaterial = materialRepository.getMaterialByName(name)

        return if (existingMaterial != null) {
            // Update quantity if it's different
            if (existingMaterial.currentQuantity != quantity) {
                materialRepository.updateMaterialQuantity(existingMaterial.id, quantity)
                existingMaterial.copy(currentQuantity = quantity, lastUpdated = LocalDateTime.now())
            } else {
                existingMaterial
            }
        } else {
            // Create new material
            val newMaterial = Material(
                name = name,
                currentQuantity = quantity,
                lastUpdated = LocalDateTime.now()
            )
            val id = materialRepository.insertMaterial(newMaterial)
            newMaterial.copy(id = id)
        }
    }
}

/**
 * Use case to search for materials by name.
 */
@Singleton
class SearchMaterialsUseCase @Inject constructor(
    private val materialRepository: MaterialRepository
) {
    suspend operator fun invoke(searchTerm: String): List<Material> {
        return materialRepository.searchMaterials(searchTerm)
    }
}
