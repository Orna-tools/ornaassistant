package com.lloir.ornaassistant.domain.repository

import com.lloir.ornaassistant.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Repository interface for managing dungeon visit data.
 * 
 * This repository is responsible for:
 * - Storing and retrieving dungeon visit records
 * - Providing access to dungeon statistics
 * - Managing dungeon visit filtering and queries
 * - Handling CRUD operations for dungeon visits
 * 
 * It serves as the single source of truth for dungeon-related data
 * in the application, abstracting the data source from the domain layer.
 */
interface DungeonRepository {

    /**
     * Gets all dungeon visits as a Flow for reactive updates.
     * @return Flow of all dungeon visits, ordered by start time descending
     */
    fun getAllVisits(): Flow<List<DungeonVisit>>

    /**
     * Gets all dungeon visits for a specific wayvessel session.
     * @param sessionId The ID of the wayvessel session
     * @return Flow of dungeon visits for the session
     */
    fun getVisitsForSession(sessionId: Long): Flow<List<DungeonVisit>>

    /**
     * Gets dungeon visits between two dates.
     * @param startTime The start of the time range
     * @param endTime The end of the time range
     * @return List of dungeon visits within the time range
     */
    suspend fun getVisitsBetween(startTime: LocalDateTime, endTime: LocalDateTime): List<DungeonVisit>

    /**
     * Gets recent dungeon visits within a specified number of days.
     * @param days Number of days to look back (default: 7)
     * @return List of recent dungeon visits
     */
    suspend fun getRecentVisits(days: Int = 7): List<DungeonVisit>

    /**
     * Gets a specific dungeon visit by ID.
     * @param id The ID of the dungeon visit
     * @return The dungeon visit, or null if not found
     */
    suspend fun getVisitById(id: Long): DungeonVisit?

    /**
     * Inserts a new dungeon visit.
     * @param visit The dungeon visit to insert
     * @return The ID of the inserted visit
     */
    suspend fun insertVisit(visit: DungeonVisit): Long

    /**
     * Updates an existing dungeon visit.
     * @param visit The dungeon visit to update
     */
    suspend fun updateVisit(visit: DungeonVisit)

    /**
     * Deletes a dungeon visit.
     * @param visit The dungeon visit to delete
     */
    suspend fun deleteVisit(visit: DungeonVisit)

    /**
     * Deletes all dungeon visits.
     */
    suspend fun deleteAllVisits()

    /**
     * Gets statistics for dungeon visits since a specified date.
     * @param startDate The date from which to calculate statistics
     * @return Statistics for the specified time period
     */
    suspend fun getStatistics(startDate: LocalDateTime): DungeonStatistics
}

/**
 * Repository interface for managing kingdom member data.
 * 
 * This repository is responsible for:
 * - Storing and retrieving kingdom member information
 * - Tracking active members and their wayvessel sessions
 * - Managing member floor information for party invites
 * - Handling CRUD operations for kingdom members
 * 
 * It provides access to kingdom-related data for features like
 * wayvessel session tracking and party invite management.
 */
interface KingdomRepository {

    /**
     * Gets all kingdom members as a Flow for reactive updates.
     * @return Flow of all kingdom members, ordered by character name
     */
    fun getAllMembers(): Flow<List<KingdomMember>>

    /**
     * Gets a specific kingdom member by character name.
     * @param characterName The character name to search for
     * @return The kingdom member, or null if not found
     */
    suspend fun getMemberByName(characterName: String): KingdomMember?

    /**
     * Gets all currently active kingdom members (with active wayvessel sessions).
     * @return List of active kingdom members
     */
    suspend fun getActiveMembers(): List<KingdomMember>

    /**
     * Inserts a new kingdom member.
     * @param member The kingdom member to insert
     */
    suspend fun insertMember(member: KingdomMember)

    /**
     * Updates an existing kingdom member.
     * @param member The kingdom member to update
     */
    suspend fun updateMember(member: KingdomMember)

    /**
     * Deletes a kingdom member.
     * @param member The kingdom member to delete
     */
    suspend fun deleteMember(member: KingdomMember)

    /**
     * Deletes all kingdom members.
     */
    suspend fun deleteAllMembers()
}

