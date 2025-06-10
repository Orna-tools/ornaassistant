package com.lloir.ornaassistant.data.repository

import com.lloir.ornaassistant.data.database.dao.DungeonVisitDao
import com.lloir.ornaassistant.data.database.entities.DungeonVisitEntity
import com.lloir.ornaassistant.domain.model.DungeonStatistics
import com.lloir.ornaassistant.domain.model.DungeonVisit
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

// FILE: data/repository/WayvesselRepositoryImpl.kt
package com.lloir.ornaassistant.data.repository

import com.lloir.ornaassistant.data.database.dao.WayvesselSessionDao
import com.lloir.ornaassistant.data.database.entities.WayvesselSessionEntity
import com.lloir.ornaassistant.domain.model.WayvesselSession
import com.lloir.ornaassistant.domain.repository.WayvesselRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WayvesselRepositoryImpl @Inject constructor(
    private val wayvesselSessionDao: WayvesselSessionDao
) : WayvesselRepository {

    override fun getAllSessions(): Flow<List<WayvesselSession>> {
        return wayvesselSessionDao.getAllSessions().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getLastSessionsFor(name: String, limit: Int): List<WayvesselSession> {
        return wayvesselSessionDao.getLastSessionsFor(name, limit).map { it.toDomainModel() }
    }

    override suspend fun getLastSessions(limit: Int): List<WayvesselSession> {
        return wayvesselSessionDao.getLastSessions(limit).map { it.toDomainModel() }
    }

    override suspend fun getSessionsBetween(startTime: LocalDateTime, endTime: LocalDateTime): List<WayvesselSession> {
        return wayvesselSessionDao.getSessionsBetween(startTime, endTime).map { it.toDomainModel() }
    }

    override suspend fun getSessionById(id: Long): WayvesselSession? {
        return wayvesselSessionDao.getSessionById(id)?.toDomainModel()
    }

    override suspend fun insertSession(session: WayvesselSession): Long {
        return wayvesselSessionDao.insertSession(session.toEntity())
    }

    override suspend fun updateSession(session: WayvesselSession) {
        wayvesselSessionDao.updateSession(session.toEntity())
    }

    override suspend fun deleteSession(session: WayvesselSession) {
        wayvesselSessionDao.deleteSession(session.toEntity())
    }

    override suspend fun deleteAllSessions() {
        wayvesselSessionDao.deleteAllSessions()
    }

    override suspend fun getCurrentSession(): WayvesselSession? {
        // Get the most recent active session (durationSeconds = 0)
        return getLastSessions(1).firstOrNull { it.isActive() }
    }
}

private fun WayvesselSessionEntity.toDomainModel(): WayvesselSession {
    return WayvesselSession(
        id = id,
        name = name,
        startTime = startTime,
        durationSeconds = durationSeconds,
        orns = orns,
        gold = gold,
        experience = experience,
        dungeonsVisited = dungeonsVisited
    )
}

private fun WayvesselSession.toEntity(): WayvesselSessionEntity {
    return WayvesselSessionEntity(
        id = id,
        name = name,
        startTime = startTime,
        durationSeconds = durationSeconds,
        orns = orns,
        gold = gold,
        experience = experience,
        dungeonsVisited = dungeonsVisited
    )
}