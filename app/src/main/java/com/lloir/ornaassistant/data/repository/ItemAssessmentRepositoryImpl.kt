package com.lloir.ornaassistant.data.repository

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

    override suspend fun deleteOldAssessments(daysOld: Int) {
        val cutoffDate = LocalDateTime.now().minusDays(daysOld.toLong())
        itemAssessmentDao.deleteOldAssessments(cutoffDate)
    }

    override suspend fun deleteAllAssessments() {
        itemAssessmentDao.deleteAllAssessments()
    }

    override suspend fun assessItem(itemName: String, level: Int, attributes: Map<String, Int>): AssessmentResult {
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

// FILE: data/repository/SettingsRepositoryImpl.kt
package com.lloir.ornaassistant.data.repository

import com.lloir.ornaassistant.data.preferences.SettingsDataStore
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override suspend fun getSettings(): AppSettings {
        return settingsDataStore.getSettings()
    }

    override suspend fun updateSettings(settings: AppSettings) {
        settingsDataStore.updateSettings(settings)
    }

    override suspend fun updateSessionOverlay(enabled: Boolean) {
        settingsDataStore.updateSessionOverlay(enabled)
    }

    override suspend fun updateInvitesOverlay(enabled: Boolean) {
        settingsDataStore.updateInvitesOverlay(enabled)
    }

    override suspend fun updateAssessOverlay(enabled: Boolean) {
        settingsDataStore.updateAssessOverlay(enabled)
    }

    override suspend fun updateWayvesselNotifications(enabled: Boolean) {
        settingsDataStore.updateWayvesselNotifications(enabled)
    }

    override suspend fun updateOverlayTransparency(transparency: Float) {
        settingsDataStore.updateOverlayTransparency(transparency)
    }

    override fun getSettingsFlow(): Flow<AppSettings> {
        return settingsDataStore.settingsFlow
    }
}

// FILE: data/repository/KingdomRepositoryImpl.kt
package com.lloir.ornaassistant.data.repository

import com.lloir.ornaassistant.data.database.dao.KingdomMemberDao
import com.lloir.ornaassistant.data.database.entities.KingdomMemberEntity
import com.lloir.ornaassistant.domain.model.KingdomMember
import com.lloir.ornaassistant.domain.repository.KingdomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KingdomRepositoryImpl @Inject constructor(
    private val kingdomMemberDao: KingdomMemberDao
) : KingdomRepository {

    override fun getAllMembers(): Flow<List<KingdomMember>> {
        return kingdomMemberDao.getAllMembers().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getMemberByName(characterName: String): KingdomMember? {
        return kingdomMemberDao.getMemberByName(characterName)?.toDomainModel()
    }

    override suspend fun getActiveMembers(): List<KingdomMember> {
        return kingdomMemberDao.getActiveMembers(LocalDateTime.now()).map { it.toDomainModel() }
    }

    override suspend fun insertMember(member: KingdomMember) {
        kingdomMemberDao.insertMember(member.toEntity())
    }

    override suspend fun updateMember(member: KingdomMember) {
        kingdomMemberDao.updateMember(member.toEntity())
    }

    override suspend fun deleteMember(member: KingdomMember) {
        kingdomMemberDao.deleteMember(member.toEntity())
    }

    override suspend fun deleteAllMembers() {
        kingdomMemberDao.deleteAllMembers()
    }
}

private fun KingdomMemberEntity.toDomainModel(): KingdomMember {
    return KingdomMember(
        characterName = characterName,
        discordName = discordName,
        immunity = immunity,
        endTime = endTime,
        endTimeLeftSeconds = endTimeLeftSeconds,
        seenCount = seenCount,
        timezone = timezone,
        floors = emptyMap() // Convert floors map if needed
    )
}

private fun KingdomMember.toEntity(): KingdomMemberEntity {
    return KingdomMemberEntity(
        characterName = characterName,
        discordName = discordName,
        immunity = immunity,
        endTime = endTime,
        endTimeLeftSeconds = endTimeLeftSeconds,
        seenCount = seenCount,
        timezone = timezone,
        floors = emptyMap() // Convert floors map if needed
    )
}