package com.lloir.ornaassistant.data.repository

import android.util.Log
import com.lloir.ornaassistant.data.database.dao.QuestDao
import com.lloir.ornaassistant.data.database.entities.QuestEntity
import com.lloir.ornaassistant.domain.model.Quest
import com.lloir.ornaassistant.domain.model.QuestObjective
import com.lloir.ornaassistant.domain.model.QuestType
import com.lloir.ornaassistant.domain.repository.QuestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of the QuestRepository interface.
 * 
 * This class is responsible for:
 * - Storing and retrieving quest data from the database
 * - Mapping between entity and domain models
 * - Implementing all quest-related operations
 * - Tracking quest progress and completion
 * 
 * It serves as the single source of truth for quest data in the application.
 */
@Singleton
class QuestRepositoryImpl @Inject constructor(
    private val questDao: QuestDao
) : QuestRepository {

    companion object {
        private const val TAG = "QuestRepositoryImpl"
    }

    override fun getAllQuests(): Flow<List<Quest>> {
        return questDao.getAllQuests().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTrackedQuests(): Flow<List<Quest>> {
        return questDao.getTrackedQuests().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getActiveQuests(): Flow<List<Quest>> {
        return questDao.getActiveQuests().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getQuestsByType(questType: QuestType): Flow<List<Quest>> {
        return questDao.getQuestsByType(questType.name).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getQuestById(id: Long): Quest? {
        return questDao.getQuestById(id)?.toDomainModel()
    }

    override suspend fun searchQuests(searchTerm: String): List<Quest> {
        return questDao.searchQuests(searchTerm).map { it.toDomainModel() }
    }

    override suspend fun insertQuest(quest: Quest): Long {
        Log.d(TAG, "Inserting quest: ${quest.name}")
        val id = questDao.insertQuest(quest.toEntity())
        Log.d(TAG, "Inserted with ID: $id")
        return id
    }

    override suspend fun updateQuest(quest: Quest) {
        Log.d(TAG, "Updating quest ID ${quest.id}: ${quest.name}")
        questDao.updateQuest(quest.toEntity())
    }

    override suspend fun deleteQuest(quest: Quest) {
        questDao.deleteQuest(quest.toEntity())
    }

    override suspend fun deleteAllQuests() {
        questDao.deleteAllQuests()
    }

    override suspend fun clearAllTracking() {
        questDao.clearAllTracking()
    }

    override suspend fun updateQuestTracking(questId: Long, isTracked: Boolean) {
        questDao.updateQuestTracking(questId, isTracked)
    }

    override suspend fun getActiveQuestCount(): Int {
        return questDao.getActiveQuestCount()
    }

    override suspend fun getCompletedQuestCount(): Int {
        return questDao.getCompletedQuestCount()
    }

    override suspend fun getQuestCountByType(questType: QuestType): Int {
        return questDao.getQuestCountByType(questType.name)
    }

    override suspend fun getExpiredQuests(): List<Quest> {
        return questDao.getExpiredQuests(LocalDateTime.now()).map { it.toDomainModel() }
    }

    override suspend fun updateObjectiveProgress(questId: Long, objectiveIndex: Int, currentAmount: Int): Quest? {
        val quest = getQuestById(questId) ?: return null
        
        // Create a new list of objectives with the updated one
        val updatedObjectives = quest.objectives.toMutableList()
        if (objectiveIndex < 0 || objectiveIndex >= updatedObjectives.size) {
            Log.e(TAG, "Invalid objective index: $objectiveIndex for quest ID: $questId")
            return null
        }
        
        // Update the objective
        val objective = updatedObjectives[objectiveIndex]
        val updatedObjective = objective.copy(
            currentAmount = currentAmount,
            isCompleted = currentAmount >= objective.targetAmount
        )
        updatedObjectives[objectiveIndex] = updatedObjective
        
        // Check if all objectives are completed
        val allCompleted = updatedObjectives.all { it.isCompleted }
        
        // Create updated quest
        val updatedQuest = quest.copy(
            objectives = updatedObjectives,
            isCompleted = allCompleted,
            completionTime = if (allCompleted && quest.completionTime == null) LocalDateTime.now() else quest.completionTime
        )
        
        // Save to database
        updateQuest(updatedQuest)
        
        return updatedQuest
    }

    override suspend fun completeQuest(questId: Long, completionTime: LocalDateTime): Quest? {
        val quest = getQuestById(questId) ?: return null
        
        // Mark all objectives as completed
        val completedObjectives = quest.objectives.map { objective ->
            objective.copy(
                currentAmount = objective.targetAmount,
                isCompleted = true
            )
        }
        
        // Create updated quest
        val updatedQuest = quest.copy(
            objectives = completedObjectives,
            isCompleted = true,
            completionTime = completionTime
        )
        
        // Save to database
        updateQuest(updatedQuest)
        
        return updatedQuest
    }
}

// Extension functions for mapping
private fun QuestEntity.toDomainModel(): Quest {
    return Quest(
        id = id,
        name = name,
        description = description,
        objectives = objectives,
        rewards = rewards,
        isCompleted = isCompleted,
        isTracked = isTracked,
        questType = questType,
        requiredLevel = requiredLevel,
        unlockRequirements = unlockRequirements,
        startTime = startTime,
        completionTime = completionTime,
        expiryTime = expiryTime,
        locationHint = locationHint,
        questGiver = questGiver
    )
}

private fun Quest.toEntity(): QuestEntity {
    return QuestEntity(
        id = id,
        name = name,
        description = description,
        objectives = objectives,
        rewards = rewards,
        isCompleted = isCompleted,
        isTracked = isTracked,
        questType = questType,
        requiredLevel = requiredLevel,
        unlockRequirements = unlockRequirements,
        startTime = startTime,
        completionTime = completionTime,
        expiryTime = expiryTime,
        locationHint = locationHint,
        questGiver = questGiver
    )
}