/**
 * Repository interface for managing item assessment data.
 * 
 * This repository is responsible for:
 * - Storing and retrieving item assessment records
 * - Communicating with external APIs for item quality assessment
 * - Managing assessment history and caching
 * - Providing item quality analysis functionality
 * 
 * It handles both local storage of assessment history and the API
 * communication needed to assess new items.
 */
interface ItemAssessmentRepository {

    /**
     * Gets all item assessments as a Flow for reactive updates.
     * @return Flow of all item assessments, ordered by timestamp descending
     */
    fun getAllAssessments(): Flow<List<ItemAssessment>>

    /**
     * Gets assessments for a specific item.
     * @param itemName The name of the item to search for (partial matching supported)
     * @param limit Maximum number of assessments to return (default: 10)
     * @return List of assessments for the item
     */
    suspend fun getAssessmentsForItem(itemName: String, limit: Int = 10): List<ItemAssessment>

    /**
     * Gets a specific assessment by ID.
     * @param id The ID of the assessment
     * @return The assessment, or null if not found
     */
    suspend fun getAssessmentById(id: Long): ItemAssessment?

    /**
     * Inserts a new item assessment.
     * @param assessment The assessment to insert
     * @return The ID of the inserted assessment
     */
    suspend fun insertAssessment(assessment: ItemAssessment): Long

    /**
     * Deletes an item assessment.
     * @param assessment The assessment to delete
     */
    suspend fun deleteAssessment(assessment: ItemAssessment)

    /**
     * Deletes assessments older than the specified number of days.
     * @param daysOld Age threshold in days (default: 30)
     */
    suspend fun deleteOldAssessments(daysOld: Int = 30)

    /**
     * Deletes all item assessments.
     */
    suspend fun deleteAllAssessments()

    /**
     * Assesses an item using the external API.
     * @param itemName The name of the item to assess
     * @param level The level of the item
     * @param attributes Map of attribute names to values
     * @return The assessment result
     */
    suspend fun assessItem(itemName: String, level: Int, attributes: Map<String, Int>): AssessmentResult
}

/**
 * Repository interface for managing application settings.
 * 
 * This repository is responsible for:
 * - Storing and retrieving user preferences
 * - Managing overlay and notification settings
 * - Providing access to settings as both one-time values and reactive flows
 * - Persisting settings changes to device storage
 * 
 * It serves as the single source of truth for application settings,
 * abstracting the underlying storage mechanism from the domain layer.
 */
interface SettingsRepository {

    /**
     * Gets the current application settings.
     * @return The current settings
     */
    suspend fun getSettings(): AppSettings

    /**
     * Updates all application settings.
     * @param settings The new settings to apply
     */
    suspend fun updateSettings(settings: AppSettings)

    /**
     * Updates the assessment overlay visibility setting.
     * @param enabled Whether the assessment overlay should be shown
     */
    suspend fun updateAssessOverlay(enabled: Boolean)

    /**
     * Updates the overlay transparency setting.
     * @param transparency The transparency value (0.0-1.0)
     */
    suspend fun updateOverlayTransparency(transparency: Float)

    /**
     * Updates the debug mode setting.
     * @param enabled Whether debug mode should be enabled
     */
    suspend fun updateDebugMode(enabled: Boolean)

    /**
     * Updates the ML Kit screen reader setting.
     * @param enabled Whether ML Kit screen reader should be enabled
     */
    suspend fun updateUseMlKit(enabled: Boolean)

    /**
     * Updates the material tracking feature setting.
     * @param enabled Whether material tracking should be enabled
     */
    suspend fun updateEnableMaterialTracking(enabled: Boolean)

    /**
     * Gets the application settings as a Flow for reactive updates.
     * @return Flow of application settings
     */
    fun getSettingsFlow(): Flow<AppSettings>
}

/**
 * Repository interface for managing application notifications.
 * 
 * This repository is responsible for:
 * - Showing and hiding service notifications
 * - Displaying overlay-related notifications
 * - Managing notification channels and priorities
 * - Handling notification lifecycle
 * 
 * It abstracts the Android notification system from the domain layer,
 * providing a clean interface for notification management.
 */
