package com.lloir.ornaassistant.domain.repository

import com.lloir.ornaassistant.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface DungeonRepository {

    fun getAllVisits(): Flow<List<DungeonVisit>>

    fun getVisitsForSession(sessionId: Long): Flow<List<DungeonVisit>>

    suspend fun getVisitsBetween(startTime: LocalDateTime, endTime: LocalDateTime): List<DungeonVisit>

    suspend fun getRecentVisits(days: Int = 7): List<DungeonVisit>

    suspend fun getVisitById(id: Long): DungeonVisit?

    suspend fun insertVisit(visit: DungeonVisit): Long

    suspend fun updateVisit(visit: DungeonVisit)

    suspend fun deleteVisit(visit: DungeonVisit)

    suspend fun deleteAllVisits()

    suspend fun getStatistics(startDate: LocalDateTime): DungeonStatistics
    
    suspend fun getAllVisitsForExport(): List<DungeonVisit>
}

interface WayvesselRepository {

    fun getAllSessions(): Flow<List<WayvesselSession>>

    suspend fun getLastSessionsFor(name: String, limit: Int = 10): List<WayvesselSession>

    suspend fun getLastSessions(limit: Int = 10): List<WayvesselSession>

    suspend fun getSessionsBetween(startTime: LocalDateTime, endTime: LocalDateTime): List<WayvesselSession>

    suspend fun getSessionById(id: Long): WayvesselSession?

    suspend fun insertSession(session: WayvesselSession): Long

    suspend fun updateSession(session: WayvesselSession)

    suspend fun deleteSession(session: WayvesselSession)

    suspend fun deleteAllSessions()

    suspend fun getCurrentSession(): WayvesselSession?
}

interface KingdomRepository {

    fun getAllMembers(): Flow<List<KingdomMember>>

    suspend fun getMemberByName(characterName: String): KingdomMember?

    suspend fun getActiveMembers(): List<KingdomMember>

    suspend fun insertMember(member: KingdomMember)

    suspend fun updateMember(member: KingdomMember)

    suspend fun deleteMember(member: KingdomMember)

    suspend fun deleteAllMembers()
}

interface ItemAssessmentRepository {

    fun getAllAssessments(): Flow<List<ItemAssessment>>

    suspend fun getAssessmentsForItem(itemName: String, limit: Int = 10): List<ItemAssessment>

    suspend fun getAssessmentById(id: Long): ItemAssessment?

    suspend fun insertAssessment(assessment: ItemAssessment): Long

    suspend fun deleteAssessment(assessment: ItemAssessment)

    suspend fun deleteOldAssessments(daysOld: Int = 30)

    suspend fun deleteAllAssessments()

    suspend fun assessItem(itemName: String, level: Int, attributes: Map<String, Int>): AssessmentResult
    
    suspend fun getOrnateCount(since: LocalDateTime): Int
    
    suspend fun getGodforgeCount(since: LocalDateTime): Int
    
    suspend fun getLastOrnate(): ItemAssessment?
    
    suspend fun getLastGodforge(): ItemAssessment?
    
    suspend fun getAllAssessmentsForExport(): List<ItemAssessment>
}

interface SettingsRepository {

    suspend fun getSettings(): AppSettings

    suspend fun updateSettings(settings: AppSettings)

    suspend fun updateSessionOverlay(enabled: Boolean)

    suspend fun updateInvitesOverlay(enabled: Boolean)

    suspend fun updateAssessOverlay(enabled: Boolean)

    suspend fun updateWayvesselNotifications(enabled: Boolean)

    suspend fun updateOverlayTransparency(transparency: Float)

    fun getSettingsFlow(): Flow<AppSettings>
}

interface NotificationRepository {

    suspend fun scheduleWayvesselNotification(wayvesselName: String, delayMinutes: Long)

    suspend fun cancelWayvesselNotification(wayvesselName: String)

    suspend fun showServiceNotification()

    suspend fun hideServiceNotification()

    suspend fun showOverlayNotification(message: String)
}