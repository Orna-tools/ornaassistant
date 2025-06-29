package com.lloir.ornaassistant.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.lloir.ornaassistant.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val SHOW_ASSESS_OVERLAY = booleanPreferencesKey("show_assess_overlay")
        val NOTIFICATION_SOUNDS = booleanPreferencesKey("notification_sounds")
        val OVERLAY_TRANSPARENCY = floatPreferencesKey("overlay_transparency")
        val AUTO_HIDE_OVERLAYS = booleanPreferencesKey("auto_hide_overlays")
        val DEBUG_MODE = booleanPreferencesKey("debug_mode")
        val USE_ML_KIT = booleanPreferencesKey("use_ml_kit")
        val ENABLE_QUEST_FEATURE = booleanPreferencesKey("enable_quest_feature")
        val ENABLE_DUNGEON_TRACKER = booleanPreferencesKey("enable_dungeon_tracker")
        val ENABLE_MATERIAL_TRACKING = booleanPreferencesKey("enable_material_tracking")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            showAssessOverlay = preferences[PreferencesKeys.SHOW_ASSESS_OVERLAY] ?: true,
            notificationSounds = preferences[PreferencesKeys.NOTIFICATION_SOUNDS] ?: true,
            overlayTransparency = preferences[PreferencesKeys.OVERLAY_TRANSPARENCY] ?: 0.8f,
            autoHideOverlays = preferences[PreferencesKeys.AUTO_HIDE_OVERLAYS] ?: false,
            debugMode = preferences[PreferencesKeys.DEBUG_MODE] ?: false,
            useMlKit = preferences[PreferencesKeys.USE_ML_KIT] ?: false,
            enableQuestFeature = preferences[PreferencesKeys.ENABLE_QUEST_FEATURE] ?: false,
            enableDungeonTracker = preferences[PreferencesKeys.ENABLE_DUNGEON_TRACKER] ?: true,
            enableMaterialTracking = preferences[PreferencesKeys.ENABLE_MATERIAL_TRACKING] ?: false,
            themeMode = preferences[PreferencesKeys.THEME_MODE]?.let { 
                try {
                    com.lloir.ornaassistant.domain.model.ThemeMode.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    com.lloir.ornaassistant.domain.model.ThemeMode.SYSTEM
                }
            } ?: com.lloir.ornaassistant.domain.model.ThemeMode.SYSTEM,
            useDynamicColors = preferences[PreferencesKeys.USE_DYNAMIC_COLORS] ?: true
        )
    }

    suspend fun getSettings(): AppSettings {
        return settingsFlow.first()
    }

    suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_ASSESS_OVERLAY] = settings.showAssessOverlay
            preferences[PreferencesKeys.NOTIFICATION_SOUNDS] = settings.notificationSounds
            preferences[PreferencesKeys.OVERLAY_TRANSPARENCY] = settings.overlayTransparency
            preferences[PreferencesKeys.AUTO_HIDE_OVERLAYS] = settings.autoHideOverlays
            preferences[PreferencesKeys.DEBUG_MODE] = settings.debugMode
        }
    }

    suspend fun updateAssessOverlay(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_ASSESS_OVERLAY] = enabled
        }
    }

    suspend fun updateOverlayTransparency(transparency: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERLAY_TRANSPARENCY] = transparency
        }
    }

    suspend fun updateDebugMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEBUG_MODE] = enabled
        }
    }

    suspend fun updateUseMlKit(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_ML_KIT] = enabled
        }
    }

    suspend fun updateEnableQuestFeature(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_QUEST_FEATURE] = enabled
        }
    }

    suspend fun updateEnableDungeonTracker(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_DUNGEON_TRACKER] = enabled
        }
    }

    suspend fun updateEnableMaterialTracking(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_MATERIAL_TRACKING] = enabled
        }
    }

    suspend fun updateThemeMode(themeMode: com.lloir.ornaassistant.domain.model.ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateUseDynamicColors(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLORS] = enabled
        }
    }
}
