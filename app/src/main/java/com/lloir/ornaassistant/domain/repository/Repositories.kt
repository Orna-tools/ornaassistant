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

    suspend fun assessItem(
        itemName: String,
        level: Int,
        attributes: Map<String, Int>,
        originalItemName: String = itemName,
        adornmentValues: Map<String, Int> = emptyMap(),
        anguishLevel: Int = 0,
        isCelestialWeapon: Boolean = false,
        isTwoHanded: Boolean = false,
        isOffHand: Boolean = false
    ): AssessmentResult
}

interface SettingsRepository {

    suspend fun getSettings(): AppSettings

    suspend fun updateSettings(settings: AppSettings)

    suspend fun updateAssessOverlay(enabled: Boolean)

    suspend fun updateOverlayTransparency(transparency: Float)

    suspend fun updateDebugMode(enabled: Boolean)

    suspend fun updateEnableMaterialTracking(enabled: Boolean)

    suspend fun updateThemeMode(themeMode: ThemeMode)

    suspend fun updateUseDynamicColors(enabled: Boolean)

    suspend fun updateUseAdaptiveLayouts(enabled: Boolean)

    suspend fun updateUseHighContrastMode(enabled: Boolean)

    // Assessment overlay customization methods
    suspend fun updateAssessOverlayFontSizes(
        titleSize: Float,
        qualitySize: Float,
        statsSize: Float,
        materialsSize: Float
    )

    suspend fun updateAssessOverlayColors(
        titleColor: Int,
        qualityColor: Int,
        statsColor: Int,
        materialsColor: Int
    )

    suspend fun updateAssessOverlayContent(
        showMaterials: Boolean,
        showStats: Boolean
    )

    // Individual font size update methods
    suspend fun updateAssessOverlayTitleSize(size: Float)

    suspend fun updateAssessOverlayQualitySize(size: Float)

    suspend fun updateAssessOverlayStatsSize(size: Float)

    suspend fun updateAssessOverlayMaterialsSize(size: Float)

    // Individual color update methods
    suspend fun updateAssessOverlayTitleColor(color: Int)

    suspend fun updateAssessOverlayQualityColor(color: Int)

    suspend fun updateAssessOverlayStatsColor(color: Int)

    suspend fun updateAssessOverlayMaterialsColor(color: Int)

    // Dungeon overlay settings methods
    suspend fun updateShowDungeonOverlay(enabled: Boolean)

    suspend fun updateShowFloorProgress(enabled: Boolean)

    suspend fun updateColorCodeDungeons(enabled: Boolean)

    suspend fun updateShowRewardsEstimate(enabled: Boolean)

    suspend fun updateShowDungeonSpecialInfo(enabled: Boolean)

    suspend fun updateFlashOnFloorChange(enabled: Boolean)

    // Combined method to update all dungeon overlay settings at once
    suspend fun updateDungeonOverlaySettings(
        showDungeonOverlay: Boolean,
        showFloorProgress: Boolean,
        colorCodeDungeons: Boolean,
        showRewardsEstimate: Boolean,
        showDungeonSpecialInfo: Boolean,
        flashOnFloorChange: Boolean
    )

    fun getSettingsFlow(): Flow<AppSettings>
}

interface NotificationRepository {

    suspend fun showServiceNotification()

    suspend fun hideServiceNotification()

    suspend fun showOverlayNotification(message: String)

    suspend fun showMaterialTargetReachedNotification(
        materialName: String,
        currentQuantity: Int,
        targetQuantity: Int
    )
}

interface MaterialRepository {
    fun getAllMaterials(): Flow<List<Material>>

    fun getTrackedMaterials(): Flow<List<Material>>

    suspend fun getMaterialByName(name: String): Material?

    suspend fun getMaterialById(id: Long): Material?

    suspend fun insertMaterial(material: Material): Long

    suspend fun updateMaterial(material: Material)

    suspend fun deleteMaterial(material: Material)

    suspend fun deleteAllMaterials()

    suspend fun updateMaterialTracking(materialId: Long, isTracked: Boolean)

    suspend fun updateMaterialTarget(materialId: Long, targetQuantity: Int)

    suspend fun updateMaterialQuantity(materialId: Long, currentQuantity: Int)

    suspend fun getTrackedMaterialsCount(): Int

    suspend fun searchMaterials(searchTerm: String): List<Material>
}
