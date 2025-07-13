package com.lloir.ornaassistant.data.repository

import com.lloir.ornaassistant.data.preferences.SettingsDataStore
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.domain.model.ThemeMode
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

    override fun getSettingsFlow(): Flow<AppSettings> {
        return settingsDataStore.settingsFlow
    }
}
