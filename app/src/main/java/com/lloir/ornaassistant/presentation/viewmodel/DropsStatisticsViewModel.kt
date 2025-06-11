package com.lloir.ornaassistant.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloir.ornaassistant.domain.repository.ItemAssessmentRepository
import com.lloir.ornaassistant.utils.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class DropsStatisticsViewModel @Inject constructor(
    private val itemAssessmentRepository: ItemAssessmentRepository,
    private val csvExporter: CsvExporter
) : ViewModel() {

    private val _dropsStats = MutableStateFlow(DropsStatistics())
    val dropsStats: StateFlow<DropsStatistics> = _dropsStats.asStateFlow()
    
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    init {
        loadStatistics()
        observeAssessments()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val totalAssessments = itemAssessmentRepository.getAllAssessments().first().size
                val ornateCount = itemAssessmentRepository.getOrnateCount(LocalDateTime.MIN)
                val godforgeCount = itemAssessmentRepository.getGodforgeCount(LocalDateTime.MIN)
                val lastOrnate = itemAssessmentRepository.getLastOrnate()
                val lastGodforge = itemAssessmentRepository.getLastGodforge()
                
                _dropsStats.value = DropsStatistics(
                    totalAssessments = totalAssessments,
                    ornateCount = ornateCount,
                    godforgeCount = godforgeCount,
                    lastOrnateTime = lastOrnate?.timestamp,
                    lastGodforgeTime = lastGodforge?.timestamp
                )
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    private fun observeAssessments() {
        itemAssessmentRepository.getAllAssessments()
            .onEach { loadStatistics() }
            .launchIn(viewModelScope)
    }
    
    fun exportAssessments() {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val assessments = itemAssessmentRepository.getAllAssessmentsForExport()
                val uri = csvExporter.exportAssessments(assessments)
                
                uri?.let {
                    csvExporter.shareFile(it, "Export Item Assessments")
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isExporting.value = false
            }
        }
    }
}

data class DropsStatistics(
    val totalAssessments: Int = 0,
    val ornateCount: Int = 0,
    val godforgeCount: Int = 0,
    val demonforgeCount: Int = 0,
    val masterforgeCount: Int = 0,
    val lastOrnateTime: LocalDateTime? = null,
    val lastGodforgeTime: LocalDateTime? = null,
    val lastDemonforgeTime: LocalDateTime? = null,
    val lastMasterforgeTime: LocalDateTime? = null
) {
    val ornateRate: Float =
        if (totalAssessments > 0) (ornateCount.toFloat() / totalAssessments) * 100 else 0f
    val godforgeRate: Float =
        if (totalAssessments > 0) (godforgeCount.toFloat() / totalAssessments) * 100 else 0f
}
