package com.lloir.ornaassistant.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.domain.model.ThemeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
        val USE_AMOLED_DARK_MODE = booleanPreferencesKey("use_amoled_dark_mode")
        val ENHANCED_DARK_MODE_CONTRAST = booleanPreferencesKey("enhanced_dark_mode_contrast")

        // Dashboard settings
        val DASHBOARD_LAYOUT = stringPreferencesKey("dashboard_layout")
        val ENABLED_WIDGETS = stringPreferencesKey("enabled_widgets")
        val WIDGET_ORDER = stringPreferencesKey("widget_order")

        // Accessibility preference keys
        val USE_HIGH_CONTRAST_MODE = booleanPreferencesKey("use_high_contrast_mode")
        val USE_LARGER_FONT_SIZE = booleanPreferencesKey("use_larger_font_size")
        val USE_TEXT_TO_SPEECH = booleanPreferencesKey("use_text_to_speech")
        val USE_REDUCED_MOTION = booleanPreferencesKey("use_reduced_motion")

        // Tutorial preference keys
        val HAS_COMPLETED_TUTORIAL = booleanPreferencesKey("has_completed_tutorial")
        val SHOW_FEATURE_TUTORIALS = booleanPreferencesKey("show_feature_tutorials")

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

        // Dungeon overlay customization keys
        val DUNGEON_OVERLAY_TITLE_SIZE = floatPreferencesKey("dungeon_overlay_title_size")
        val DUNGEON_OVERLAY_MODE_SIZE = floatPreferencesKey("dungeon_overlay_mode_size")
        val DUNGEON_OVERLAY_FLOOR_SIZE = floatPreferencesKey("dungeon_overlay_floor_size")
        val DUNGEON_OVERLAY_REWARDS_SIZE = floatPreferencesKey("dungeon_overlay_rewards_size")
        val DUNGEON_OVERLAY_COOLDOWN_SIZE = floatPreferencesKey("dungeon_overlay_cooldown_size")
        val DUNGEON_OVERLAY_SPECIAL_INFO_SIZE = floatPreferencesKey("dungeon_overlay_special_info_size")
        val DUNGEON_OVERLAY_TITLE_COLOR = intPreferencesKey("dungeon_overlay_title_color")
        val DUNGEON_OVERLAY_MODE_COLOR = intPreferencesKey("dungeon_overlay_mode_color")
        val DUNGEON_OVERLAY_FLOOR_COLOR = intPreferencesKey("dungeon_overlay_floor_color")
        val DUNGEON_OVERLAY_REWARDS_COLOR = intPreferencesKey("dungeon_overlay_rewards_color")
        val DUNGEON_OVERLAY_COOLDOWN_COLOR = intPreferencesKey("dungeon_overlay_cooldown_color")
        val DUNGEON_OVERLAY_SPECIAL_INFO_COLOR = intPreferencesKey("dungeon_overlay_special_info_color")

        // Premium theme keys
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val IS_PREMIUM_USER = booleanPreferencesKey("is_premium_user")
        val PREMIUM_EXPIRY_DATE = stringPreferencesKey("premium_expiry_date")
        
        // Backup & Restore keys
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_FREQUENCY = stringPreferencesKey("auto_backup_frequency")
        val AUTO_BACKUP_RETENTION = intPreferencesKey("auto_backup_retention")
        val LAST_BACKUP_DATE = stringPreferencesKey("last_backup_date")
        
        // Data retention keys
        val DUNGEON_DATA_RETENTION_DAYS = intPreferencesKey("dungeon_data_retention_days")
        val ASSESSMENT_DATA_RETENTION_DAYS = intPreferencesKey("assessment_data_retention_days")
        
        // Performance keys
        val BATTERY_SAVER_MODE = booleanPreferencesKey("battery_saver_mode")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val LOW_MEMORY_MODE = booleanPreferencesKey("low_memory_mode")
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
            useLargerFontSize = preferences[PreferencesKeys.USE_LARGER_FONT_SIZE] ?: false,
            useTextToSpeech = preferences[PreferencesKeys.USE_TEXT_TO_SPEECH] ?: false,
            useReducedMotion = preferences[PreferencesKeys.USE_REDUCED_MOTION] ?: false,
            // Tutorial settings
            hasCompletedTutorial = preferences[PreferencesKeys.HAS_COMPLETED_TUTORIAL] ?: false,
            showFeatureTutorials = preferences[PreferencesKeys.SHOW_FEATURE_TUTORIALS] ?: true,

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
            flashOnFloorChange = preferences[PreferencesKeys.FLASH_ON_FLOOR_CHANGE] ?: true,

            // Dungeon overlay customization
            dungeonOverlayTitleSize = preferences[PreferencesKeys.DUNGEON_OVERLAY_TITLE_SIZE] ?: 14f,
            dungeonOverlayModeSize = preferences[PreferencesKeys.DUNGEON_OVERLAY_MODE_SIZE] ?: 12f,
            dungeonOverlayFloorSize = preferences[PreferencesKeys.DUNGEON_OVERLAY_FLOOR_SIZE] ?: 12f,
            dungeonOverlayRewardsSize = preferences[PreferencesKeys.DUNGEON_OVERLAY_REWARDS_SIZE] ?: 11f,
            dungeonOverlayCooldownSize = preferences[PreferencesKeys.DUNGEON_OVERLAY_COOLDOWN_SIZE] ?: 10f,
            dungeonOverlaySpecialInfoSize = preferences[PreferencesKeys.DUNGEON_OVERLAY_SPECIAL_INFO_SIZE] ?: 10f,
            dungeonOverlayTitleColor = preferences[PreferencesKeys.DUNGEON_OVERLAY_TITLE_COLOR] ?: android.graphics.Color.WHITE,
            dungeonOverlayModeColor = preferences[PreferencesKeys.DUNGEON_OVERLAY_MODE_COLOR] ?: android.graphics.Color.YELLOW,
            dungeonOverlayFloorColor = preferences[PreferencesKeys.DUNGEON_OVERLAY_FLOOR_COLOR] ?: android.graphics.Color.WHITE,
            dungeonOverlayRewardsColor = preferences[PreferencesKeys.DUNGEON_OVERLAY_REWARDS_COLOR] ?: android.graphics.Color.CYAN,
            dungeonOverlayCooldownColor = preferences[PreferencesKeys.DUNGEON_OVERLAY_COOLDOWN_COLOR] ?: android.graphics.Color.LTGRAY,
            dungeonOverlaySpecialInfoColor = preferences[PreferencesKeys.DUNGEON_OVERLAY_SPECIAL_INFO_COLOR] ?: android.graphics.Color.GREEN,

            // Theme settings
            selectedTheme = preferences[PreferencesKeys.SELECTED_THEME]?.let {
                try {
                    ThemeType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    ThemeType.DEFAULT
                }
            } ?: ThemeType.DEFAULT,

            // Backup & Restore settings
            autoBackupEnabled = preferences[PreferencesKeys.AUTO_BACKUP_ENABLED] ?: false,
            autoBackupFrequency = preferences[PreferencesKeys.AUTO_BACKUP_FREQUENCY]?.let {
                try {
                    com.lloir.ornaassistant.domain.model.BackupFrequency.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    com.lloir.ornaassistant.domain.model.BackupFrequency.WEEKLY
                }
            } ?: com.lloir.ornaassistant.domain.model.BackupFrequency.WEEKLY,
            autoBackupRetention = preferences[PreferencesKeys.AUTO_BACKUP_RETENTION] ?: 3,
            lastBackupDate = preferences[PreferencesKeys.LAST_BACKUP_DATE]?.let {
                try {
                    LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                } catch (e: Exception) {
                    null
                }
            },

            // Data retention settings
            dungeonDataRetentionDays = preferences[PreferencesKeys.DUNGEON_DATA_RETENTION_DAYS] ?: 90,
            assessmentDataRetentionDays = preferences[PreferencesKeys.ASSESSMENT_DATA_RETENTION_DAYS] ?: 30,

            // Performance settings
            batterySaverMode = preferences[PreferencesKeys.BATTERY_SAVER_MODE] ?: false,
            offlineMode = preferences[PreferencesKeys.OFFLINE_MODE] ?: false,
            lowMemoryMode = preferences[PreferencesKeys.LOW_MEMORY_MODE] ?: false,

            // Dashboard settings
            dashboardLayout = preferences[PreferencesKeys.DASHBOARD_LAYOUT]?.let {
                try {
                    com.lloir.ornaassistant.domain.model.DashboardLayout.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    com.lloir.ornaassistant.domain.model.DashboardLayout.STANDARD
                }
            } ?: com.lloir.ornaassistant.domain.model.DashboardLayout.STANDARD,
            enabledWidgets = preferences[PreferencesKeys.ENABLED_WIDGETS]?.let {
                try {
                    it.split(",").mapNotNull { widgetName ->
                        try {
                            com.lloir.ornaassistant.domain.model.DashboardWidget.valueOf(widgetName)
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    }.toSet()
                } catch (e: Exception) {
                    com.lloir.ornaassistant.domain.model.DashboardWidget.values().toSet()
                }
            } ?: com.lloir.ornaassistant.domain.model.DashboardWidget.values().toSet(),
            widgetOrder = preferences[PreferencesKeys.WIDGET_ORDER]?.let {
                try {
                    it.split(",").mapNotNull { widgetName ->
                        try {
                            com.lloir.ornaassistant.domain.model.DashboardWidget.valueOf(widgetName)
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    }
                } catch (e: Exception) {
                    com.lloir.ornaassistant.domain.model.DashboardWidget.values().toList()
                }
            } ?: com.lloir.ornaassistant.domain.model.DashboardWidget.values().toList()
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
            preferences[PreferencesKeys.USE_LARGER_FONT_SIZE] = settings.useLargerFontSize
            preferences[PreferencesKeys.USE_TEXT_TO_SPEECH] = settings.useTextToSpeech
            preferences[PreferencesKeys.USE_REDUCED_MOTION] = settings.useReducedMotion

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

    suspend fun updateUseLargerFontSize(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_LARGER_FONT_SIZE] = enabled
        }
    }

    suspend fun updateUseTextToSpeech(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_TEXT_TO_SPEECH] = enabled
        }
    }

    suspend fun updateUseReducedMotion(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_REDUCED_MOTION] = enabled
        }
    }

    // Tutorial settings methods
    suspend fun updateHasCompletedTutorial(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_TUTORIAL] = completed
        }
    }

    suspend fun updateShowFeatureTutorials(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_FEATURE_TUTORIALS] = show
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

    // Dungeon overlay font size methods
    suspend fun updateDungeonOverlayFontSizes(
        titleSize: Float,
        modeSize: Float,
        floorSize: Float,
        rewardsSize: Float,
        cooldownSize: Float,
        specialInfoSize: Float
    ) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUNGEON_OVERLAY_TITLE_SIZE] = titleSize
            preferences[PreferencesKeys.DUNGEON_OVERLAY_MODE_SIZE] = modeSize
            preferences[PreferencesKeys.DUNGEON_OVERLAY_FLOOR_SIZE] = floorSize
            preferences[PreferencesKeys.DUNGEON_OVERLAY_REWARDS_SIZE] = rewardsSize
            preferences[PreferencesKeys.DUNGEON_OVERLAY_COOLDOWN_SIZE] = cooldownSize
            preferences[PreferencesKeys.DUNGEON_OVERLAY_SPECIAL_INFO_SIZE] = specialInfoSize
        }
    }

    // Dungeon overlay color methods
    suspend fun updateDungeonOverlayColors(
        titleColor: Int,
        modeColor: Int,
        floorColor: Int,
        rewardsColor: Int,
        cooldownColor: Int,
        specialInfoColor: Int
    ) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUNGEON_OVERLAY_TITLE_COLOR] = titleColor
            preferences[PreferencesKeys.DUNGEON_OVERLAY_MODE_COLOR] = modeColor
            preferences[PreferencesKeys.DUNGEON_OVERLAY_FLOOR_COLOR] = floorColor
            preferences[PreferencesKeys.DUNGEON_OVERLAY_REWARDS_COLOR] = rewardsColor
            preferences[PreferencesKeys.DUNGEON_OVERLAY_COOLDOWN_COLOR] = cooldownColor
            preferences[PreferencesKeys.DUNGEON_OVERLAY_SPECIAL_INFO_COLOR] = specialInfoColor
        }
    }

    // Individual font size update methods for dungeon overlay
    suspend fun updateDungeonOverlayTitleSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUNGEON_OVERLAY_TITLE_SIZE] = size
        }
    }

    suspend fun updateDungeonOverlayModeSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUNGEON_OVERLAY_MODE_SIZE] = size
        }
    }

    suspend fun updateDungeonOverlayFloorSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUNGEON_OVERLAY_FLOOR_SIZE] = size
        }
    }

    suspend fun updateDungeonOverlayRewardsSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUNGEON_OVERLAY_REWARDS_SIZE] = size
        }
    }

    suspend fun updateDungeonOverlayCooldownSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUNGEON_OVERLAY_COOLDOWN_SIZE] = size
        }
    }

    suspend fun updateDungeonOverlaySpecialInfoSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUNGEON_OVERLAY_SPECIAL_INFO_SIZE] = size
        }
    }

    // Individual color update methods for dungeon overlay
    suspend fun updateDungeonOverlayTitleColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUNGEON_OVERLAY_TITLE_COLOR] = color
        }
    }

    suspend fun updateDungeonOverlayModeColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUNGEON_OVERLAY_MODE_COLOR] = color
        }
    }

    suspend fun updateDungeonOverlayFloorColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUNGEON_OVERLAY_FLOOR_COLOR] = color
        }
    }

    suspend fun updateDungeonOverlayRewardsColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUNGEON_OVERLAY_REWARDS_COLOR] = color
        }
    }

    suspend fun updateDungeonOverlayCooldownColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUNGEON_OVERLAY_COOLDOWN_COLOR] = color
        }
    }

    suspend fun updateDungeonOverlaySpecialInfoColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUNGEON_OVERLAY_SPECIAL_INFO_COLOR] = color
        }
    }

    // Premium theme methods
    suspend fun updateSelectedTheme(themeType: ThemeType) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_THEME] = themeType.name
        }
    }

    suspend fun updatePremiumStatus(isPremium: Boolean, expiryDate: LocalDateTime?) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_PREMIUM_USER] = isPremium
            preferences[PreferencesKeys.PREMIUM_EXPIRY_DATE] = expiryDate?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: ""
        }
    }

    // Backup & Restore methods
    suspend fun updateAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_BACKUP_ENABLED] = enabled
        }
    }

    suspend fun updateAutoBackupFrequency(frequency: com.lloir.ornaassistant.domain.model.BackupFrequency) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_BACKUP_FREQUENCY] = frequency.name
        }
    }

    suspend fun updateAutoBackupRetention(retention: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_BACKUP_RETENTION] = retention
        }
    }

    suspend fun updateLastBackupDate(date: LocalDateTime?) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_DATE] = date?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: ""
        }
    }

    // Data retention methods
    suspend fun updateDungeonDataRetentionDays(days: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUNGEON_DATA_RETENTION_DAYS] = days
        }
    }

    suspend fun updateAssessmentDataRetentionDays(days: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASSESSMENT_DATA_RETENTION_DAYS] = days
        }
    }

    // Performance methods
    suspend fun updateBatterySaverMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BATTERY_SAVER_MODE] = enabled
        }
    }

    suspend fun updateOfflineMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.OFFLINE_MODE] = enabled
        }
    }

    suspend fun updateLowMemoryMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOW_MEMORY_MODE] = enabled
        }
    }

    // Dark Mode Refinements methods
    suspend fun updateUseAmoledDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_AMOLED_DARK_MODE] = enabled
        }
    }

    suspend fun updateEnhancedDarkModeContrast(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENHANCED_DARK_MODE_CONTRAST] = enabled
        }
    }

    // Dashboard settings methods
    suspend fun updateDashboardLayout(layout: com.lloir.ornaassistant.domain.model.DashboardLayout) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DASHBOARD_LAYOUT] = layout.name
        }
    }

    suspend fun updateEnabledWidgets(enabledWidgets: Set<com.lloir.ornaassistant.domain.model.DashboardWidget>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLED_WIDGETS] = enabledWidgets.joinToString(",") { it.name }
        }
    }

    suspend fun updateWidgetOrder(widgetOrder: List<com.lloir.ornaassistant.domain.model.DashboardWidget>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIDGET_ORDER] = widgetOrder.joinToString(",") { it.name }
        }
    }
}