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
        val ENABLE_MATERIAL_TRACKING = booleanPreferencesKey("enable_material_tracking")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        val USE_ADAPTIVE_LAYOUTS = booleanPreferencesKey("use_adaptive_layouts")

        // Accessibility preference keys
        val USE_HIGH_CONTRAST_MODE = booleanPreferencesKey("use_high_contrast_mode")

        // Assessment overlay customization keys
        val ASSESS_OVERLAY_TITLE_SIZE = floatPreferencesKey("assess_overlay_title_size")
        val ASSESS_OVERLAY_QUALITY_SIZE = floatPreferencesKey("assess_overlay_quality_size")
        val ASSESS_OVERLAY_STATS_SIZE = floatPreferencesKey("assess_overlay_stats_size")
        val ASSESS_OVERLAY_MATERIALS_SIZE = floatPreferencesKey("assess_overlay_materials_size")
        val ASSESS_OVERLAY_TITLE_COLOR = intPreferencesKey("assess_overlay_title_color")
        val ASSESS_OVERLAY_QUALITY_COLOR = intPreferencesKey("assess_overlay_quality_color")
        val ASSESS_OVERLAY_STATS_COLOR = intPreferencesKey("assess_overlay_stats_color")
        val ASSESS_OVERLAY_MATERIALS_COLOR = intPreferencesKey("assess_overlay_materials_color")
        val ASSESS_OVERLAY_SHOW_MATERIALS = booleanPreferencesKey("assess_overlay_show_materials")
        val ASSESS_OVERLAY_SHOW_STATS = booleanPreferencesKey("assess_overlay_show_stats")

        // Dungeon overlay settings keys
        val SHOW_DUNGEON_OVERLAY = booleanPreferencesKey("show_dungeon_overlay")
        val SHOW_FLOOR_PROGRESS = booleanPreferencesKey("show_floor_progress")
        val COLOR_CODE_DUNGEONS = booleanPreferencesKey("color_code_dungeons")
        val SHOW_REWARDS_ESTIMATE = booleanPreferencesKey("show_rewards_estimate")
        val SHOW_DUNGEON_SPECIAL_INFO = booleanPreferencesKey("show_dungeon_special_info")
        val FLASH_ON_FLOOR_CHANGE = booleanPreferencesKey("flash_on_floor_change")
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            showAssessOverlay = preferences[PreferencesKeys.SHOW_ASSESS_OVERLAY] ?: true,
            notificationSounds = preferences[PreferencesKeys.NOTIFICATION_SOUNDS] ?: true,
            overlayTransparency = preferences[PreferencesKeys.OVERLAY_TRANSPARENCY] ?: 0.8f,
            autoHideOverlays = preferences[PreferencesKeys.AUTO_HIDE_OVERLAYS] ?: false,
            debugMode = preferences[PreferencesKeys.DEBUG_MODE] ?: false,
            enableMaterialTracking = preferences[PreferencesKeys.ENABLE_MATERIAL_TRACKING] ?: false,
            themeMode = preferences[PreferencesKeys.THEME_MODE]?.let { 
                try {
                    com.lloir.ornaassistant.domain.model.ThemeMode.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    com.lloir.ornaassistant.domain.model.ThemeMode.SYSTEM
                }
            } ?: com.lloir.ornaassistant.domain.model.ThemeMode.SYSTEM,
            useDynamicColors = preferences[PreferencesKeys.USE_DYNAMIC_COLORS] ?: true,
            useAdaptiveLayouts = preferences[PreferencesKeys.USE_ADAPTIVE_LAYOUTS] ?: (android.os.Build.VERSION.SDK_INT >= 36),
            useHighContrastMode = preferences[PreferencesKeys.USE_HIGH_CONTRAST_MODE] ?: false,

            // Assessment overlay customization
            assessOverlayTitleSize = preferences[PreferencesKeys.ASSESS_OVERLAY_TITLE_SIZE] ?: 14f,
            assessOverlayQualitySize = preferences[PreferencesKeys.ASSESS_OVERLAY_QUALITY_SIZE] ?: 12f,
            assessOverlayStatsSize = preferences[PreferencesKeys.ASSESS_OVERLAY_STATS_SIZE] ?: 11f,
            assessOverlayMaterialsSize = preferences[PreferencesKeys.ASSESS_OVERLAY_MATERIALS_SIZE] ?: 10f,
            assessOverlayTitleColor = preferences[PreferencesKeys.ASSESS_OVERLAY_TITLE_COLOR] ?: android.graphics.Color.WHITE,
            assessOverlayQualityColor = preferences[PreferencesKeys.ASSESS_OVERLAY_QUALITY_COLOR] ?: android.graphics.Color.CYAN,
            assessOverlayStatsColor = preferences[PreferencesKeys.ASSESS_OVERLAY_STATS_COLOR] ?: android.graphics.Color.CYAN,
            assessOverlayMaterialsColor = preferences[PreferencesKeys.ASSESS_OVERLAY_MATERIALS_COLOR] ?: android.graphics.Color.LTGRAY,
            assessOverlayShowMaterials = preferences[PreferencesKeys.ASSESS_OVERLAY_SHOW_MATERIALS] ?: true,
            assessOverlayShowStats = preferences[PreferencesKeys.ASSESS_OVERLAY_SHOW_STATS] ?: true,

            // Dungeon overlay settings
            showDungeonOverlay = preferences[PreferencesKeys.SHOW_DUNGEON_OVERLAY] ?: true,
            showFloorProgress = preferences[PreferencesKeys.SHOW_FLOOR_PROGRESS] ?: true,
            colorCodeDungeons = preferences[PreferencesKeys.COLOR_CODE_DUNGEONS] ?: true,
            showRewardsEstimate = preferences[PreferencesKeys.SHOW_REWARDS_ESTIMATE] ?: false,
            showDungeonSpecialInfo = preferences[PreferencesKeys.SHOW_DUNGEON_SPECIAL_INFO] ?: true,
            flashOnFloorChange = preferences[PreferencesKeys.FLASH_ON_FLOOR_CHANGE] ?: true
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
            preferences[PreferencesKeys.ENABLE_MATERIAL_TRACKING] = settings.enableMaterialTracking
            preferences[PreferencesKeys.THEME_MODE] = settings.themeMode.name
            preferences[PreferencesKeys.USE_DYNAMIC_COLORS] = settings.useDynamicColors
            preferences[PreferencesKeys.USE_ADAPTIVE_LAYOUTS] = settings.useAdaptiveLayouts
            preferences[PreferencesKeys.USE_HIGH_CONTRAST_MODE] = settings.useHighContrastMode

            // Assessment overlay customization
            preferences[PreferencesKeys.ASSESS_OVERLAY_TITLE_SIZE] = settings.assessOverlayTitleSize
            preferences[PreferencesKeys.ASSESS_OVERLAY_QUALITY_SIZE] = settings.assessOverlayQualitySize
            preferences[PreferencesKeys.ASSESS_OVERLAY_STATS_SIZE] = settings.assessOverlayStatsSize
            preferences[PreferencesKeys.ASSESS_OVERLAY_MATERIALS_SIZE] = settings.assessOverlayMaterialsSize
            preferences[PreferencesKeys.ASSESS_OVERLAY_TITLE_COLOR] = settings.assessOverlayTitleColor
            preferences[PreferencesKeys.ASSESS_OVERLAY_QUALITY_COLOR] = settings.assessOverlayQualityColor
            preferences[PreferencesKeys.ASSESS_OVERLAY_STATS_COLOR] = settings.assessOverlayStatsColor
            preferences[PreferencesKeys.ASSESS_OVERLAY_MATERIALS_COLOR] = settings.assessOverlayMaterialsColor
            preferences[PreferencesKeys.ASSESS_OVERLAY_SHOW_MATERIALS] = settings.assessOverlayShowMaterials
            preferences[PreferencesKeys.ASSESS_OVERLAY_SHOW_STATS] = settings.assessOverlayShowStats
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

    suspend fun updateUseAdaptiveLayouts(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_ADAPTIVE_LAYOUTS] = enabled
        }
    }

    suspend fun updateUseHighContrastMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_HIGH_CONTRAST_MODE] = enabled
        }
    }

    // Assessment overlay font size methods
    suspend fun updateAssessOverlayFontSizes(
        titleSize: Float,
        qualitySize: Float,
        statsSize: Float,
        materialsSize: Float
    ) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASSESS_OVERLAY_TITLE_SIZE] = titleSize
            preferences[PreferencesKeys.ASSESS_OVERLAY_QUALITY_SIZE] = qualitySize
            preferences[PreferencesKeys.ASSESS_OVERLAY_STATS_SIZE] = statsSize
            preferences[PreferencesKeys.ASSESS_OVERLAY_MATERIALS_SIZE] = materialsSize
        }
    }

    // Assessment overlay color methods
    suspend fun updateAssessOverlayColors(
        titleColor: Int,
        qualityColor: Int,
        statsColor: Int,
        materialsColor: Int
    ) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASSESS_OVERLAY_TITLE_COLOR] = titleColor
            preferences[PreferencesKeys.ASSESS_OVERLAY_QUALITY_COLOR] = qualityColor
            preferences[PreferencesKeys.ASSESS_OVERLAY_STATS_COLOR] = statsColor
            preferences[PreferencesKeys.ASSESS_OVERLAY_MATERIALS_COLOR] = materialsColor
        }
    }

    // Assessment overlay content visibility methods
    suspend fun updateAssessOverlayContent(
        showMaterials: Boolean,
        showStats: Boolean
    ) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASSESS_OVERLAY_SHOW_MATERIALS] = showMaterials
            preferences[PreferencesKeys.ASSESS_OVERLAY_SHOW_STATS] = showStats
        }
    }

    // Individual font size update methods
    suspend fun updateAssessOverlayTitleSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASSESS_OVERLAY_TITLE_SIZE] = size
        }
    }

    suspend fun updateAssessOverlayQualitySize(size: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASSESS_OVERLAY_QUALITY_SIZE] = size
        }
    }

    suspend fun updateAssessOverlayStatsSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASSESS_OVERLAY_STATS_SIZE] = size
        }
    }

    suspend fun updateAssessOverlayMaterialsSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASSESS_OVERLAY_MATERIALS_SIZE] = size
        }
    }

    // Individual color update methods
    suspend fun updateAssessOverlayTitleColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASSESS_OVERLAY_TITLE_COLOR] = color
        }
    }

    suspend fun updateAssessOverlayQualityColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASSESS_OVERLAY_QUALITY_COLOR] = color
        }
    }

    suspend fun updateAssessOverlayStatsColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASSESS_OVERLAY_STATS_COLOR] = color
        }
    }

    suspend fun updateAssessOverlayMaterialsColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASSESS_OVERLAY_MATERIALS_COLOR] = color
        }
    }

    // Dungeon overlay settings update methods
    suspend fun updateShowDungeonOverlay(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_DUNGEON_OVERLAY] = enabled
        }
    }

    suspend fun updateShowFloorProgress(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_FLOOR_PROGRESS] = enabled
        }
    }

    suspend fun updateColorCodeDungeons(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.COLOR_CODE_DUNGEONS] = enabled
        }
    }

    suspend fun updateShowRewardsEstimate(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_REWARDS_ESTIMATE] = enabled
        }
    }

    suspend fun updateShowDungeonSpecialInfo(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_DUNGEON_SPECIAL_INFO] = enabled
        }
    }

    suspend fun updateFlashOnFloorChange(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FLASH_ON_FLOOR_CHANGE] = enabled
        }
    }

    // Combined method to update all dungeon overlay settings at once
    suspend fun updateDungeonOverlaySettings(
        showDungeonOverlay: Boolean,
        showFloorProgress: Boolean,
        colorCodeDungeons: Boolean,
        showRewardsEstimate: Boolean,
        showDungeonSpecialInfo: Boolean,
        flashOnFloorChange: Boolean
    ) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_DUNGEON_OVERLAY] = showDungeonOverlay
            preferences[PreferencesKeys.SHOW_FLOOR_PROGRESS] = showFloorProgress
            preferences[PreferencesKeys.COLOR_CODE_DUNGEONS] = colorCodeDungeons
            preferences[PreferencesKeys.SHOW_REWARDS_ESTIMATE] = showRewardsEstimate
            preferences[PreferencesKeys.SHOW_DUNGEON_SPECIAL_INFO] = showDungeonSpecialInfo
            preferences[PreferencesKeys.FLASH_ON_FLOOR_CHANGE] = flashOnFloorChange
        }
    }
}
