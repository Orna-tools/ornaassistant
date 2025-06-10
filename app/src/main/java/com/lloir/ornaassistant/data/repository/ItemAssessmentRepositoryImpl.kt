package com.lloir.ornaassistant.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.lloir.ornaassistant.data.database.dao.ItemAssessmentDao
import com.lloir.ornaassistant.data.database.entities.ItemAssessmentEntity
import com.lloir.ornaassistant.data.network.api.OrnaGuideApi
import com.lloir.ornaassistant.data.network.toAssessmentRequest
import com.lloir.ornaassistant.data.network.toAssessmentResult
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.model.ItemAssessment
import com.lloir.ornaassistant.domain.repository.ItemAssessmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemAssessmentRepositoryImpl @Inject constructor(
    private val itemAssessmentDao: ItemAssessmentDao,
    private val ornaGuideApi: OrnaGuideApi
) : ItemAssessmentRepository {

    override fun getAllAssessments(): Flow<List<ItemAssessment>> {
        return itemAssessmentDao.getAllAssessments().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getAssessmentsForItem(itemName: String, limit: Int): List<ItemAssessment> {
        return itemAssessmentDao.getAssessmentsForItem(itemName, limit).map { it.toDomainModel() }
    }

    override suspend fun getAssessmentById(id: Long): ItemAssessment? {
        return itemAssessmentDao.getAssessmentById(id)?.toDomainModel()
    }

    override suspend fun insertAssessment(assessment: ItemAssessment): Long {
        return itemAssessmentDao.insertAssessment(assessment.toEntity())
    }

    override suspend fun deleteAssessment(assessment: ItemAssessment) {
        itemAssessmentDao.deleteAssessment(assessment.toEntity())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun deleteOldAssessments(daysOld: Int) {
        val cutoffDate = LocalDateTime.now().minusDays(daysOld.toLong())
        itemAssessmentDao.deleteOldAssessments(cutoffDate)
    }

    override suspend fun deleteAllAssessments() {
        itemAssessmentDao.deleteAllAssessments()
    }

    override suspend fun assessItem(itemName: String, level: Int, attributes: Map<String, Int>): AssessmentResult {
        val bannedNames = setOf(
            "Vagrant Beasts", "Daily Login", "Notifications", "Codex", "News", "Party",
            "Arena", "Character", "Options", "Runeshop", "Inventory", "Knights of Inferno",
            "Earthen Legion", "FrozenGuard", "Gauntlet"
        )

        if (itemName.isBlank() || itemName.length < 3 || bannedNames.any { itemName.contains(it, ignoreCase = true) }) {
            return AssessmentResult(
                quality = 0.0,
                stats = emptyMap(),
                materials = emptyList() // ✅ required field
            )
        }

        val request = attributes.toAssessmentRequest(itemName, level)
        val response = ornaGuideApi.assessItem(request)
        return response.toAssessmentResult()
    }
}

// Extension functions

private fun ItemAssessmentEntity.toDomainModel(): ItemAssessment {
    return ItemAssessment(
        id = id,
        itemName = itemName,
        level = level,
        attributes = attributes.mapValues { it.value.toInt() },
        assessmentResult = com.google.gson.Gson().fromJson(assessmentResult, AssessmentResult::class.java),
        timestamp = timestamp,
        quality = quality
    )
}

private fun ItemAssessment.toEntity(): ItemAssessmentEntity {
    return ItemAssessmentEntity(
        id = id,
        itemName = itemName,
        level = level,
        attributes = attributes.mapValues { it.value.toString() },
        assessmentResult = com.google.gson.Gson().toJson(assessmentResult),
        timestamp = timestamp,
        quality = quality
    )
}
