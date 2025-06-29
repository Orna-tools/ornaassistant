package com.lloir.ornaassistant.presentation.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloir.ornaassistant.domain.model.*
import com.lloir.ornaassistant.domain.repository.*
import com.lloir.ornaassistant.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for the main screen of the application.
 * 
 * This ViewModel is responsible for:
 * - Loading and providing dungeon statistics for display
 * - Managing weekly statistics for charts and summaries
 * - Handling user settings and preferences
 * - Managing loading states and error handling
 * 
 * It interacts with domain layer use cases to retrieve data and exposes
 * this data to the UI through StateFlow objects, following MVVM architecture
 * principles and unidirectional data flow.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getDungeonStatisticsUseCase: GetDungeonStatisticsUseCase,
    private val getWeeklyStatisticsUseCase: GetWeeklyStatisticsUseCase,
    private val dungeonRepository: DungeonRepository,
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

/**
 * ViewModel for the settings screen of the application.
 * 
 * This ViewModel is responsible for:
 * - Managing user preferences and settings
 * - Handling overlay visibility and transparency settings
 * - Managing notification preferences
 * - Providing debug log submission functionality
 * - Persisting settings changes to the data store
 * 
 * It interacts with the settings repository to retrieve and update settings,
 * and with debug use cases to collect and submit logs for troubleshooting.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val debugUseCases: DebugUseCases
) : ViewModel() {

    val settings = settingsRepository.getSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
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

    fun updateUseMlKit(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseMlKit(enabled)
        }
    }

    fun updateEnableMaterialTracking(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEnableMaterialTracking(enabled)
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
}

/**
 * ViewModel for the dungeon history screen of the application.
 * 
 * This ViewModel is responsible for:
 * - Loading and filtering dungeon visit history
 * - Managing time range selection for filtering
 * - Providing dungeon visit data to the UI
 * - Handling deletion of dungeon visit records
 * 
 * It interacts with the dungeon repository to retrieve visit data and
 * applies filtering based on user-selected time ranges. The data is exposed
 * to the UI through StateFlow objects for reactive updates.
 * 
 * Requires Android O (API 26) or higher due to use of LocalDateTime.
 */
@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class DungeonHistoryViewModel @Inject constructor(
    private val dungeonRepository: DungeonRepository,
) : ViewModel() {

    private val TAG = "DungeonHistoryVM"

    private val _selectedTimeRange = MutableStateFlow(TimeRange.WEEK)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()

    val dungeonVisits = dungeonRepository.getAllVisits()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _filteredVisits = MutableStateFlow<List<DungeonVisit>>(emptyList())
    val filteredVisits: StateFlow<List<DungeonVisit>> = _filteredVisits.asStateFlow()

    init {
        // Combine time range selection with dungeon visits
        viewModelScope.launch {
            combine(dungeonVisits, selectedTimeRange) { visits, timeRange ->
                Log.d(TAG, "Received ${visits.size} dungeon visits from repository")
                visits.forEach { visit ->
                    Log.d(TAG, "Visit: ${visit.name} - orns: ${visit.orns}, gold: ${visit.gold}, exp: ${visit.experience}")
                    Log.d(TAG, "  - Floor rewards: ${visit.floorRewards}")
                }
                filterVisitsByTimeRange(visits, timeRange)
            }.collect { filtered ->
                Log.d(TAG, "Filtered to ${filtered.size} visits for time range: ${selectedTimeRange.value}")
                filtered.forEach { visit ->
                    Log.d(TAG, "Filtered visit: ${visit.name} - orns: ${visit.orns}, gold: ${visit.gold}, exp: ${visit.experience}")
                }
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
}

/**
 * ViewModel for managing the accessibility service state.
 * 
 * This ViewModel is responsible for:
 * - Tracking the connection status of the accessibility service
 * - Managing permission status for accessibility and overlay features
 * - Coordinating notifications related to the service state
 * - Providing service status information to the UI
 * 
 * It acts as a bridge between the UI and the accessibility service,
 * allowing the UI to react to service state changes and permission status
 * updates without direct coupling to the service implementation.
 */
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
/**
 * ViewModel for managing chart data in the application.
 * 
 * This ViewModel is responsible for:
 * - Loading and processing weekly dungeon statistics for charts
 * - Converting domain data into chart-friendly formats
 * - Providing formatted chart data to UI components
 * - Handling refresh operations for chart data
 * 
 * It uses the weekly statistics use case to retrieve domain data and
 * transforms it into a format suitable for chart rendering in the UI.
 * The data is exposed through StateFlow for reactive updates.
 */
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
