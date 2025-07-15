package com.lloir.ornaassistant.data.repository

import com.lloir.ornaassistant.data.preferences.SettingsDataStore
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.domain.model.BackupFrequency
import com.lloir.ornaassistant.domain.model.ThemeMode
import com.lloir.ornaassistant.domain.model.ThemeType
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
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

    override suspend fun updateAssessOverlay(enabled: Boolean) {
        settingsDataStore.updateAssessOverlay(enabled)
    }

    override suspend fun updateOverlayTransparency(transparency: Float) {
        settingsDataStore.updateOverlayTransparency(transparency)
    }

    override suspend fun updateDebugMode(enabled: Boolean) {
        settingsDataStore.updateDebugMode(enabled)
    }

    override suspend fun updateEnableMaterialTracking(enabled: Boolean) {
        settingsDataStore.updateEnableMaterialTracking(enabled)
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        settingsDataStore.updateThemeMode(themeMode)
    }

    override suspend fun updateUseDynamicColors(enabled: Boolean) {
        settingsDataStore.updateUseDynamicColors(enabled)
    }

    override suspend fun updateUseAdaptiveLayouts(enabled: Boolean) {
        settingsDataStore.updateUseAdaptiveLayouts(enabled)
    }

    override suspend fun updateUseHighContrastMode(enabled: Boolean) {
        settingsDataStore.updateUseHighContrastMode(enabled)
    }

    override suspend fun updateUseLargerFontSize(enabled: Boolean) {
        settingsDataStore.updateUseLargerFontSize(enabled)
    }

    override suspend fun updateUseTextToSpeech(enabled: Boolean) {
        settingsDataStore.updateUseTextToSpeech(enabled)
    }

    override suspend fun updateUseReducedMotion(enabled: Boolean) {
        settingsDataStore.updateUseReducedMotion(enabled)
    }

    // Tutorial settings methods
    override suspend fun updateHasCompletedTutorial(completed: Boolean) {
        settingsDataStore.updateHasCompletedTutorial(completed)
    }

    override suspend fun updateShowFeatureTutorials(show: Boolean) {
        settingsDataStore.updateShowFeatureTutorials(show)
    }

    // Assessment overlay customization methods
    override suspend fun updateAssessOverlayFontSizes(
        titleSize: Float,
        qualitySize: Float,
        statsSize: Float,
        materialsSize: Float
    ) {
        settingsDataStore.updateAssessOverlayFontSizes(
            titleSize,
            qualitySize,
            statsSize,
            materialsSize
        )
    }

    override suspend fun updateAssessOverlayColors(
        titleColor: Int,
        qualityColor: Int,
        statsColor: Int,
        materialsColor: Int
    ) {
        settingsDataStore.updateAssessOverlayColors(
            titleColor,
            qualityColor,
            statsColor,
            materialsColor
        )
    }

    override suspend fun updateAssessOverlayContent(
        showMaterials: Boolean,
        showStats: Boolean
    ) {
        settingsDataStore.updateAssessOverlayContent(
            showMaterials,
            showStats
        )
    }

    // Individual font size update methods
    override suspend fun updateAssessOverlayTitleSize(size: Float) {
        settingsDataStore.updateAssessOverlayTitleSize(size)
    }

    override suspend fun updateAssessOverlayQualitySize(size: Float) {
        settingsDataStore.updateAssessOverlayQualitySize(size)
    }

    override suspend fun updateAssessOverlayStatsSize(size: Float) {
        settingsDataStore.updateAssessOverlayStatsSize(size)
    }

    override suspend fun updateAssessOverlayMaterialsSize(size: Float) {
        settingsDataStore.updateAssessOverlayMaterialsSize(size)
    }

    // Individual color update methods
    override suspend fun updateAssessOverlayTitleColor(color: Int) {
        settingsDataStore.updateAssessOverlayTitleColor(color)
    }

    override suspend fun updateAssessOverlayQualityColor(color: Int) {
        settingsDataStore.updateAssessOverlayQualityColor(color)
    }

    override suspend fun updateAssessOverlayStatsColor(color: Int) {
        settingsDataStore.updateAssessOverlayStatsColor(color)
    }

    override suspend fun updateAssessOverlayMaterialsColor(color: Int) {
        settingsDataStore.updateAssessOverlayMaterialsColor(color)
    }

    // Dungeon overlay settings methods
    override suspend fun updateShowDungeonOverlay(enabled: Boolean) {
        settingsDataStore.updateShowDungeonOverlay(enabled)
    }

    override suspend fun updateShowFloorProgress(enabled: Boolean) {
        settingsDataStore.updateShowFloorProgress(enabled)
    }

    override suspend fun updateColorCodeDungeons(enabled: Boolean) {
        settingsDataStore.updateColorCodeDungeons(enabled)
    }

    override suspend fun updateShowRewardsEstimate(enabled: Boolean) {
        settingsDataStore.updateShowRewardsEstimate(enabled)
    }

    override suspend fun updateShowDungeonSpecialInfo(enabled: Boolean) {
        settingsDataStore.updateShowDungeonSpecialInfo(enabled)
    }

    override suspend fun updateFlashOnFloorChange(enabled: Boolean) {
        settingsDataStore.updateFlashOnFloorChange(enabled)
    }

    // Combined method to update all dungeon overlay settings at once
    override suspend fun updateDungeonOverlaySettings(
        showDungeonOverlay: Boolean,
        showFloorProgress: Boolean,
        colorCodeDungeons: Boolean,
        showRewardsEstimate: Boolean,
        showDungeonSpecialInfo: Boolean,
        flashOnFloorChange: Boolean
    ) {
        settingsDataStore.updateDungeonOverlaySettings(
            showDungeonOverlay,
            showFloorProgress,
            colorCodeDungeons,
            showRewardsEstimate,
            showDungeonSpecialInfo,
            flashOnFloorChange
        )
    }

    // Dungeon overlay font size methods
    override suspend fun updateDungeonOverlayFontSizes(
        titleSize: Float,
        modeSize: Float,
        floorSize: Float,
        rewardsSize: Float,
        cooldownSize: Float,
        specialInfoSize: Float
    ) {
        settingsDataStore.updateDungeonOverlayFontSizes(
            titleSize,
            modeSize,
            floorSize,
            rewardsSize,
            cooldownSize,
            specialInfoSize
        )
    }

    // Dungeon overlay color methods
    override suspend fun updateDungeonOverlayColors(
        titleColor: Int,
        modeColor: Int,
        floorColor: Int,
        rewardsColor: Int,
        cooldownColor: Int,
        specialInfoColor: Int
    ) {
        settingsDataStore.updateDungeonOverlayColors(
            titleColor,
            modeColor,
            floorColor,
            rewardsColor,
            cooldownColor,
            specialInfoColor
        )
    }

    // Individual font size update methods for dungeon overlay
    override suspend fun updateDungeonOverlayTitleSize(size: Float) {
        settingsDataStore.updateDungeonOverlayTitleSize(size)
    }

    override suspend fun updateDungeonOverlayModeSize(size: Float) {
        settingsDataStore.updateDungeonOverlayModeSize(size)
    }

    override suspend fun updateDungeonOverlayFloorSize(size: Float) {
        settingsDataStore.updateDungeonOverlayFloorSize(size)
    }

    override suspend fun updateDungeonOverlayRewardsSize(size: Float) {
        settingsDataStore.updateDungeonOverlayRewardsSize(size)
    }

    override suspend fun updateDungeonOverlayCooldownSize(size: Float) {
        settingsDataStore.updateDungeonOverlayCooldownSize(size)
    }

    override suspend fun updateDungeonOverlaySpecialInfoSize(size: Float) {
        settingsDataStore.updateDungeonOverlaySpecialInfoSize(size)
    }

    // Individual color update methods for dungeon overlay
    override suspend fun updateDungeonOverlayTitleColor(color: Int) {
        settingsDataStore.updateDungeonOverlayTitleColor(color)
    }

    override suspend fun updateDungeonOverlayModeColor(color: Int) {
        settingsDataStore.updateDungeonOverlayModeColor(color)
    }

    override suspend fun updateDungeonOverlayFloorColor(color: Int) {
        settingsDataStore.updateDungeonOverlayFloorColor(color)
    }

    override suspend fun updateDungeonOverlayRewardsColor(color: Int) {
        settingsDataStore.updateDungeonOverlayRewardsColor(color)
    }

    override suspend fun updateDungeonOverlayCooldownColor(color: Int) {
        settingsDataStore.updateDungeonOverlayCooldownColor(color)
    }

    override suspend fun updateDungeonOverlaySpecialInfoColor(color: Int) {
        settingsDataStore.updateDungeonOverlaySpecialInfoColor(color)
    }

    // Premium theme methods
    override suspend fun updateSelectedTheme(themeType: ThemeType) {
        settingsDataStore.updateSelectedTheme(themeType)
    }

    override suspend fun updatePremiumStatus(isPremium: Boolean, expiryDate: LocalDateTime?) {
        settingsDataStore.updatePremiumStatus(isPremium, expiryDate)
    }

    // Backup & Restore methods
    override suspend fun updateAutoBackupEnabled(enabled: Boolean) {
        settingsDataStore.updateAutoBackupEnabled(enabled)
    }

    override suspend fun updateAutoBackupFrequency(frequency: BackupFrequency) {
        settingsDataStore.updateAutoBackupFrequency(frequency)
    }

    override suspend fun updateAutoBackupRetention(retention: Int) {
        settingsDataStore.updateAutoBackupRetention(retention)
    }

    override suspend fun updateLastBackupDate(date: LocalDateTime?) {
        settingsDataStore.updateLastBackupDate(date)
    }

    // Data retention methods
    override suspend fun updateDungeonDataRetentionDays(days: Int) {
        settingsDataStore.updateDungeonDataRetentionDays(days)
    }

    override suspend fun updateAssessmentDataRetentionDays(days: Int) {
        settingsDataStore.updateAssessmentDataRetentionDays(days)
    }

    // Performance methods
    override suspend fun updateBatterySaverMode(enabled: Boolean) {
        settingsDataStore.updateBatterySaverMode(enabled)
    }

    override suspend fun updateOfflineMode(enabled: Boolean) {
        settingsDataStore.updateOfflineMode(enabled)
    }

    override suspend fun updateLowMemoryMode(enabled: Boolean) {
        settingsDataStore.updateLowMemoryMode(enabled)
    }

    // Dark Mode Refinements methods
    override suspend fun updateUseAmoledDarkMode(enabled: Boolean) {
        settingsDataStore.updateUseAmoledDarkMode(enabled)
    }

    override suspend fun updateEnhancedDarkModeContrast(enabled: Boolean) {
        settingsDataStore.updateEnhancedDarkModeContrast(enabled)
    }

    override fun getSettingsFlow(): Flow<AppSettings> {
        return settingsDataStore.settingsFlow
    }
}
