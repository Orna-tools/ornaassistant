package com.lloir.ornaassistant.data.database.dao

import androidx.room.*
import com.lloir.ornaassistant.data.database.entities.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Data Access Object (DAO) for dungeon visit entities.
 * 
 * This DAO provides methods for:
 * - Retrieving dungeon visits with various filtering options
 * - Inserting, updating, and deleting dungeon visit records
 * - Calculating statistics about dungeon visits
 * 
 * It uses Room's query annotations to define SQL operations and
 * supports both one-time suspending functions and reactive Flow returns.
 */
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

/**
 * Data Access Object (DAO) for kingdom member entities.
 * 
 * This DAO provides methods for:
 * - Retrieving kingdom members with various filtering options
 * - Managing active members with wayvessel sessions
 * - Inserting, updating, and deleting kingdom member records
 * 
 * It supports both one-time suspending functions for immediate operations
 * and reactive Flow returns for observing changes to the data.
 */
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

/**
 * Data Access Object (DAO) for item assessment entities.
 * 
 * This DAO provides methods for:
 * - Retrieving item assessments with filtering and search options
 * - Managing assessment history with automatic cleanup
 * - Inserting, updating, and deleting assessment records
 * 
 * It supports fuzzy text search for finding assessments by item name
 * and includes methods for cleaning up old assessment data to prevent
 * database bloat.
 */
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

/**
 * Data Access Object (DAO) for quest entities.
 * 
 * This DAO provides methods for:
 * - Retrieving quests with various filtering options
 * - Managing quest tracking and progress
 * - Inserting, updating, and deleting quest records
 * 
 * It supports both one-time suspending functions for immediate operations
 * and reactive Flow returns for observing changes to quest data in real-time.
 */
@Dao
interface QuestDao {
    @Query("SELECT * FROM quests ORDER BY isTracked DESC, questType ASC, name ASC")
    fun getAllQuests(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE isTracked = 1 ORDER BY questType ASC, name ASC")
    fun getTrackedQuests(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE isCompleted = 0 ORDER BY isTracked DESC, questType ASC, name ASC")
    fun getActiveQuests(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE questType = :questType ORDER BY isTracked DESC, name ASC")
    fun getQuestsByType(questType: String): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE id = :id")
    suspend fun getQuestById(id: Long): QuestEntity?

    @Query("SELECT * FROM quests WHERE name LIKE '%' || :searchTerm || '%' OR description LIKE '%' || :searchTerm || '%'")
    suspend fun searchQuests(searchTerm: String): List<QuestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: QuestEntity): Long

    @Update
    suspend fun updateQuest(quest: QuestEntity)

    @Delete
    suspend fun deleteQuest(quest: QuestEntity)

    @Query("DELETE FROM quests")
    suspend fun deleteAllQuests()

    @Query("UPDATE quests SET isTracked = 0")
    suspend fun clearAllTracking()

    @Query("UPDATE quests SET isTracked = :isTracked WHERE id = :questId")
    suspend fun updateQuestTracking(questId: Long, isTracked: Boolean)

    @Query("SELECT COUNT(*) FROM quests WHERE isCompleted = 0")
    suspend fun getActiveQuestCount(): Int

    @Query("SELECT COUNT(*) FROM quests WHERE isCompleted = 1")
    suspend fun getCompletedQuestCount(): Int

    @Query("SELECT COUNT(*) FROM quests WHERE questType = :questType")
    suspend fun getQuestCountByType(questType: String): Int

    @Query("SELECT * FROM quests WHERE expiryTime IS NOT NULL AND expiryTime < :currentTime AND isCompleted = 0")
    suspend fun getExpiredQuests(currentTime: LocalDateTime): List<QuestEntity>
}
