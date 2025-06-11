package com.lloir.ornaassistant.presentation.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloir.ornaassistant.domain.model.*
import com.lloir.ornaassistant.domain.repository.*
import com.lloir.ornaassistant.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import com.lloir.ornaassistant.utils.CsvExporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getDungeonStatisticsUseCase: GetDungeonStatisticsUseCase,
    private val getWeeklyStatisticsUseCase: GetWeeklyStatisticsUseCase,
    private val dungeonRepository: DungeonRepository,
    private val wayvesselRepository: WayvesselRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _weeklyStats = MutableStateFlow<WeeklyStatistics?>(null)
    val weeklyStats: StateFlow<WeeklyStatistics?> = _weeklyStats.asStateFlow()

    val settings = settingsRepository.getSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val statistics = getDungeonStatisticsUseCase(7)
                val weeklyStats = getWeeklyStatisticsUseCase()

                _uiState.value = _uiState.value.copy(
                    dungeonStatistics = statistics,
                    isLoading = false
                )

                _weeklyStats.value = weeklyStats

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun refreshData() {
        loadInitialData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings = settingsRepository.getSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun updateSessionOverlay(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSessionOverlay(enabled)
        }
    }

    fun updateInvitesOverlay(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateInvitesOverlay(enabled)
        }
    }

    fun updateAssessOverlay(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAssessOverlay(enabled)
        }
    }

    fun updateEfficiencyOverlay(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateSettings(
                currentSettings.copy(showEfficiencyOverlay = enabled)
            )
        }
    }

    fun updateCombatLogOverlay(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateSettings(
                currentSettings.copy(showCombatLogOverlay = enabled)
            )
        }
    }

    // ... Add all other new methods from the enhanced SettingsViewModel

    fun updateWayvesselNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWayvesselNotifications(enabled)
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
}

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class DungeonHistoryViewModel @Inject constructor(
    private val dungeonRepository: DungeonRepository,
    private val wayvesselRepository: WayvesselRepository,
    private val csvExporter: CsvExporter
) : ViewModel() {

    private val _selectedTimeRange = MutableStateFlow(TimeRange.WEEK)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()

    val dungeonVisits = dungeonRepository.getAllVisits()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val wayvesselSessions = wayvesselRepository.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _filteredVisits = MutableStateFlow<List<DungeonVisit>>(emptyList())
    val filteredVisits: StateFlow<List<DungeonVisit>> = _filteredVisits.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    init {
        // Combine time range selection with dungeon visits
        viewModelScope.launch {
            combine(dungeonVisits, selectedTimeRange) { visits, timeRange ->
                filterVisitsByTimeRange(visits, timeRange)
            }.collect { filtered ->
                _filteredVisits.value = filtered
            }
        }
    }

    fun selectTimeRange(timeRange: TimeRange) {
        _selectedTimeRange.value = timeRange
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filterVisitsByTimeRange(visits: List<DungeonVisit>, timeRange: TimeRange): List<DungeonVisit> {
        val cutoffDate = when (timeRange) {
            TimeRange.DAY -> LocalDateTime.now().minusDays(1)
            TimeRange.WEEK -> LocalDateTime.now().minusDays(7)
            TimeRange.MONTH -> LocalDateTime.now().minusMonths(1)
            TimeRange.ALL -> LocalDateTime.MIN
        }

        return visits.filter { it.startTime.isAfter(cutoffDate) }
    }

    fun deleteVisit(visit: DungeonVisit) {
        viewModelScope.launch {
            dungeonRepository.deleteVisit(visit)
        }
    }

    fun deleteAllVisits() {
        viewModelScope.launch {
            dungeonRepository.deleteAllVisits()
        }
    }
    
    fun exportDungeons() {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val visits = dungeonRepository.getAllVisitsForExport()
                val uri = csvExporter.exportDungeonVisits(visits)
                
                uri?.let {
                    csvExporter.shareFile(it, "Export Dungeon History")
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isExporting.value = false
            }
        }
    }
}

@HiltViewModel
class AccessibilityServiceViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _serviceStatus = MutableStateFlow(ServiceStatus.DISCONNECTED)
    val serviceStatus: StateFlow<ServiceStatus> = _serviceStatus.asStateFlow()

    private val _permissionStatus = MutableStateFlow(PermissionStatus.NOT_GRANTED)
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    fun updateServiceStatus(status: ServiceStatus) {
        _serviceStatus.value = status

        viewModelScope.launch {
            when (status) {
                ServiceStatus.CONNECTED -> notificationRepository.showServiceNotification()
                ServiceStatus.DISCONNECTED -> notificationRepository.hideServiceNotification()
                ServiceStatus.ERROR -> {
                    // Handle error case - maybe show error notification or hide service notification
                    notificationRepository.hideServiceNotification()
                }
            }
        }
    }

    fun updatePermissionStatus(status: PermissionStatus) {
        _permissionStatus.value = status
    }
    
    fun checkAndUpdatePermissions(hasOverlay: Boolean, hasAccessibility: Boolean) {
        val status = when {
            hasOverlay && hasAccessibility -> PermissionStatus.GRANTED
            else -> PermissionStatus.NOT_GRANTED
        }
        updatePermissionStatus(status)
    }
}

// UI State classes
data class MainUiState(
    val isLoading: Boolean = false,
    val dungeonStatistics: DungeonStatistics? = null,
    val error: String? = null
)

enum class TimeRange {
    DAY, WEEK, MONTH, ALL
}

enum class ServiceStatus {
    CONNECTED, DISCONNECTED, ERROR
}

enum class PermissionStatus {
    GRANTED, NOT_GRANTED, DENIED
}

// Chart Data ViewModels
@HiltViewModel
class ChartViewModel @Inject constructor(
    private val getWeeklyStatisticsUseCase: GetWeeklyStatisticsUseCase,
    private val dungeonRepository: DungeonRepository
) : ViewModel() {

    private val _chartData = MutableStateFlow<ChartData?>(null)
    val chartData: StateFlow<ChartData?> = _chartData.asStateFlow()

    init {
        loadChartData()
    }

    private fun loadChartData() {
        viewModelScope.launch {
            try {
                val weeklyStats = getWeeklyStatisticsUseCase()
                val chartData = createChartData(weeklyStats)
                _chartData.value = chartData
            } catch (e: Exception) {
                // Handle error - could log or show error state
            }
        }
    }

    private fun createChartData(weeklyStats: WeeklyStatistics): ChartData {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val visits = listOf(
            weeklyStats.mondayVisits,
            weeklyStats.tuesdayVisits,
            weeklyStats.wednesdayVisits,
            weeklyStats.thursdayVisits,
            weeklyStats.fridayVisits,
            weeklyStats.saturdayVisits,
            weeklyStats.sundayVisits
        )
        val orns = listOf(
            weeklyStats.mondayOrns,
            weeklyStats.tuesdayOrns,
            weeklyStats.wednesdayOrns,
            weeklyStats.thursdayOrns,
            weeklyStats.fridayOrns,
            weeklyStats.saturdayOrns,
            weeklyStats.sundayOrns
        )

        return ChartData(
            days = days,
            visits = visits,
            orns = orns
        )
    }

    fun refreshData() {
        loadChartData()
    }
}

data class ChartData(
    val days: List<String>,
    val visits: List<Int>,
    val orns: List<Long>
)