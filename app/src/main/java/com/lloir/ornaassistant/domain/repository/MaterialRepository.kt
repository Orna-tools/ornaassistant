package com.lloir.ornaassistant.domain.repository

import com.lloir.ornaassistant.domain.model.Material
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Repository interface for materials.
 * 
 * This interface provides methods to interact with the materials data.
 * It serves as the single source of truth for material-related data
 * in the application, abstracting the data source from the domain layer.
 */
interface MaterialRepository {
    /**
     * Get all materials as a Flow.
     *
     * @return Flow of all materials
     */
    fun getAllMaterials(): Flow<List<Material>>
    
    /**
     * Get all tracked materials as a Flow.
     *
     * @return Flow of tracked materials
     */
    fun getTrackedMaterials(): Flow<List<Material>>
    
    /**
     * Get a material by its name.
     *
     * @param name The name of the material to get
     * @return The material with the specified name, or null if not found
     */
    suspend fun getMaterialByName(name: String): Material?
    
    /**
     * Get a material by its ID.
     *
     * @param id The ID of the material to get
     * @return The material with the specified ID, or null if not found
     */
    suspend fun getMaterialById(id: Long): Material?
    
    /**
     * Insert a new material.
     *
     * @param material The material to insert
     * @return The ID of the inserted material
     */
    suspend fun insertMaterial(material: Material): Long
    
    /**
     * Update an existing material.
     *
     * @param material The material to update
     */
    suspend fun updateMaterial(material: Material)
    
    /**
     * Delete a material.
     *
     * @param material The material to delete
     */
    suspend fun deleteMaterial(material: Material)
    
    /**
     * Delete all materials.
     */
    suspend fun deleteAllMaterials()
    
    /**
     * Update the tracking status of a material.
     *
     * @param materialId The ID of the material to update
     * @param isTracked Whether the material should be tracked
     */
    suspend fun updateMaterialTracking(materialId: Long, isTracked: Boolean)
    
    /**
     * Update the target quantity of a material.
     *
     * @param materialId The ID of the material to update
     * @param targetQuantity The new target quantity for the material
     */
    suspend fun updateMaterialTarget(materialId: Long, targetQuantity: Int)
    
    /**
     * Update the current quantity of a material.
     *
     * @param materialId The ID of the material to update
     * @param currentQuantity The new current quantity for the material
     */
    suspend fun updateMaterialQuantity(materialId: Long, currentQuantity: Int)
    
    /**
     * Get the count of tracked materials.
     *
     * @return The count of tracked materials
     */
    suspend fun getTrackedMaterialsCount(): Int
    
    /**
     * Search for materials by name.
     *
     * @param searchTerm The term to search for
     * @return List of materials matching the search term
     */
    suspend fun searchMaterials(searchTerm: String): List<Material>
}