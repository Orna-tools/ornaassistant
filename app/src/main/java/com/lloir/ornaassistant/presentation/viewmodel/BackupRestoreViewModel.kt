package com.lloir.ornaassistant.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloir.ornaassistant.data.backup.DataBackupService
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.domain.model.BackupFrequency
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for the backup and restore screen.
 */
@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val dataBackupService: DataBackupService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _backupState = MutableStateFlow(BackupState())
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    val settings = settingsRepository.getSettingsFlow()

    init {
        viewModelScope.launch {
            // Load last backup date from settings
            settingsRepository.getSettings().lastBackupDate?.let { date ->
                _backupState.update { it.copy(lastBackupDate = date) }
            }
            
            // Load available backups
            loadAvailableBackups()
        }
    }

    /**
     * Creates a backup of the app data.
     */
    fun createBackup() {
        viewModelScope.launch {
            _backupState.update { it.copy(isInProgress = true, error = null) }
            
            try {
                // In a real implementation, we would show a file picker dialog
                // For now, we'll create an automatic backup
                val backupUri = dataBackupService.createAutomaticBackup()
                
                if (backupUri != null) {
                    // Update last backup date
                    val now = LocalDateTime.now()
                    settingsRepository.updateLastBackupDate(now)
                    _backupState.update { it.copy(lastBackupDate = now) }
                    
                    // Reload available backups
                    loadAvailableBackups()
                } else {
                    _backupState.update { it.copy(error = "Failed to create backup") }
                }
            } catch (e: Exception) {
                _backupState.update { it.copy(error = "Error: ${e.message}") }
            } finally {
                _backupState.update { it.copy(isInProgress = false) }
            }
        }
    }

    /**
     * Restores a backup from the specified URI.
     * If no URI is provided, a file picker dialog will be shown.
     */
    fun restoreBackup(uri: Uri? = null) {
        viewModelScope.launch {
            _backupState.update { it.copy(isInProgress = true, error = null) }
            
            try {
                if (uri != null) {
                    // Restore from the provided URI
                    val result = dataBackupService.restoreBackup(uri)
                    
                    if (result.isSuccess) {
                        // Reload settings after restore
                        val settings = settingsRepository.getSettings()
                        _backupState.update { it.copy(lastBackupDate = settings.lastBackupDate) }
                    } else {
                        _backupState.update { it.copy(error = "Failed to restore backup: ${result.exceptionOrNull()?.message}") }
                    }
                } else {
                    // In a real implementation, we would show a file picker dialog
                    // For now, we'll just show an error
                    _backupState.update { it.copy(error = "File picker not implemented") }
                }
            } catch (e: Exception) {
                _backupState.update { it.copy(error = "Error: ${e.message}") }
            } finally {
                _backupState.update { it.copy(isInProgress = false) }
            }
        }
    }

    /**
     * Updates the auto backup enabled setting.
     */
    fun updateAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoBackupEnabled(enabled)
            
            // Schedule or cancel automatic backups
            dataBackupService.scheduleAutomaticBackups()
        }
    }

    /**
     * Updates the auto backup frequency setting.
     */
    fun updateAutoBackupFrequency(frequency: BackupFrequency) {
        viewModelScope.launch {
            settingsRepository.updateAutoBackupFrequency(frequency)
            
            // Update automatic backup schedule
            dataBackupService.scheduleAutomaticBackups()
        }
    }

    /**
     * Updates the auto backup retention setting.
     */
    fun updateAutoBackupRetention(retention: Int) {
        viewModelScope.launch {
            settingsRepository.updateAutoBackupRetention(retention)
        }
    }

    /**
     * Loads the list of available backups.
     */
    private suspend fun loadAvailableBackups() {
        try {
            val backups = dataBackupService.getAvailableBackups()
            _backupState.update { it.copy(availableBackups = backups) }
        } catch (e: Exception) {
            _backupState.update { it.copy(error = "Error loading backups: ${e.message}") }
        }
    }

    /**
     * State for the backup and restore screen.
     */
    data class BackupState(
        val isInProgress: Boolean = false,
        val lastBackupDate: LocalDateTime? = null,
        val error: String? = null,
        val availableBackups: List<DataBackupService.BackupInfo> = emptyList()
    )
}