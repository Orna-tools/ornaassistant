package com.lloir.ornaassistant.data.database.dao

import androidx.room.*
import com.lloir.ornaassistant.data.database.entities.MaterialEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Data Access Object for the materials table.
 * 
 * This interface provides methods to interact with the materials table in the database.
 */
@Dao
interface MaterialDao {
    /**
     * Get all materials as a Flow.
     */
    @Query("SELECT * FROM materials ORDER BY name ASC")
    fun getAllMaterials(): Flow<List<MaterialEntity>>
    
    /**
     * Get all tracked materials as a Flow.
     */
    @Query("SELECT * FROM materials WHERE isTracked = 1 ORDER BY name ASC")
    fun getTrackedMaterials(): Flow<List<MaterialEntity>>
    
    /**
     * Get a material by its name.
     */
    @Query("SELECT * FROM materials WHERE name = :name LIMIT 1")
    suspend fun getMaterialByName(name: String): MaterialEntity?
    
    /**
     * Get a material by its ID.
     */
    @Query("SELECT * FROM materials WHERE id = :id LIMIT 1")
    suspend fun getMaterialById(id: Long): MaterialEntity?
    
    /**
     * Insert a new material.
     * 
     * @return The ID of the inserted material.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: MaterialEntity): Long
    
    /**
     * Update an existing material.
     */
    @Update
    suspend fun updateMaterial(material: MaterialEntity)
    
    /**
     * Delete a material.
     */
    @Delete
    suspend fun deleteMaterial(material: MaterialEntity)
    
    /**
     * Delete all materials.
     */
    @Query("DELETE FROM materials")
    suspend fun deleteAllMaterials()
    
    /**
     * Update the tracking status of a material.
     */
    @Query("UPDATE materials SET isTracked = :isTracked WHERE id = :materialId")
    suspend fun updateMaterialTracking(materialId: Long, isTracked: Boolean)
    
    /**
     * Update the target quantity of a material.
     */
    @Query("UPDATE materials SET targetQuantity = :targetQuantity, isTracked = 1 WHERE id = :materialId")
    suspend fun updateMaterialTarget(materialId: Long, targetQuantity: Int)
    
    /**
     * Update the current quantity of a material.
     */
    @Query("UPDATE materials SET currentQuantity = :currentQuantity, lastUpdated = :lastUpdated WHERE id = :materialId")
    suspend fun updateMaterialQuantity(materialId: Long, currentQuantity: Int, lastUpdated: LocalDateTime = LocalDateTime.now())
    
    /**
     * Get the count of tracked materials.
     */
    @Query("SELECT COUNT(*) FROM materials WHERE isTracked = 1")
    suspend fun getTrackedMaterialsCount(): Int
    
    /**
     * Search for materials by name.
     */
    @Query("SELECT * FROM materials WHERE name LIKE '%' || :searchTerm || '%' ORDER BY name ASC")
    suspend fun searchMaterials(searchTerm: String): List<MaterialEntity>
}