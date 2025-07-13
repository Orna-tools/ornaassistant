package com.lloir.ornaassistant.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.domain.model.ThemeMode
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import com.lloir.ornaassistant.domain.usecase.DebugUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.graphics.Color

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val debugUseCases: DebugUseCases
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.getSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    private val _debugLogResult = MutableStateFlow<DebugUseCases.Result?>(null)
    val debugLogResult: StateFlow<DebugUseCases.Result?> = _debugLogResult.asStateFlow()

    private val _isSubmittingLogs = MutableStateFlow(false)
    val isSubmittingLogs: StateFlow<Boolean> = _isSubmittingLogs.asStateFlow()

    fun updateSessionOverlay(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateSettings(
                currentSettings.copy(showSessionOverlay = enabled)
            )
        }
    }

    fun updateAssessOverlay(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAssessOverlay(enabled)
        }
    }

    fun updateOverlayTransparency(transparency: Float) {
        viewModelScope.launch {
            settingsRepository.updateOverlayTransparency(transparency)
        }
    }

    fun updateNotificationSounds(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateSettings(
                currentSettings.copy(notificationSounds = enabled)
            )
        }
    }

    fun updateAutoHideOverlays(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateSettings(
                currentSettings.copy(autoHideOverlays = enabled)
            )
        }
    }

    fun updateDebugMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDebugMode(enabled)
        }
    }

    fun submitDebugLogs(userDescription: String, userEmail: String? = null) {
        viewModelScope.launch {
            _isSubmittingLogs.value = true
            try {
                val result = debugUseCases(userDescription, userEmail)
                _debugLogResult.value = result
            } catch (e: Exception) {
                _debugLogResult.value = DebugUseCases.Result.Error(e.message ?: "Unknown error")
            } finally {
                _isSubmittingLogs.value = false
            }
        }
    }

    fun clearDebugLogResult() {
        _debugLogResult.value = null
    }

    fun resetSubmissionState() {
        _isSubmittingLogs.value = false
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(themeMode)
        }
    }

    fun updateUseDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseDynamicColors(enabled)
        }
    }

    fun updateUseAdaptiveLayouts(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseAdaptiveLayouts(enabled)
        }
    }

    fun updateUseHighContrastMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseHighContrastMode(enabled)
        }
    }

    fun updateUseLargerFontSize(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseLargerFontSize(enabled)
        }
    }

    fun updateUseTextToSpeech(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseTextToSpeech(enabled)
        }
    }

    fun updateUseReducedMotion(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseReducedMotion(enabled)
        }
    }

    // Tutorial settings methods
    fun updateHasCompletedTutorial(completed: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateHasCompletedTutorial(completed)
        }
    }

    fun updateShowFeatureTutorials(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowFeatureTutorials(show)
        }
    }

    fun restartTutorial() {
        viewModelScope.launch {
            settingsRepository.updateHasCompletedTutorial(false)
        }
    }

    // Assessment overlay customization methods

    fun updateAssessOverlayFontSizes(
        titleSize: Float,
        qualitySize: Float,
        statsSize: Float,
        materialsSize: Float
    ) {
        viewModelScope.launch {
            settingsRepository.updateAssessOverlayFontSizes(
                titleSize,
                qualitySize,
                statsSize,
                materialsSize
            )
        }
    }

    fun updateAssessOverlayColors(
        titleColor: Int,
        qualityColor: Int,
        statsColor: Int,
        materialsColor: Int
    ) {
        viewModelScope.launch {
            settingsRepository.updateAssessOverlayColors(
                titleColor,
                qualityColor,
                statsColor,
                materialsColor
            )
        }
    }

    fun updateAssessOverlayContent(
        showMaterials: Boolean,
        showStats: Boolean
    ) {
        viewModelScope.launch {
            settingsRepository.updateAssessOverlayContent(
                showMaterials,
                showStats
            )
        }
    }

    // Individual font size update methods

    fun updateAssessOverlayTitleSize(size: Float) {
        viewModelScope.launch {
            settingsRepository.updateAssessOverlayTitleSize(size)
        }
    }

    fun updateAssessOverlayQualitySize(size: Float) {
        viewModelScope.launch {
            settingsRepository.updateAssessOverlayQualitySize(size)
        }
    }

    fun updateAssessOverlayStatsSize(size: Float) {
        viewModelScope.launch {
            settingsRepository.updateAssessOverlayStatsSize(size)
        }
    }

    fun updateAssessOverlayMaterialsSize(size: Float) {
        viewModelScope.launch {
            settingsRepository.updateAssessOverlayMaterialsSize(size)
        }
    }

    // Individual color update methods

    fun updateAssessOverlayTitleColor(color: Int) {
        viewModelScope.launch {
            settingsRepository.updateAssessOverlayTitleColor(color)
        }
    }

    fun updateAssessOverlayQualityColor(color: Int) {
        viewModelScope.launch {
            settingsRepository.updateAssessOverlayQualityColor(color)
        }
    }

    fun updateAssessOverlayStatsColor(color: Int) {
        viewModelScope.launch {
            settingsRepository.updateAssessOverlayStatsColor(color)
        }
    }

    fun updateAssessOverlayMaterialsColor(color: Int) {
        viewModelScope.launch {
            settingsRepository.updateAssessOverlayMaterialsColor(color)
        }
    }

    // Dungeon overlay settings update methods

    fun updateShowDungeonOverlay(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowDungeonOverlay(enabled)
        }
    }

    fun updateShowFloorProgress(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowFloorProgress(enabled)
        }
    }

    fun updateColorCodeDungeons(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateColorCodeDungeons(enabled)
        }
    }

    fun updateShowRewardsEstimate(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowRewardsEstimate(enabled)
        }
    }

    fun updateShowDungeonSpecialInfo(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowDungeonSpecialInfo(enabled)
        }
    }

    fun updateFlashOnFloorChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateFlashOnFloorChange(enabled)
        }
    }

    // Combined method to update all dungeon overlay settings at once
    fun updateDungeonOverlaySettings(
        showDungeonOverlay: Boolean,
        showFloorProgress: Boolean,
        colorCodeDungeons: Boolean,
        showRewardsEstimate: Boolean,
        showDungeonSpecialInfo: Boolean,
        flashOnFloorChange: Boolean
    ) {
        viewModelScope.launch {
            settingsRepository.updateDungeonOverlaySettings(
                showDungeonOverlay,
                showFloorProgress,
                colorCodeDungeons,
                showRewardsEstimate,
                showDungeonSpecialInfo,
                flashOnFloorChange
            )
        }
    }

    // Dungeon overlay font size methods
    fun updateDungeonOverlayFontSizes(
        titleSize: Float,
        modeSize: Float,
        floorSize: Float,
        rewardsSize: Float,
        cooldownSize: Float,
        specialInfoSize: Float
    ) {
        viewModelScope.launch {
            settingsRepository.updateDungeonOverlayFontSizes(
                titleSize,
                modeSize,
                floorSize,
                rewardsSize,
                cooldownSize,
                specialInfoSize
            )
        }
    }

    // Dungeon overlay color methods
    fun updateDungeonOverlayColors(
        titleColor: Int,
        modeColor: Int,
        floorColor: Int,
        rewardsColor: Int,
        cooldownColor: Int,
        specialInfoColor: Int
    ) {
        viewModelScope.launch {
            settingsRepository.updateDungeonOverlayColors(
                titleColor,
                modeColor,
                floorColor,
                rewardsColor,
                cooldownColor,
                specialInfoColor
            )
        }
    }

    // Individual font size update methods for dungeon overlay
    fun updateDungeonOverlayTitleSize(size: Float) {
        viewModelScope.launch {
            settingsRepository.updateDungeonOverlayTitleSize(size)
        }
    }

    fun updateDungeonOverlayModeSize(size: Float) {
        viewModelScope.launch {
            settingsRepository.updateDungeonOverlayModeSize(size)
        }
    }

    fun updateDungeonOverlayFloorSize(size: Float) {
        viewModelScope.launch {
            settingsRepository.updateDungeonOverlayFloorSize(size)
        }
    }

    fun updateDungeonOverlayRewardsSize(size: Float) {
        viewModelScope.launch {
            settingsRepository.updateDungeonOverlayRewardsSize(size)
        }
    }

    fun updateDungeonOverlayCooldownSize(size: Float) {
        viewModelScope.launch {
            settingsRepository.updateDungeonOverlayCooldownSize(size)
        }
    }

    fun updateDungeonOverlaySpecialInfoSize(size: Float) {
        viewModelScope.launch {
            settingsRepository.updateDungeonOverlaySpecialInfoSize(size)
        }
    }

    // Individual color update methods for dungeon overlay
    fun updateDungeonOverlayTitleColor(color: Int) {
        viewModelScope.launch {
            settingsRepository.updateDungeonOverlayTitleColor(color)
        }
    }

    fun updateDungeonOverlayModeColor(color: Int) {
        viewModelScope.launch {
            settingsRepository.updateDungeonOverlayModeColor(color)
        }
    }

    fun updateDungeonOverlayFloorColor(color: Int) {
        viewModelScope.launch {
            settingsRepository.updateDungeonOverlayFloorColor(color)
        }
    }

    fun updateDungeonOverlayRewardsColor(color: Int) {
        viewModelScope.launch {
            settingsRepository.updateDungeonOverlayRewardsColor(color)
        }
    }

    fun updateDungeonOverlayCooldownColor(color: Int) {
        viewModelScope.launch {
            settingsRepository.updateDungeonOverlayCooldownColor(color)
        }
    }

    fun updateDungeonOverlaySpecialInfoColor(color: Int) {
        viewModelScope.launch {
            settingsRepository.updateDungeonOverlaySpecialInfoColor(color)
        }
    }
}
