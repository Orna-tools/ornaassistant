package com.lloir.ornaassistant.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.lloir.ornaassistant.data.database.dao.DungeonVisitDao
import com.lloir.ornaassistant.data.database.entities.DungeonVisitEntity
//import com.lloir.ornaassistant.domain.model.DungeonStatistics
import com.lloir.ornaassistant.domain.model.DungeonVisit
import com.lloir.ornaassistant.domain.repository.DungeonRepository
import com.lloir.ornaassistant.domain.repository.DungeonStatistics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DungeonRepositoryImpl @Inject constructor(
    private val dungeonVisitDao: DungeonVisitDao
) : DungeonRepository {

    override fun getAllVisits(): Flow<List<DungeonVisit>> {
        return dungeonVisitDao.getAllVisits().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getVisitsForSession(sessionId: Long): Flow<List<DungeonVisit>> {
        return dungeonVisitDao.getVisitsForSession(sessionId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getVisitsBetween(startTime: LocalDateTime, endTime: LocalDateTime): List<DungeonVisit> {
        return dungeonVisitDao.getVisitsBetween(startTime, endTime).map { it.toDomainModel() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getRecentVisits(days: Int): List<DungeonVisit> {
        val startDate = LocalDateTime.now().minusDays(days.toLong())
        return dungeonVisitDao.getRecentVisits(startDate, 100).map { it.toDomainModel() }
    }

    override suspend fun getVisitById(id: Long): DungeonVisit? {
        return dungeonVisitDao.getVisitById(id)?.toDomainModel()
    }

    override suspend fun insertVisit(visit: DungeonVisit): Long {
        return dungeonVisitDao.insertVisit(visit.toEntity())
    }

    override suspend fun updateVisit(visit: DungeonVisit) {
        dungeonVisitDao.updateVisit(visit.toEntity())
    }

    override suspend fun deleteVisit(visit: DungeonVisit) {
        dungeonVisitDao.deleteVisit(visit.toEntity())
    }

    override suspend fun deleteAllVisits() {
        dungeonVisitDao.deleteAllVisits()
    }

    override suspend fun getStatistics(startDate: LocalDateTime): DungeonStatistics {
        val totalVisits = dungeonVisitDao.getCompletedVisitsCount(startDate)
        val completedVisits = dungeonVisitDao.getCompletedVisitsCount(startDate)
        val totalOrns = dungeonVisitDao.getTotalOrnsEarned(startDate) ?: 0L
        val totalExperience = dungeonVisitDao.getTotalExperienceEarned(startDate) ?: 0L

        return DungeonStatistics(
            totalVisits = totalVisits,
            completedVisits = completedVisits,
            failedVisits = totalVisits - completedVisits,
            totalOrns = totalOrns,
            totalGold = 0L, // Add implementation if needed
            totalExperience = totalExperience,
            averageDuration = 0L, // Add implementation if needed
            favoriteMode = com.lloir.ornaassistant.domain.model.DungeonMode.Type.NORMAL, // Add implementation
            completionRate = if (totalVisits > 0) completedVisits.toFloat() / totalVisits else 0f
        )
    }
}

// Extension functions for mapping
private fun DungeonVisitEntity.toDomainModel(): DungeonVisit {
    return DungeonVisit(
        id = id,
        sessionId = sessionId,
        name = name,
        mode = mode,
        startTime = startTime,
        durationSeconds = durationSeconds,
        orns = orns,
        gold = gold,
        experience = experience,
        floor = floor,
        godforges = godforges,
        completed = completed
    )
}

private fun DungeonVisit.toEntity(): DungeonVisitEntity {
    return DungeonVisitEntity(
        id = id,
        sessionId = sessionId,
        name = name,
        mode = mode,
        startTime = startTime,
        durationSeconds = durationSeconds,
        orns = orns,
        gold = gold,
        experience = experience,
        floor = floor,
        godforges = godforges,
        completed = completed
    )
}

