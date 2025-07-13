package com.lloir.ornaassistant.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloir.ornaassistant.domain.model.ItemType
import com.lloir.ornaassistant.domain.model.OrnaItem
import com.lloir.ornaassistant.domain.repository.OrnaItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemBrowserViewModel @Inject constructor(
    private val itemRepository: OrnaItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemBrowserUiState())
    val uiState: StateFlow<ItemBrowserUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedType = MutableStateFlow<ItemType?>(null)
    val selectedType: StateFlow<ItemType?> = _selectedType.asStateFlow()

    private val _tierFilter = MutableStateFlow<TierFilter>(TierFilter.ALL)
    val tierFilter: StateFlow<TierFilter> = _tierFilter.asStateFlow()

    private val _showBossOnly = MutableStateFlow(false)
    val showBossOnly: StateFlow<Boolean> = _showBossOnly.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadItems()
        setupSearchFlow()
    }

    private fun loadItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                itemRepository.loadAllItems()
                _uiState.value = _uiState.value.copy(isLoading = false)
                applyFilters()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load items: ${e.message}"
                )
            }
        }
    }

    private fun setupSearchFlow() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Add debounce to avoid too many searches while typing
                .distinctUntilChanged()
                .collect {
                    applyFilters()
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedType(type: ItemType?) {
        _selectedType.value = type
        applyFilters()
    }

    fun setTierFilter(filter: TierFilter) {
        _tierFilter.value = filter
        applyFilters()
    }

    fun setShowBossOnly(showBossOnly: Boolean) {
        _showBossOnly.value = showBossOnly
        applyFilters()
    }

    private fun applyFilters() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Start with all items
                var filteredItems = itemRepository.loadAllItems()

                // Apply search filter
                if (_searchQuery.value.isNotEmpty()) {
                    filteredItems = filteredItems.filter { 
                        it.name.contains(_searchQuery.value, ignoreCase = true) 
                    }
                }

                // Apply type filter
                _selectedType.value?.let { type ->
                    filteredItems = filteredItems.filter { it.type == type }
                }

                // Apply tier filter
                when (_tierFilter.value) {
                    TierFilter.ALL -> { /* No filtering needed */ }
                    is TierFilter.SINGLE -> {
                        val tier = (_tierFilter.value as TierFilter.SINGLE).tier
                        filteredItems = filteredItems.filter { it.tier == tier }
                    }
                    is TierFilter.RANGE -> {
                        val range = (_tierFilter.value as TierFilter.RANGE)
                        filteredItems = filteredItems.filter { 
                            it.tier != null && it.tier >= range.min && it.tier <= range.max 
                        }
                    }
                }

                // Apply boss filter
                if (_showBossOnly.value) {
                    filteredItems = filteredItems.filter { it.isBossItem }
                }

                // Update UI state with filtered items
                _uiState.value = _uiState.value.copy(
                    items = filteredItems,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error applying filters: ${e.message}"
                )
            }
        }
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedType.value = null
        _tierFilter.value = TierFilter.ALL
        _showBossOnly.value = false
        applyFilters()
    }

    fun refreshItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Force reload items from repository
                itemRepository.loadAllItems()
                applyFilters()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to refresh items: ${e.message}"
                )
            }
        }
    }
}

data class ItemBrowserUiState(
    val items: List<OrnaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class TierFilter {
    object ALL : TierFilter()
    data class SINGLE(val tier: Int) : TierFilter()
    data class RANGE(val min: Int, val max: Int) : TierFilter()
}
