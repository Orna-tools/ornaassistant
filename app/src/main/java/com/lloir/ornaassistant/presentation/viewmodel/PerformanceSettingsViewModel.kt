package com.lloir.ornaassistant.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloir.ornaassistant.data.performance.BatterySaverService
import com.lloir.ornaassistant.data.performance.MemoryOptimizationService
import com.lloir.ornaassistant.data.performance.OfflineModeService
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the performance settings screen.
 */
@HiltViewModel
class PerformanceSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val batterySaverService: BatterySaverService,
    private val offlineModeService: OfflineModeService,
    private val memoryOptimizationService: MemoryOptimizationService
) : ViewModel() {

    val settings = settingsRepository.getSettingsFlow()
    
    private val _memoryUsage = MutableStateFlow("")
    val memoryUsage: StateFlow<String> = _memoryUsage.asStateFlow()
    
    init {
        updateMemoryUsage()
    }
    
    /**
     * Updates the battery saver mode setting.
     */
    fun updateBatterySaverMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateBatterySaverMode(enabled)
            batterySaverService.applyBatterySaverMode()
        }
    }
    
    /**
     * Updates the offline mode setting.
     */
    fun updateOfflineMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateOfflineMode(enabled)
            offlineModeService.applyOfflineMode()
        }
    }
    
    /**
     * Updates the low memory mode setting.
     */
    fun updateLowMemoryMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateLowMemoryMode(enabled)
            memoryOptimizationService.applyMemoryOptimizations()
        }
    }
    
    /**
     * Clears the memory cache.
     */
    fun clearMemoryCache() {
        viewModelScope.launch {
            memoryOptimizationService.clearMemoryCache()
            updateMemoryUsage()
        }
    }
    
    /**
     * Updates the memory usage display.
     */
    private fun updateMemoryUsage() {
        _memoryUsage.update { memoryOptimizationService.getCurrentMemoryUsage() }
    }
}