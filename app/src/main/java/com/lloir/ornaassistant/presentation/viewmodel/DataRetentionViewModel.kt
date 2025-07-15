package com.lloir.ornaassistant.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloir.ornaassistant.data.retention.DataRetentionService
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the data retention screen.
 */
@HiltViewModel
class DataRetentionViewModel @Inject constructor(
    private val dataRetentionService: DataRetentionService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings = settingsRepository.getSettingsFlow()
    
    var showPruneDialog by mutableStateOf(false)
        private set
    
    var isPruning by mutableStateOf(false)
        private set
    
    var pruneResult by mutableStateOf<PruneResult?>(null)
        private set
    
    /**
     * Updates the dungeon data retention days setting.
     */
    fun updateDungeonRetention(days: Int) {
        viewModelScope.launch {
            settingsRepository.updateDungeonDataRetentionDays(days)
        }
    }
    
    /**
     * Updates the assessment data retention days setting.
     */
    fun updateAssessmentRetention(days: Int) {
        viewModelScope.launch {
            settingsRepository.updateAssessmentDataRetentionDays(days)
        }
    }
    
    /**
     * Shows the prune data confirmation dialog.
     */
    fun showPruneDataDialog() {
        showPruneDialog = true
    }
    
    /**
     * Dismisses the prune data confirmation dialog.
     */
    fun dismissPruneDataDialog() {
        showPruneDialog = false
    }
    
    /**
     * Prunes old data based on retention settings.
     */
    fun pruneDataNow() {
        viewModelScope.launch {
            isPruning = true
            pruneResult = null
            
            try {
                val prunedData = dataRetentionService.pruneOldData()
                pruneResult = if (prunedData) {
                    PruneResult.Success("Old data has been successfully pruned.")
                } else {
                    PruneResult.Success("No data needed pruning.")
                }
            } catch (e: Exception) {
                pruneResult = PruneResult.Error("Failed to prune data: ${e.message}")
            } finally {
                isPruning = false
            }
        }
    }
    
    /**
     * Clears the prune result.
     */
    fun clearPruneResult() {
        pruneResult = null
    }
    
    /**
     * Result of the prune operation.
     */
    sealed class PruneResult {
        data class Success(val message: String) : PruneResult()
        data class Error(val message: String) : PruneResult()
    }
}