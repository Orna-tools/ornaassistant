package com.lloir.ornaassistant.domain.model

import java.time.LocalDateTime
import java.time.DayOfWeek

// Efficiency Tracking
data class EfficiencyMetrics(
    val id: Long = 0,
    val sessionId: Long? = null,
    val timestamp: LocalDateTime,
    val ornsPerHour: Long,
    val expPerHour: Long,
    val goldPerHour: Long,
    val dungeonsPerHour: Float,
    val itemsPerHour: Float
)

// Combat Log
data class CombatLogEntry(
    val id: Long = 0,
    val timestamp: LocalDateTime,
    val type: LogType,
    val message: String,
    val value: Long? = null,
    val itemQuality: Double? = null,
    val dungeonName: String? = null
)

enum class LogType {
    BATTLE_START, VICTORY, DEFEAT,
    LOOT_ORNS, LOOT_GOLD, LOOT_EXP,
    LOOT_ITEM, ORNATE_FOUND, GODFORGE_FOUND,
    LEVEL_UP, DUNGEON_COMPLETE, FLOOR_CLEARED,
    BUFF_GAINED, DEBUFF_GAINED
}

// Dungeon Cooldowns
data class DungeonCooldown(
    val dungeonName: String,
    val lastVisitTime: LocalDateTime,
    val cooldownHours: Int,
    val cooldownEndTime: LocalDateTime,
    val isReady: Boolean
)

// Daily Goals
data class DailyGoal(
    val id: Long = 0,
    val type: GoalType,
    val description: String,
    val targetValue: Long,
    val currentValue: Long,
    val isCompleted: Boolean,
    val completedAt: LocalDateTime? = null,
    val streak: Int = 0
)

enum class GoalType {
    ORNS_EARNED, EXP_EARNED, GOLD_EARNED,
    DUNGEONS_CLEARED, ITEMS_FOUND, ORNATES_FOUND,
    GODFORGES_FOUND, BOSSES_DEFEATED, PVP_WINS,
    PLAYTIME_MINUTES
}

// Drop Analytics
data class DropAnalytics(
    val id: Long = 0,
    val timestamp: LocalDateTime,
    val itemName: String,
    val quality: Double,
    val dungeonName: String?,
    val timeOfDay: Int, // 0-23
    val dayOfWeek: DayOfWeek,
    val moonPhase: MoonPhase? = null // For the superstitious!
)

enum class MoonPhase {
    NEW_MOON, WAXING_CRESCENT, FIRST_QUARTER, WAXING_GIBBOUS,
    FULL_MOON, WANING_GIBBOUS, LAST_QUARTER, WANING_CRESCENT
}

data class DropPattern(
    val timeRange: String, // "Morning", "Afternoon", etc.
    val ornateRate: Float,
    val godforgeRate: Float,
    val avgQuality: Double,
    val bestDungeon: String?,
    val totalDrops: Int
)

// Screenshot Records
data class ScreenshotRecord(
    val id: Long = 0,
    val timestamp: LocalDateTime,
    val filename: String,
    val type: ScreenshotType,
    val itemName: String?,
    val quality: Double?,
    val filePath: String
)

enum class ScreenshotType {
    ORNATE, GODFORGE, ACHIEVEMENT, LEVEL_UP, MANUAL
}

// Efficiency Milestones
data class EfficiencyMilestone(
    val type: MilestoneType,
    val value: Long,
    val unit: String,
    val message: String
)

enum class MilestoneType {
    ORNS_PER_HOUR, EXP_PER_HOUR, DUNGEONS_PER_HOUR
}

// Weekly Progress
data class WeeklyProgress(
    val completedGoals: Int = 0,
    val totalGoals: Int = 0,
    val weeklyOrns: Long = 0,
    val weeklyExp: Long = 0,
    val weeklyDungeons: Int = 0
)
