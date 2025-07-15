package com.lloir.ornaassistant.data.database.dao

import androidx.room.*
import com.lloir.ornaassistant.data.database.entities.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface DungeonVisitDao {
    @Query("SELECT * FROM dungeon_visits ORDER BY startTime DESC")
    fun getAllVisits(): Flow<List<DungeonVisitEntity>>

    @Query("SELECT * FROM dungeon_visits ORDER BY startTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getVisitsPaginated(limit: Int, offset: Int): List<DungeonVisitEntity>

    @Query("SELECT COUNT(*) FROM dungeon_visits")
    suspend fun getVisitsCount(): Int

    @Query("SELECT * FROM dungeon_visits WHERE sessionId = :sessionId ORDER BY startTime DESC")
    fun getVisitsForSession(sessionId: Long): Flow<List<DungeonVisitEntity>>

    @Query("""
        SELECT * FROM dungeon_visits 
        WHERE startTime BETWEEN :startTime AND :endTime 
        ORDER BY startTime DESC
    """)
    suspend fun getVisitsBetween(
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<DungeonVisitEntity>

    @Query("""
        SELECT * FROM dungeon_visits 
        WHERE startTime BETWEEN :startTime AND :endTime 
        ORDER BY startTime DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getVisitsBetweenPaginated(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        limit: Int,
        offset: Int
    ): List<DungeonVisitEntity>

    @Query("""
        SELECT COUNT(*) FROM dungeon_visits 
        WHERE startTime BETWEEN :startTime AND :endTime
    """)
    suspend fun getVisitsBetweenCount(
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Int

    @Query("""
        SELECT * FROM dungeon_visits 
        WHERE startTime >= :startDate 
        ORDER BY startTime DESC 
        LIMIT :limit
    """)
    suspend fun getRecentVisits(startDate: LocalDateTime, limit: Int): List<DungeonVisitEntity>

    @Query("SELECT * FROM dungeon_visits WHERE id = :id")
    suspend fun getVisitById(id: Long): DungeonVisitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: DungeonVisitEntity): Long

    @Update
    suspend fun updateVisit(visit: DungeonVisitEntity)

    @Delete
    suspend fun deleteVisit(visit: DungeonVisitEntity)

    @Query("DELETE FROM dungeon_visits")
    suspend fun deleteAllVisits()

    @Query("""
        SELECT COUNT(*) FROM dungeon_visits 
        WHERE startTime >= :startDate AND completed = 1
    """)
    suspend fun getCompletedVisitsCount(startDate: LocalDateTime): Int

    @Query("""
        SELECT SUM(orns) FROM dungeon_visits 
        WHERE startTime >= :startDate AND completed = 1
    """)
    suspend fun getTotalOrnsEarned(startDate: LocalDateTime): Long?

    @Query("""
        SELECT SUM(experience) FROM dungeon_visits 
        WHERE startTime >= :startDate AND completed = 1
    """)
    suspend fun getTotalExperienceEarned(startDate: LocalDateTime): Long?
}

@Dao
interface KingdomMemberDao {
    @Query("SELECT * FROM kingdom_members ORDER BY characterName ASC")
    fun getAllMembers(): Flow<List<KingdomMemberEntity>>

    @Query("SELECT * FROM kingdom_members WHERE characterName = :characterName")
    suspend fun getMemberByName(characterName: String): KingdomMemberEntity?

    @Query("""
        SELECT * FROM kingdom_members 
        WHERE endTime > :currentTime 
        ORDER BY endTime ASC
    """)
    suspend fun getActiveMembers(currentTime: LocalDateTime): List<KingdomMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: KingdomMemberEntity)

    @Update
    suspend fun updateMember(member: KingdomMemberEntity)

    @Delete
    suspend fun deleteMember(member: KingdomMemberEntity)

    @Query("DELETE FROM kingdom_members")
    suspend fun deleteAllMembers()
}

@Dao
interface ItemAssessmentDao {
    @Query("SELECT * FROM item_assessments ORDER BY timestamp DESC")
    fun getAllAssessments(): Flow<List<ItemAssessmentEntity>>

    @Query("""
        SELECT * FROM item_assessments 
        WHERE itemName LIKE '%' || :itemName || '%' 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    suspend fun getAssessmentsForItem(itemName: String, limit: Int): List<ItemAssessmentEntity>

    @Query("SELECT * FROM item_assessments WHERE id = :id")
    suspend fun getAssessmentById(id: Long): ItemAssessmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: ItemAssessmentEntity): Long

    @Delete
    suspend fun deleteAssessment(assessment: ItemAssessmentEntity)

    @Query("DELETE FROM item_assessments WHERE timestamp < :cutoffDate")
    suspend fun deleteOldAssessments(cutoffDate: LocalDateTime)

    @Query("DELETE FROM item_assessments")
    suspend fun deleteAllAssessments()
}

@Dao
interface MaterialDao {
    /**
     * Get all materials as a Flow.
     */
    @Query("SELECT * FROM materials ORDER BY name ASC")
    fun getAllMaterials(): Flow<List<MaterialEntity>>

    /**
     * Get materials with pagination.
     */
    @Query("SELECT * FROM materials ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getMaterialsPaginated(limit: Int, offset: Int): List<MaterialEntity>

    /**
     * Get the total count of materials.
     */
    @Query("SELECT COUNT(*) FROM materials")
    suspend fun getMaterialsCount(): Int

    /**
     * Get all tracked materials as a Flow.
     */
    @Query("SELECT * FROM materials WHERE isTracked = 1 ORDER BY name ASC")
    fun getTrackedMaterials(): Flow<List<MaterialEntity>>

    /**
     * Get tracked materials with pagination.
     */
    @Query("SELECT * FROM materials WHERE isTracked = 1 ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getTrackedMaterialsPaginated(limit: Int, offset: Int): List<MaterialEntity>


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

    /**
     * Search for materials by name with pagination.
     */
    @Query("SELECT * FROM materials WHERE name LIKE '%' || :searchTerm || '%' ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun searchMaterialsPaginated(searchTerm: String, limit: Int, offset: Int): List<MaterialEntity>

    /**
     * Get the count of materials matching a search term.
     */
    @Query("SELECT COUNT(*) FROM materials WHERE name LIKE '%' || :searchTerm || '%'")
    suspend fun getSearchMaterialsCount(searchTerm: String): Int

    /**
     * Get all materials that should be displayed on the dashboard.
     */
    @Query("SELECT * FROM materials WHERE displayOnDashboard = 1 ORDER BY name ASC")
    fun getDashboardMaterials(): Flow<List<MaterialEntity>>

    /**
     * Get dashboard materials with pagination.
     */
    @Query("SELECT * FROM materials WHERE displayOnDashboard = 1 ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getDashboardMaterialsPaginated(limit: Int, offset: Int): List<MaterialEntity>

    /**
     * Get the count of materials displayed on the dashboard.
     */
    @Query("SELECT COUNT(*) FROM materials WHERE displayOnDashboard = 1")
    suspend fun getDashboardMaterialsCount(): Int

    /**
     * Update the dashboard display status of a material.
     */
    @Query("UPDATE materials SET displayOnDashboard = :displayOnDashboard WHERE id = :materialId")
    suspend fun updateMaterialDashboardDisplay(materialId: Long, displayOnDashboard: Boolean)
}
