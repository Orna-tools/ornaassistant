package com.lloir.ornaassistant.domain.repository

import com.lloir.ornaassistant.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface GoalRepository {
    suspend fun getLastGoalDate(): LocalDate?
    suspend fun archiveGoals()
    suspend fun insertGoal(goal: DailyGoal)
    suspend fun updateGoal(goal: DailyGoal)
    suspend fun getStreakForType(type: GoalType): Int
    suspend fun updateStreak(type: GoalType, streak: Int)
}

interface ScreenshotRepository {
    suspend fun insertRecord(record: ScreenshotRecord)
    fun getAllRecords(): Flow<List<ScreenshotRecord>>
    suspend fun deleteRecord(record: ScreenshotRecord)
}
