package com.lloir.ornaassistant.domain.usecase

import com.lloir.ornaassistant.domain.model.*
import com.lloir.ornaassistant.domain.repository.QuestRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for retrieving all quests.
 * 
 * This use case provides access to all quests in the system,
 * ordered by tracking status and type.
 */
@Singleton
class GetAllQuestsUseCase @Inject constructor(
    private val questRepository: QuestRepository
) {
    operator fun invoke(): Flow<List<Quest>> {
        return questRepository.getAllQuests()
    }
}

/**
 * Use case for retrieving tracked quests.
 * 
 * This use case provides access to quests that the user is actively tracking,
 * which are displayed in the quest tracker overlay.
 */
@Singleton
class GetTrackedQuestsUseCase @Inject constructor(
    private val questRepository: QuestRepository
) {
    operator fun invoke(): Flow<List<Quest>> {
        return questRepository.getTrackedQuests()
    }
}

/**
 * Use case for retrieving active (not completed) quests.
 * 
 * This use case provides access to quests that are still in progress,
 * ordered by tracking status and type.
 */
@Singleton
class GetActiveQuestsUseCase @Inject constructor(
    private val questRepository: QuestRepository
) {
    operator fun invoke(): Flow<List<Quest>> {
        return questRepository.getActiveQuests()
    }
}

/**
 * Use case for retrieving quests of a specific type.
 * 
 * This use case provides access to quests filtered by their type
 * (main, side, daily, etc.), ordered by tracking status.
 */
@Singleton
class GetQuestsByTypeUseCase @Inject constructor(
    private val questRepository: QuestRepository
) {
    operator fun invoke(questType: QuestType): Flow<List<Quest>> {
        return questRepository.getQuestsByType(questType)
    }
}

/**
 * Use case for creating a new quest.
 * 
 * This use case handles the creation of a new quest with all its
 * objectives and rewards, and returns the created quest with its ID.
 */
@Singleton
class CreateQuestUseCase @Inject constructor(
    private val questRepository: QuestRepository
) {
    suspend operator fun invoke(
        name: String,
        description: String,
        objectives: List<QuestObjective>,
        rewards: QuestRewards,
        questType: QuestType,
        requiredLevel: Int = 1,
        unlockRequirements: String = "",
        locationHint: String = "",
        questGiver: String = "",
        expiryTime: LocalDateTime? = null,
        isTracked: Boolean = false
    ): Quest {
        val quest = Quest(
            name = name,
            description = description,
            objectives = objectives,
            rewards = rewards,
            questType = questType,
            requiredLevel = requiredLevel,
            unlockRequirements = unlockRequirements,
            locationHint = locationHint,
            questGiver = questGiver,
            startTime = LocalDateTime.now(),
            expiryTime = expiryTime,
            isTracked = isTracked,
            isCompleted = false
        )
        
        val id = questRepository.insertQuest(quest)
        return quest.copy(id = id)
    }
}

/**
 * Use case for updating quest tracking status.
 * 
 * This use case handles toggling whether a quest is being tracked
 * by the user, which affects its visibility in the quest tracker overlay.
 */
@Singleton
class ToggleQuestTrackingUseCase @Inject constructor(
    private val questRepository: QuestRepository
) {
    suspend operator fun invoke(questId: Long, isTracked: Boolean) {
        questRepository.updateQuestTracking(questId, isTracked)
    }
}

/**
 * Use case for updating objective progress.
 * 
 * This use case handles updating the progress of a specific objective
 * within a quest, and automatically marks objectives and quests as
 * completed when appropriate.
 */
@Singleton
class UpdateObjectiveProgressUseCase @Inject constructor(
    private val questRepository: QuestRepository
) {
    suspend operator fun invoke(questId: Long, objectiveIndex: Int, currentAmount: Int): Quest? {
        return questRepository.updateObjectiveProgress(questId, objectiveIndex, currentAmount)
    }
}

/**
 * Use case for completing a quest.
 * 
 * This use case handles marking a quest as completed, including
 * all its objectives, and records the completion time.
 */
@Singleton
class CompleteQuestUseCase @Inject constructor(
    private val questRepository: QuestRepository
) {
    suspend operator fun invoke(questId: Long): Quest? {
        return questRepository.completeQuest(questId)
    }
}

/**
 * Use case for retrieving quest statistics.
 * 
 * This use case provides statistics about quests, including counts
 * of active, completed, and quests by type.
 */
@Singleton
class GetQuestStatisticsUseCase @Inject constructor(
    private val questRepository: QuestRepository
) {
    suspend operator fun invoke(): QuestStatistics {
        val activeCount = questRepository.getActiveQuestCount()
        val completedCount = questRepository.getCompletedQuestCount()
        
        val mainQuestCount = questRepository.getQuestCountByType(QuestType.MAIN)
        val sideQuestCount = questRepository.getQuestCountByType(QuestType.SIDE)
        val dailyQuestCount = questRepository.getQuestCountByType(QuestType.DAILY)
        val weeklyQuestCount = questRepository.getQuestCountByType(QuestType.WEEKLY)
        val eventQuestCount = questRepository.getQuestCountByType(QuestType.EVENT)
        val kingdomQuestCount = questRepository.getQuestCountByType(QuestType.KINGDOM)
        val achievementQuestCount = questRepository.getQuestCountByType(QuestType.ACHIEVEMENT)
        
        return QuestStatistics(
            activeQuestCount = activeCount,
            completedQuestCount = completedCount,
            totalQuestCount = activeCount + completedCount,
            mainQuestCount = mainQuestCount,
            sideQuestCount = sideQuestCount,
            dailyQuestCount = dailyQuestCount,
            weeklyQuestCount = weeklyQuestCount,
            eventQuestCount = eventQuestCount,
            kingdomQuestCount = kingdomQuestCount,
            achievementQuestCount = achievementQuestCount
        )
    }
}

/**
 * Statistics about quests in the system.
 * 
 * This data class provides counts of quests by status and type,
 * which can be used for displaying summary information to the user.
 */
data class QuestStatistics(
    val activeQuestCount: Int,
    val completedQuestCount: Int,
    val totalQuestCount: Int,
    val mainQuestCount: Int,
    val sideQuestCount: Int,
    val dailyQuestCount: Int,
    val weeklyQuestCount: Int,
    val eventQuestCount: Int,
    val kingdomQuestCount: Int,
    val achievementQuestCount: Int
)