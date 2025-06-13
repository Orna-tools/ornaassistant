package com.lloir.ornaassistant.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.lloir.ornaassistant.data.database.dao.DungeonVisitDao
import com.lloir.ornaassistant.data.database.entities.DungeonVisitEntity
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
        val allVisits = allVisitEntities.map { it.toDomainModel() }

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

// Extension functions for mapping
private fun DungeonVisitEntity.toDomainModel(): DungeonVisit {
    Log.d("DungeonRepoMapping", "Converting entity to domain: $name")
    Log.d("DungeonRepoMapping", "  - Entity orns: $orns, gold: $gold, exp: $experience")
    Log.d("DungeonRepoMapping", "  - Entity battle orns: $battleOrns, gold: $battleGold, exp: $battleExperience")
    Log.d("DungeonRepoMapping", "  - Entity floor orns: $floorOrns, gold: $floorGold, exp: $floorExperience")
    Log.d("DungeonRepoMapping", "  - Entity floor rewards: $floorRewards")
    
    return DungeonVisit(
        id = id,
        sessionId = sessionId,
        name = name,
        mode = mode.toDomainModel(),
        startTime = startTime,
        durationSeconds = durationSeconds,
        battleOrns = battleOrns,
        battleGold = battleGold,
        battleExperience = battleExperience,
        floorOrns = floorOrns,
        floorGold = floorGold,
        floorExperience = floorExperience,
        orns = orns,
        gold = gold,
        experience = experience,
        floor = floor,
        godforges = godforges,
        completed = completed,
        floorRewards = floorRewards.map { it.toDomainModel() }
    )
}

private fun DungeonVisit.toEntity(): DungeonVisitEntity {
    Log.d("DungeonRepoMapping", "Converting domain to entity: $name")
    Log.d("DungeonRepoMapping", "  - Domain orns: $orns, gold: $gold, exp: $experience")
    Log.d("DungeonRepoMapping", "  - Domain battle orns: $battleOrns, gold: $battleGold, exp: $battleExperience")
    Log.d("DungeonRepoMapping", "  - Domain floor orns: $floorOrns, gold: $floorGold, exp: $floorExperience")
    Log.d("DungeonRepoMapping", "  - Domain floor rewards: $floorRewards")
    return DungeonVisitEntity(
        id = id,
        sessionId = sessionId,
        name = name,
        mode = mode.toEntity(),
        startTime = startTime,
        durationSeconds = durationSeconds,
        battleOrns = battleOrns,
        battleGold = battleGold,
        battleExperience = battleExperience,
        floorOrns = floorOrns,
        floorGold = floorGold,
        floorExperience = floorExperience,
        orns = orns,
        gold = gold,
        experience = experience,
        floor = floor,
        godforges = godforges,
        completed = completed,
        floorRewards = floorRewards.map { it.toEntity() }
    )
}

// DungeonMode conversion functions
private fun com.lloir.ornaassistant.data.database.entities.DungeonMode.toDomainModel(): com.lloir.ornaassistant.domain.model.DungeonMode {
    return com.lloir.ornaassistant.domain.model.DungeonMode(
        type = when (this.type) {
            com.lloir.ornaassistant.data.database.entities.DungeonMode.Type.NORMAL ->
                com.lloir.ornaassistant.domain.model.DungeonMode.Type.NORMAL
            com.lloir.ornaassistant.data.database.entities.DungeonMode.Type.BOSS ->
                com.lloir.ornaassistant.domain.model.DungeonMode.Type.BOSS
            com.lloir.ornaassistant.data.database.entities.DungeonMode.Type.ENDLESS ->
                com.lloir.ornaassistant.domain.model.DungeonMode.Type.ENDLESS
        },
        isHard = this.isHard
    )
}

private fun com.lloir.ornaassistant.domain.model.DungeonMode.toEntity(): com.lloir.ornaassistant.data.database.entities.DungeonMode {
    return com.lloir.ornaassistant.data.database.entities.DungeonMode(
        type = when (this.type) {
            com.lloir.ornaassistant.domain.model.DungeonMode.Type.NORMAL ->
                com.lloir.ornaassistant.data.database.entities.DungeonMode.Type.NORMAL
            com.lloir.ornaassistant.domain.model.DungeonMode.Type.BOSS ->
                com.lloir.ornaassistant.data.database.entities.DungeonMode.Type.BOSS
            com.lloir.ornaassistant.domain.model.DungeonMode.Type.ENDLESS ->
                com.lloir.ornaassistant.data.database.entities.DungeonMode.Type.ENDLESS
        },
        isHard = this.isHard
    )
}

// FloorReward conversion functions
private fun com.lloir.ornaassistant.data.database.entities.FloorReward.toDomainModel(): com.lloir.ornaassistant.domain.model.FloorReward {
    return com.lloir.ornaassistant.domain.model.FloorReward(
        floor = this.floor,
        orns = this.orns,
        gold = this.gold,
        experience = this.experience
    )
}

private fun com.lloir.ornaassistant.domain.model.FloorReward.toEntity(): com.lloir.ornaassistant.data.database.entities.FloorReward {
    return com.lloir.ornaassistant.data.database.entities.FloorReward(
        floor = this.floor,
        orns = this.orns,
        gold = this.gold,
        experience = this.experience
    )
}
