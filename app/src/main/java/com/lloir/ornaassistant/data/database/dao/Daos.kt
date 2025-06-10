package com.lloir.ornaassistant.data.database.dao

import androidx.room.*
import com.lloir.ornaassistant.data.database.entities.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface DungeonVisitDao {
    @Query("SELECT * FROM dungeon_visits ORDER BY startTime DESC")
    fun getAllVisits(): Flow<List<DungeonVisitEntity>>

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
interface WayvesselSessionDao {
    @Query("SELECT * FROM wayvessel_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WayvesselSessionEntity>>

    @Query("""
        SELECT * FROM wayvessel_sessions 
        WHERE name = :name 
        ORDER BY startTime DESC 
        LIMIT :limit
    """)
    suspend fun getLastSessionsFor(name: String, limit: Int): List<WayvesselSessionEntity>

    @Query("""
        SELECT * FROM wayvessel_sessions 
        ORDER BY startTime DESC 
        LIMIT :limit
    """)
    suspend fun getLastSessions(limit: Int): List<WayvesselSessionEntity>

    @Query("""
        SELECT * FROM wayvessel_sessions 
        WHERE startTime BETWEEN :startTime AND :endTime 
        ORDER BY startTime DESC
    """)
    suspend fun getSessionsBetween(
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<WayvesselSessionEntity>

    @Query("SELECT * FROM wayvessel_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): WayvesselSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WayvesselSessionEntity): Long

    @Update
    suspend fun updateSession(session: WayvesselSessionEntity)

    @Delete
    suspend fun deleteSession(session: WayvesselSessionEntity)

    @Query("DELETE FROM wayvessel_sessions")
    suspend fun deleteAllSessions()
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