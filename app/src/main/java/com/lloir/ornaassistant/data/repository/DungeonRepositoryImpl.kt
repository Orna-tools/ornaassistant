package com.lloir.ornaassistant.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.lloir.ornaassistant.data.database.dao.DungeonVisitDao
import com.lloir.ornaassistant.data.database.entities.DungeonVisitEntity
import com.lloir.ornaassistant.data.mapper.toDomain
import com.lloir.ornaassistant.data.mapper.toEntity
import com.lloir.ornaassistant.domain.model.DungeonVisit
import com.lloir.ornaassistant.domain.model.DungeonStatistics
import com.lloir.ornaassistant.domain.repository.DungeonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DungeonRepositoryImpl @Inject constructor(
    private val dungeonVisitDao: DungeonVisitDao
) : DungeonRepository {

    companion object {
        private const val TAG = "DungeonRepositoryImpl"
    }

    override fun getAllVisits(): Flow<List<DungeonVisit>> {
        return dungeonVisitDao.getAllVisits().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getVisitsForSession(sessionId: Long): Flow<List<DungeonVisit>> {
        return dungeonVisitDao.getVisitsForSession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getVisitsBetween(startTime: LocalDateTime, endTime: LocalDateTime): List<DungeonVisit> {
        return dungeonVisitDao.getVisitsBetween(startTime, endTime).map { it.toDomain() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getRecentVisits(days: Int): List<DungeonVisit> {
        val startDate = LocalDateTime.now().minusDays(days.toLong())
        return dungeonVisitDao.getRecentVisits(startDate, 100).map { it.toDomain() }
    }

    override suspend fun getVisitById(id: Long): DungeonVisit? {
        return dungeonVisitDao.getVisitById(id)?.toDomain()
    }

    override suspend fun insertVisit(visit: DungeonVisit): Long {
        Log.d(TAG, "Inserting dungeon visit: ${visit.name}")
        Log.d(TAG, "  - Orns: ${visit.orns} (battle: ${visit.battleOrns}, floor: ${visit.floorOrns})")
        Log.d(TAG, "  - Gold: ${visit.gold} (battle: ${visit.battleGold}, floor: ${visit.floorGold})")
        Log.d(TAG, "  - Experience: ${visit.experience} (battle: ${visit.battleExperience}, floor: ${visit.floorExperience})")
        Log.d(TAG, "  - Floor rewards: ${visit.floorRewards}")
        val id = dungeonVisitDao.insertVisit(visit.toEntity())
        Log.d(TAG, "Inserted with ID: $id")
        return id
    }

    override suspend fun updateVisit(visit: DungeonVisit) {
        Log.d(TAG, "Updating dungeon visit ID ${visit.id}: ${visit.name}, orns: ${visit.orns}, gold: ${visit.gold}, exp: ${visit.experience}")
        Log.d(TAG, "  - Floor rewards: ${visit.floorRewards}")
        dungeonVisitDao.updateVisit(visit.toEntity())
    }

    override suspend fun deleteVisit(visit: DungeonVisit) {
        dungeonVisitDao.deleteVisit(visit.toEntity())
    }

    override suspend fun deleteAllVisits() {
        dungeonVisitDao.deleteAllVisits()
    }

    override suspend fun getStatistics(startDate: LocalDateTime): DungeonStatistics {
        // Get all visits since startDate and convert to domain models
        val allVisitEntities = dungeonVisitDao.getVisitsBetween(startDate, LocalDateTime.now())
        val allVisits = allVisitEntities.map { it.toDomain() }

        val totalVisits = allVisits.size
        val completedVisits = allVisits.count { it.completed }
        val failedVisits = totalVisits - completedVisits

        val totalOrns = allVisits.sumOf { it.orns }
        val totalGold = allVisits.sumOf { it.gold }
        val totalExperience = allVisits.sumOf { it.experience }

        // Calculate average duration (only for completed visits with duration > 0)
        val completedWithDuration = allVisits.filter { it.completed && it.durationSeconds > 0 }
        val averageDuration = if (completedWithDuration.isNotEmpty()) {
            completedWithDuration.map { it.durationSeconds }.average().toLong()
        } else {
            0L
        }

        // Find most common dungeon mode type
        val modeFrequency = allVisits.groupingBy { it.mode.type }.eachCount()
        val favoriteMode = modeFrequency.maxByOrNull { it.value }?.key
            ?: com.lloir.ornaassistant.domain.model.DungeonMode.Type.NORMAL

        val completionRate = if (totalVisits > 0) completedVisits.toFloat() / totalVisits else 0f

        return DungeonStatistics(
            totalVisits = totalVisits,
            completedVisits = completedVisits,
            failedVisits = failedVisits,
            totalOrns = totalOrns,
            totalGold = totalGold,
            totalExperience = totalExperience,
            averageDuration = averageDuration,
            favoriteMode = favoriteMode,
            completionRate = completionRate
        )
    }
}

