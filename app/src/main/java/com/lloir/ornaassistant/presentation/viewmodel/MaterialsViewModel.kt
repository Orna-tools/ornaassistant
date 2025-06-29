package com.lloir.ornaassistant.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.domain.model.Material
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import com.lloir.ornaassistant.domain.usecase.GetAllMaterialsUseCase
import com.lloir.ornaassistant.domain.usecase.GetTrackedMaterialsUseCase
import com.lloir.ornaassistant.domain.usecase.TrackMaterialUseCase
import com.lloir.ornaassistant.domain.usecase.StopTrackingMaterialUseCase
import com.lloir.ornaassistant.domain.usecase.UpdateMaterialQuantityUseCase
import com.lloir.ornaassistant.domain.usecase.SearchMaterialsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Materials screen.
 */
data class MaterialsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val searchResults: List<Material> = emptyList(),
    val isSearching: Boolean = false
)

/**
 * ViewModel for the Materials screen.
 */
@HiltViewModel
class MaterialsViewModel @Inject constructor(
    private val getAllMaterialsUseCase: GetAllMaterialsUseCase,
    private val getTrackedMaterialsUseCase: GetTrackedMaterialsUseCase,
    private val trackMaterialUseCase: TrackMaterialUseCase,
    private val stopTrackingMaterialUseCase: StopTrackingMaterialUseCase,
    private val updateMaterialQuantityUseCase: UpdateMaterialQuantityUseCase,
    private val searchMaterialsUseCase: SearchMaterialsUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // UI state
    private val _uiState = MutableStateFlow(MaterialsUiState())
    val uiState: StateFlow<MaterialsUiState> = _uiState.asStateFlow()

    // Settings
    val settings = settingsRepository.getSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    // All materials
    val allMaterials: StateFlow<List<Material>> = getAllMaterialsUseCase()
        .catch { e ->
            _uiState.update { it.copy(error = "Error loading materials: ${e.message}") }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Tracked materials
    val trackedMaterials: StateFlow<List<Material>> = getTrackedMaterialsUseCase()
        .catch { e ->
            _uiState.update { it.copy(error = "Error loading tracked materials: ${e.message}") }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Start tracking a material with a target quantity.
     */
    fun trackMaterial(materialId: Long, targetQuantity: Int) {
        viewModelScope.launch {
            try {
                trackMaterialUseCase(materialId, targetQuantity)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error tracking material: ${e.message}") }
            }
        }
    }

    /**
     * Stop tracking a material.
     */
    fun stopTrackingMaterial(materialId: Long) {
        viewModelScope.launch {
            try {
                stopTrackingMaterialUseCase(materialId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error stopping tracking: ${e.message}") }
            }
        }
    }

    /**
     * Update the quantity of a material.
     */
    fun updateMaterialQuantity(materialId: Long, quantity: Int) {
        viewModelScope.launch {
            try {
                updateMaterialQuantityUseCase(materialId, quantity)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error updating quantity: ${e.message}") }
            }
        }
    }

    /**
     * Search for materials by name.
     */
    fun searchMaterials(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearching = true) }

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        viewModelScope.launch {
            try {
                val results = searchMaterialsUseCase(query)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Error searching materials: ${e.message}",
                        isSearching = false
                    ) 
                }
            }
        }
    }

    /**
     * Clear any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
