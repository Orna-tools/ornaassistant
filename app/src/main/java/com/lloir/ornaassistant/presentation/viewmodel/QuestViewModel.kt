package com.lloir.ornaassistant.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloir.ornaassistant.domain.model.Quest
import com.lloir.ornaassistant.domain.model.QuestObjective
import com.lloir.ornaassistant.domain.model.QuestRewards
import com.lloir.ornaassistant.domain.model.QuestType
import com.lloir.ornaassistant.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Data class representing the UI state for the Quest screen.
 */
data class QuestUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedQuestType: QuestType? = null,
    val showCompletedQuests: Boolean = false,
    val searchQuery: String = "",
    val questStatistics: QuestStatistics? = null
)

/**
 * ViewModel for managing quest-related data and operations.
 * 
 * This ViewModel is responsible for:
 * - Loading and filtering quests
 * - Tracking and completing quests
 * - Updating quest objectives
 * - Providing quest statistics
 * 
 * It serves as the bridge between the UI and the domain layer,
 * exposing reactive state flows that the UI can observe.
 */
@HiltViewModel
class QuestViewModel @Inject constructor(
    private val getAllQuestsUseCase: GetAllQuestsUseCase,
    private val getTrackedQuestsUseCase: GetTrackedQuestsUseCase,
    private val getActiveQuestsUseCase: GetActiveQuestsUseCase,
    private val getQuestsByTypeUseCase: GetQuestsByTypeUseCase,
    private val createQuestUseCase: CreateQuestUseCase,
    private val toggleQuestTrackingUseCase: ToggleQuestTrackingUseCase,
    private val updateObjectiveProgressUseCase: UpdateObjectiveProgressUseCase,
    private val completeQuestUseCase: CompleteQuestUseCase,
    private val getQuestStatisticsUseCase: GetQuestStatisticsUseCase
) : ViewModel() {

    // UI state
    private val _uiState = MutableStateFlow(QuestUiState())
    val uiState: StateFlow<QuestUiState> = _uiState.asStateFlow()

    // Quest data
    private val _allQuests = MutableStateFlow<List<Quest>>(emptyList())
    val allQuests: StateFlow<List<Quest>> = _allQuests.asStateFlow()

    private val _trackedQuests = MutableStateFlow<List<Quest>>(emptyList())
    val trackedQuests: StateFlow<List<Quest>> = _trackedQuests.asStateFlow()

    private val _activeQuests = MutableStateFlow<List<Quest>>(emptyList())
    val activeQuests: StateFlow<List<Quest>> = _activeQuests.asStateFlow()

    private val _filteredQuests = MutableStateFlow<List<Quest>>(emptyList())
    val filteredQuests: StateFlow<List<Quest>> = _filteredQuests.asStateFlow()

    // Selected quest
    private val _selectedQuest = MutableStateFlow<Quest?>(null)
    val selectedQuest: StateFlow<Quest?> = _selectedQuest.asStateFlow()

    init {
        loadQuests()
        loadQuestStatistics()
    }

    /**
     * Loads all quests and sets up collection of quest flows.
     */
    private fun loadQuests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Collect all quests
                getAllQuestsUseCase().collect { quests ->
                    _allQuests.value = quests
                    updateFilteredQuests()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load quests: ${e.message}") }
            }
            
            // Collect tracked quests
            viewModelScope.launch {
                getTrackedQuestsUseCase().collect { quests ->
                    _trackedQuests.value = quests
                }
            }
            
            // Collect active quests
            viewModelScope.launch {
                getActiveQuestsUseCase().collect { quests ->
                    _activeQuests.value = quests
                    updateFilteredQuests()
                }
            }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Loads quest statistics.
     */
    private fun loadQuestStatistics() {
        viewModelScope.launch {
            try {
                val statistics = getQuestStatisticsUseCase()
                _uiState.update { it.copy(questStatistics = statistics) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load quest statistics: ${e.message}") }
            }
        }
    }

    /**
     * Updates the filtered quests based on the current UI state.
     */
    private fun updateFilteredQuests() {
        val currentState = _uiState.value
        val questType = currentState.selectedQuestType
        val showCompleted = currentState.showCompletedQuests
        val searchQuery = currentState.searchQuery.lowercase()
        
        val baseQuests = if (questType != null) {
            _allQuests.value.filter { it.questType == questType }
        } else {
            _allQuests.value
        }
        
        val filteredByCompletion = if (!showCompleted) {
            baseQuests.filter { !it.isCompleted }
        } else {
            baseQuests
        }
        
        val filteredBySearch = if (searchQuery.isNotEmpty()) {
            filteredByCompletion.filter { 
                it.name.lowercase().contains(searchQuery) || 
                it.description.lowercase().contains(searchQuery) 
            }
        } else {
            filteredByCompletion
        }
        
        _filteredQuests.value = filteredBySearch.sortedWith(
            compareByDescending<Quest> { it.isTracked }
                .thenBy { it.isCompleted }
                .thenBy { it.questType.ordinal }
                .thenBy { it.name }
        )
    }

    /**
     * Sets the selected quest type filter.
     */
    fun setQuestTypeFilter(questType: QuestType?) {
        _uiState.update { it.copy(selectedQuestType = questType) }
        updateFilteredQuests()
    }

    /**
     * Sets whether to show completed quests.
     */
    fun setShowCompletedQuests(show: Boolean) {
        _uiState.update { it.copy(showCompletedQuests = show) }
        updateFilteredQuests()
    }

    /**
     * Sets the search query for filtering quests.
     */
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        updateFilteredQuests()
    }

    /**
     * Selects a quest for detailed view.
     */
    fun selectQuest(quest: Quest?) {
        _selectedQuest.value = quest
    }

    /**
     * Toggles tracking for a quest.
     */
    fun toggleQuestTracking(questId: Long, isTracked: Boolean) {
        viewModelScope.launch {
            try {
                toggleQuestTrackingUseCase(questId, isTracked)
                // The flow collection will update the UI automatically
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update quest tracking: ${e.message}") }
            }
        }
    }

    /**
     * Updates the progress of a quest objective.
     */
    fun updateObjectiveProgress(questId: Long, objectiveIndex: Int, currentAmount: Int) {
        viewModelScope.launch {
            try {
                val updatedQuest = updateObjectiveProgressUseCase(questId, objectiveIndex, currentAmount)
                if (updatedQuest != null) {
                    // If this was the selected quest, update it
                    if (_selectedQuest.value?.id == questId) {
                        _selectedQuest.value = updatedQuest
                    }
                    
                    // Refresh statistics if the quest was completed
                    if (updatedQuest.isCompleted) {
                        loadQuestStatistics()
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update objective: ${e.message}") }
            }
        }
    }

    /**
     * Completes a quest.
     */
    fun completeQuest(questId: Long) {
        viewModelScope.launch {
            try {
                val updatedQuest = completeQuestUseCase(questId)
                if (updatedQuest != null) {
                    // If this was the selected quest, update it
                    if (_selectedQuest.value?.id == questId) {
                        _selectedQuest.value = updatedQuest
                    }
                    
                    // Refresh statistics
                    loadQuestStatistics()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to complete quest: ${e.message}") }
            }
        }
    }

    /**
     * Creates a new quest.
     */
    fun createQuest(
        name: String,
        description: String,
        objectives: List<QuestObjective>,
        rewards: QuestRewards,
        questType: QuestType,
        requiredLevel: Int = 1,
        unlockRequirements: String = "",
        locationHint: String = "",
        questGiver: String = "",
        expiryTime: LocalDateTime? = null,
        isTracked: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val newQuest = createQuestUseCase(
                    name = name,
                    description = description,
                    objectives = objectives,
                    rewards = rewards,
                    questType = questType,
                    requiredLevel = requiredLevel,
                    unlockRequirements = unlockRequirements,
                    locationHint = locationHint,
                    questGiver = questGiver,
                    expiryTime = expiryTime,
                    isTracked = isTracked
                )
                
                // Select the newly created quest
                _selectedQuest.value = newQuest
                
                // Refresh statistics
                loadQuestStatistics()
                
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to create quest: ${e.message}"
                    ) 
                }
            }
        }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}