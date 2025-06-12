package com.lloir.ornaassistant.data.repository

import android.util.Log
import com.lloir.ornaassistant.data.database.dao.ItemAssessmentDao
import com.lloir.ornaassistant.data.database.entities.ItemAssessmentEntity
import com.lloir.ornaassistant.data.network.api.OrnaGuideApi
import com.lloir.ornaassistant.data.network.toAssessmentRequest
import com.lloir.ornaassistant.data.network.toAssessmentResult
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.model.ItemAssessment
import com.lloir.ornaassistant.domain.repository.ItemAssessmentRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemAssessmentRepositoryImpl @Inject constructor(
    private val itemAssessmentDao: ItemAssessmentDao,
    private val ornaGuideApi: OrnaGuideApi,
    private val gson: Gson
) : ItemAssessmentRepository {

    companion object {
        private const val TAG = "ItemAssessmentRepo"
    }

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

    override suspend fun deleteOldAssessments(daysOld: Int) {
        val cutoffDate = LocalDateTime.now().minusDays(daysOld.toLong())
        itemAssessmentDao.deleteOldAssessments(cutoffDate)
    }

    override suspend fun deleteAllAssessments() {
        itemAssessmentDao.deleteAllAssessments()
    }

    override suspend fun assessItem(itemName: String, level: Int, attributes: Map<String, Int>): AssessmentResult {
        return try {
            Log.d(TAG, "Assessing item: $itemName (level $level) with attributes: $attributes")
            val request = attributes.toAssessmentRequest(itemName, level)
            val response = ornaGuideApi.assessItem(request)
            Log.d(TAG, "Assessment response: $response")
            response.toAssessmentResult()
        } catch (e: Exception) {
            Log.e(TAG, "Error assessing item", e)
            AssessmentResult(quality = 0.0, stats = emptyMap(), materials = listOf(0, 0, 0, 0))
        }
    }

    override suspend fun getOrnateCount(since: LocalDateTime): Int {
        return itemAssessmentDao.getOrnateCount(since)
    }

    override suspend fun getGodforgeCount(since: LocalDateTime): Int {
        return itemAssessmentDao.getGodforgeCount(since)
    }

    override suspend fun getLastOrnate(): ItemAssessment? {
        return itemAssessmentDao.getLastOrnate()?.toDomainModel()
    }

    override suspend fun getLastGodforge(): ItemAssessment? {
        return itemAssessmentDao.getLastGodforge()?.toDomainModel()
    }

    override suspend fun getAllAssessmentsForExport(): List<ItemAssessment> {
        return itemAssessmentDao.getAllAssessments().first().map { it.toDomainModel() }
    }

    private fun ItemAssessment.toEntity(): ItemAssessmentEntity {
        return ItemAssessmentEntity(
            id = id,
            itemName = itemName,
            level = level,
            attributes = attributes.mapValues { it.value.toString() },
            assessmentResult = gson.toJson(assessmentResult),
            timestamp = timestamp,
            quality = quality
        )
    }

    private fun ItemAssessmentEntity.toDomainModel(): ItemAssessment {
        val assessmentResult = gson.fromJson(assessmentResult, AssessmentResult::class.java)
        return ItemAssessment(
            id = id,
            itemName = itemName,
            level = level,
            attributes = attributes.mapValues { it.value.toIntOrNull() ?: 0 },
            assessmentResult = assessmentResult,
            timestamp = timestamp,
            quality = quality
        )
    }
}