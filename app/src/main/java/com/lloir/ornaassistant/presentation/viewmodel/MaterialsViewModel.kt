package com.lloir.ornaassistant.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.domain.model.Material
import com.lloir.ornaassistant.domain.repository.MaterialRepository
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
    val isSearching: Boolean = false,
    val trackedMaterials: List<Material> = emptyList(),
    val filteredMaterials: List<Material> = emptyList(),
    val showTrackingDialog: Boolean = false,
    val selectedMaterial: Material? = null,
    val hasMoreData: Boolean = true,
    val totalItems: Int = 0,
    val currentPage: Int = 0
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
    private val settingsRepository: SettingsRepository,
    private val materialRepository: MaterialRepository
) : ViewModel() {

    // Constants
    private val PAGE_SIZE = 20

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

    // Tracked materials
    private val _trackedMaterials: StateFlow<List<Material>> = getTrackedMaterialsUseCase()
        .catch { e ->
            _uiState.update { it.copy(error = "Error loading tracked materials: ${e.message}") }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Load initial data
        viewModelScope.launch {
            loadInitialData()

            // Update UI state with tracked materials
            _trackedMaterials.collect { tracked ->
                _uiState.update { 
                    it.copy(trackedMaterials = tracked)
                }
            }
        }
    }

    private suspend fun loadInitialData() {
        _uiState.update { it.copy(isLoading = true) }

        try {
            // Get total count
            val totalCount = materialRepository.getMaterialsCount()

            // Load first page
            val materials = materialRepository.getMaterialsPaginated(PAGE_SIZE, 0)

            _uiState.update { 
                it.copy(
                    filteredMaterials = materials,
                    totalItems = totalCount,
                    currentPage = 1,
                    hasMoreData = materials.size < totalCount,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    error = "Error loading materials: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value

        if (currentState.isLoading || !currentState.hasMoreData) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val offset = currentState.currentPage * PAGE_SIZE
                val searchQuery = currentState.searchQuery

                val newItems = if (searchQuery.isBlank()) {
                    materialRepository.getMaterialsPaginated(PAGE_SIZE, offset)
                } else {
                    materialRepository.searchMaterialsPaginated(searchQuery, PAGE_SIZE, offset)
                }

                if (newItems.isEmpty()) {
                    _uiState.update { it.copy(hasMoreData = false, isLoading = false) }
                } else {
                    val updatedList = currentState.filteredMaterials.toMutableList().apply {
                        addAll(newItems)
                    }

                    _uiState.update { 
                        it.copy(
                            filteredMaterials = updatedList,
                            currentPage = it.currentPage + 1,
                            hasMoreData = updatedList.size < it.totalItems,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Error loading more materials: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun refresh() {
        _uiState.update { 
            it.copy(
                currentPage = 0,
                filteredMaterials = emptyList(),
                hasMoreData = true
            )
        }

        viewModelScope.launch {
            loadInitialData()
        }
    }

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
     * Search for materials by name with pagination.
     */
    fun searchMaterials(query: String) {
        // Reset pagination state
        _uiState.update { 
            it.copy(
                searchQuery = query, 
                isSearching = true,
                currentPage = 0,
                filteredMaterials = emptyList(),
                hasMoreData = true
            ) 
        }

        if (query.isBlank()) {
            // If query is blank, load all materials
            viewModelScope.launch {
                loadInitialData()
            }
            return
        }

        viewModelScope.launch {
            try {
                // Get total count for search results
                val totalCount = materialRepository.searchMaterials(query).size

                // Load first page of search results
                val results = materialRepository.searchMaterialsPaginated(query, PAGE_SIZE, 0)

                _uiState.update { 
                    it.copy(
                        totalItems = totalCount,
                        currentPage = 1,
                        filteredMaterials = results,
                        hasMoreData = results.size < totalCount,
                        isSearching = false
                    )
                }
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
     * Update the search query.
     */
    fun updateSearchQuery(query: String) {
        searchMaterials(query)
    }

    /**
     * Show the tracking dialog for a material.
     */
    fun showTrackingDialog(material: Material) {
        _uiState.update { it.copy(showTrackingDialog = true, selectedMaterial = material) }
    }

    /**
     * Dismiss the tracking dialog.
     */
    fun dismissTrackingDialog() {
        _uiState.update { it.copy(showTrackingDialog = false, selectedMaterial = null) }
    }

    /**
     * Clear any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Update the material tracking setting.
     */
    fun updateEnableMaterialTracking(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateEnableMaterialTracking(enabled)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error updating material tracking setting: ${e.message}") }
            }
        }
    }
}
