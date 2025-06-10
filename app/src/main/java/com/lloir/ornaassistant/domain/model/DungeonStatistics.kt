package com.lloir.ornaassistant.domain.model

/**
 * Statistics model for dungeon visits over a specific time period
 */
data class DungeonStatistics(
    val totalVisits: Int,
    val completedVisits: Int,
    val failedVisits: Int,
    val totalOrns: Long,
    val totalGold: Long,
    val totalExperience: Long,
    val averageDuration: Long, // in seconds
    val favoriteMode: DungeonMode.Type,
    val completionRate: Float // 0.0 to 1.0
)

/**
 * Weekly statistics breakdown for charts and analysis
 */
data class WeeklyStatistics(
    val mondayVisits: Int,
    val tuesdayVisits: Int,
    val wednesdayVisits: Int,
    val thursdayVisits: Int,
    val fridayVisits: Int,
    val saturdayVisits: Int,
    val sundayVisits: Int,
    val mondayOrns: Long,
    val tuesdayOrns: Long,
    val wednesdayOrns: Long,
    val thursdayOrns: Long,
    val fridayOrns: Long,
    val saturdayOrns: Long,
    val sundayOrns: Long
)