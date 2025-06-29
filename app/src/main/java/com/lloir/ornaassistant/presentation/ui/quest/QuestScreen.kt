package com.lloir.ornaassistant.presentation.ui.quest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.domain.model.Quest
import com.lloir.ornaassistant.domain.model.QuestType
import com.lloir.ornaassistant.presentation.ui.components.AdaptiveContainer
import com.lloir.ornaassistant.presentation.viewmodel.QuestViewModel

/**
 * Main screen for displaying and managing quests.
 * 
 * This screen provides:
 * - A list of quests with filtering options
 * - Quest statistics
 * - Actions for tracking and completing quests
 * - A search function for finding specific quests
 * 
 * It uses adaptive layout for better large screen support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestScreen(
    onNavigateBack: () -> Unit,
    onCreateQuest: () -> Unit,
    questViewModel: QuestViewModel = hiltViewModel()
) {
    val uiState by questViewModel.uiState.collectAsState()
    val filteredQuests by questViewModel.filteredQuests.collectAsState()
    val selectedQuest by questViewModel.selectedQuest.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quest Tracker") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    IconButton(onClick = onCreateQuest) {
                        Icon(Icons.Default.Add, contentDescription = "Add Quest")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateQuest) {
                Icon(Icons.Default.Add, contentDescription = "Add Quest")
            }
        }
    ) { paddingValues ->
        // Android 16: Use adaptive layout for better large screen support
        AdaptiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Content automatically adapts to screen size using AdaptiveContainer
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Search bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { questViewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search quests...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { questViewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quest type chips
                QuestTypeChips(
                    selectedType = uiState.selectedQuestType,
                    onTypeSelected = { questViewModel.setQuestTypeFilter(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Show completed quests switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show completed quests")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = uiState.showCompletedQuests,
                        onCheckedChange = { questViewModel.setShowCompletedQuests(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quest list
                if (filteredQuests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isEmpty() && uiState.selectedQuestType == null)
                                "No quests found. Create your first quest!"
                            else
                                "No quests match your filters.",
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredQuests) { quest ->
                            QuestCard(
                                quest = quest,
                                onQuestClick = { questViewModel.selectQuest(quest) },
                                onTrackingToggle = { isTracked ->
                                    questViewModel.toggleQuestTracking(quest.id, isTracked)
                                },
                                onCompleteQuest = {
                                    questViewModel.completeQuest(quest.id)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Statistics card
                uiState.questStatistics?.let { stats ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Quest Statistics",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Active: ${stats.activeQuestCount}")
                                    Text("Completed: ${stats.completedQuestCount}")
                                    Text("Total: ${stats.totalQuestCount}")
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Main: ${stats.mainQuestCount}")
                                    Text("Side: ${stats.sideQuestCount}")
                                    Text("Daily: ${stats.dailyQuestCount}")
                                }
                            }
                        }
                    }
                }
            }

            // Selected quest detail dialog
            selectedQuest?.let { quest ->
                QuestDetailDialog(
                    quest = quest,
                    onDismiss = { questViewModel.selectQuest(null) },
                    onTrackingToggle = { isTracked ->
                        questViewModel.toggleQuestTracking(quest.id, isTracked)
                    },
                    onCompleteQuest = {
                        questViewModel.completeQuest(quest.id)
                        questViewModel.selectQuest(null)
                    },
                    onUpdateObjective = { index, amount ->
                        questViewModel.updateObjectiveProgress(quest.id, index, amount)
                    }
                )
            }

            // Filter dialog
            if (showFilterDialog) {
                QuestFilterDialog(
                    selectedType = uiState.selectedQuestType,
                    showCompleted = uiState.showCompletedQuests,
                    onTypeSelected = { questViewModel.setQuestTypeFilter(it) },
                    onShowCompletedChanged = { questViewModel.setShowCompletedQuests(it) },
                    onDismiss = { showFilterDialog = false }
                )
            }

            // Error handling
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = questViewModel::clearError) {
                            Text("DISMISS")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/**
 * Horizontal scrollable row of chips for filtering quests by type.
 */
@Composable
fun QuestTypeChips(
    selectedType: QuestType?,
    onTypeSelected: (QuestType?) -> Unit
) {
    val types = QuestType.values()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        FilterChip(
            selected = selectedType == null,
            onClick = { onTypeSelected(null) },
            label = { Text("All") },
            leadingIcon = {
                if (selectedType == null) {
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        types.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type.name.lowercase().capitalize()) },
                leadingIcon = {
                    if (selectedType == type) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

/**
 * Dialog for filtering quests.
 */
@Composable
fun QuestFilterDialog(
    selectedType: QuestType?,
    showCompleted: Boolean,
    onTypeSelected: (QuestType?) -> Unit,
    onShowCompletedChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Quests") },
        text = {
            Column {
                Text("Quest Type")
                Spacer(modifier = Modifier.height(8.dp))

                QuestTypeChips(
                    selectedType = selectedType,
                    onTypeSelected = onTypeSelected
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show completed quests")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = showCompleted,
                        onCheckedChange = onShowCompletedChanged
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

// Extension function to capitalize first letter
private fun String.capitalize(): String {
    return this.replaceFirstChar { it.uppercase() }
}
