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
}