interface NotificationRepository {

    /**
     * Shows the persistent service notification.
     * This notification indicates that the accessibility service is running.
     */
    suspend fun showServiceNotification()

    /**
     * Hides the persistent service notification.
     * This is called when the accessibility service is stopped.
     */
    suspend fun hideServiceNotification()

    /**
     * Shows a temporary overlay notification with a custom message.
     * @param message The message to display in the notification
     */
    suspend fun showOverlayNotification(message: String)
}

/**
 * Repository interface for managing quest data.
 * 
 * This repository is responsible for:
 * - Storing and retrieving quest information
 * - Tracking quest progress and completion
 * - Managing quest objectives and rewards
 * - Providing quest filtering and search functionality
 * 
 * It serves as the single source of truth for quest-related data
 * in the application, abstracting the data source from the domain layer.
 */
interface QuestRepository {

    /**
     * Gets all quests as a Flow for reactive updates.
     * @return Flow of all quests, ordered by tracking status and type
     */
    fun getAllQuests(): Flow<List<Quest>>

    /**
     * Gets all tracked quests as a Flow for reactive updates.
     * @return Flow of tracked quests, ordered by type
     */
    fun getTrackedQuests(): Flow<List<Quest>>

    /**
     * Gets all active (not completed) quests as a Flow for reactive updates.
     * @return Flow of active quests, ordered by tracking status and type
     */
    fun getActiveQuests(): Flow<List<Quest>>

    /**
     * Gets quests of a specific type as a Flow for reactive updates.
     * @param questType The type of quests to retrieve
     * @return Flow of quests of the specified type
     */
    fun getQuestsByType(questType: QuestType): Flow<List<Quest>>

    /**
     * Gets a specific quest by ID.
     * @param id The ID of the quest
     * @return The quest, or null if not found
     */
    suspend fun getQuestById(id: Long): Quest?

    /**
     * Searches for quests by name or description.
     * @param searchTerm The term to search for
     * @return List of quests matching the search term
     */
    suspend fun searchQuests(searchTerm: String): List<Quest>

    /**
     * Inserts a new quest.
     * @param quest The quest to insert
     * @return The ID of the inserted quest
     */
    suspend fun insertQuest(quest: Quest): Long

    /**
     * Updates an existing quest.
     * @param quest The quest to update
     */
    suspend fun updateQuest(quest: Quest)

    /**
     * Deletes a quest.
     * @param quest The quest to delete
     */
    suspend fun deleteQuest(quest: Quest)

    /**
     * Deletes all quests.
     */
    suspend fun deleteAllQuests()

    /**
     * Clears tracking for all quests.
     */
    suspend fun clearAllTracking()

    /**
     * Updates the tracking status of a quest.
     * @param questId The ID of the quest
     * @param isTracked Whether the quest should be tracked
     */
    suspend fun updateQuestTracking(questId: Long, isTracked: Boolean)

    /**
     * Gets the count of active quests.
     * @return The number of active quests
     */
    suspend fun getActiveQuestCount(): Int

    /**
     * Gets the count of completed quests.
     * @return The number of completed quests
     */
    suspend fun getCompletedQuestCount(): Int

    /**
     * Gets the count of quests of a specific type.
     * @param questType The type of quests to count
     * @return The number of quests of the specified type
     */
    suspend fun getQuestCountByType(questType: QuestType): Int

    /**
     * Gets all expired quests that haven't been completed.
     * @return List of expired quests
     */
    suspend fun getExpiredQuests(): List<Quest>

    /**
     * Updates the progress of a quest objective.
     * @param questId The ID of the quest
     * @param objectiveIndex The index of the objective in the quest's objectives list
     * @param currentAmount The new current amount for the objective
     * @return The updated quest
     */
    suspend fun updateObjectiveProgress(questId: Long, objectiveIndex: Int, currentAmount: Int): Quest?

    /**
     * Marks a quest as completed.
     * @param questId The ID of the quest
     * @param completionTime The time when the quest was completed (default: now)
     * @return The updated quest
     */
    suspend fun completeQuest(questId: Long, completionTime: LocalDateTime = LocalDateTime.now()): Quest?
}
