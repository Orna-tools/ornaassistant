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