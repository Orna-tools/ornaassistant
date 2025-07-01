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

    override fun getSettingsFlow(): Flow<AppSettings> {
        return settingsDataStore.settingsFlow
    }
}
