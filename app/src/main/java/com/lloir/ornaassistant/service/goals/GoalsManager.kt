package com.lloir.ornaassistant.service.goals

import com.lloir.ornaassistant.domain.model.*
import com.lloir.ornaassistant.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalsManager @Inject constructor(
    private val dungeonRepository: DungeonRepository,
    private val itemAssessmentRepository: ItemAssessmentRepository,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob())
    
    private val _dailyGoals = MutableStateFlow<List<DailyGoal>>(emptyList())
    val dailyGoals: StateFlow<List<DailyGoal>> = _dailyGoals.asStateFlow()

    private val _weeklyProgress = MutableStateFlow(WeeklyProgress())
    val weeklyProgress: StateFlow<WeeklyProgress> = _weeklyProgress.asStateFlow()

    init {
        initializeDailyGoals()
        observeProgress()
    }

    private fun initializeDailyGoals() {
        val defaultGoals = listOf(
            DailyGoal(
                type = GoalType.ORNS_EARNED,
                description = "Earn 10 million orns",
                targetValue = 10_000_000,
                currentValue = 0,
                isCompleted = false
            ),
            DailyGoal(
                type = GoalType.DUNGEONS_CLEARED,
                description = "Clear 20 dungeons",
                targetValue = 20,
                currentValue = 0,
                isCompleted = false
            ),
            DailyGoal(
                type = GoalType.ORNATES_FOUND,
                description = "Find 1 ornate item",
                targetValue = 1,
                currentValue = 0,
                isCompleted = false
            ),
            DailyGoal(
                type = GoalType.EXP_EARNED,
                description = "Earn 1 million experience",
                targetValue = 1_000_000,
                currentValue = 0,
                isCompleted = false
            ),
            DailyGoal(
                type = GoalType.PLAYTIME_MINUTES,
                description = "Play for 60 minutes",
                targetValue = 60,
                currentValue = 0,
                isCompleted = false
            )
        )

        _dailyGoals.value = defaultGoals
    }

    private fun observeProgress() {
        dungeonRepository.getAllVisits()
            .onEach { updateDungeonGoals() }
            .launchIn(scope)

        itemAssessmentRepository.getAllAssessments()
            .onEach { updateItemGoals() }
            .launchIn(scope)
    }

    private suspend fun updateDungeonGoals() {
        // Implementation to update dungeon-related goals
    }

    private suspend fun updateItemGoals() {
        // Implementation to update item-related goals
    }

    suspend fun updateGoalProgress(type: GoalType, value: Long) {
        val goals = _dailyGoals.value.toMutableList()
        val goalIndex = goals.indexOfFirst { it.type == type && !it.isCompleted }
        
        if (goalIndex != -1) {
            val goal = goals[goalIndex]
            val newValue = goal.currentValue + value
            val isCompleted = newValue >= goal.targetValue
            
            goals[goalIndex] = goal.copy(
                currentValue = newValue,
                isCompleted = isCompleted,
                completedAt = if (isCompleted) LocalDateTime.now() else null
            )
            
            _dailyGoals.value = goals
        }
    }
}
