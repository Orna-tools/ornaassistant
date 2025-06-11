package com.lloir.ornaassistant.presentation.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.domain.model.DungeonVisit
import com.lloir.ornaassistant.presentation.viewmodel.DungeonHistoryViewModel
import com.lloir.ornaassistant.presentation.viewmodel.TimeRange
import com.lloir.ornaassistant.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DungeonHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: DungeonHistoryViewModel = hiltViewModel()
) {
    val filteredVisits by viewModel.filteredVisits.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dungeon History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    
                    IconButton(
                        onClick = viewModel::exportDungeons,
                        enabled = !isExporting
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Download, contentDescription = "Export CSV")
                        }
                    }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        TimeRange.values().forEach { timeRange ->
                            DropdownMenuItem(
                                text = { Text(timeRange.displayName) },
                                onClick = {
                                    viewModel.selectTimeRange(timeRange)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    if (selectedTimeRange == timeRange) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (filteredVisits.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No dungeon visits found",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Start playing Orna to see your dungeon history here",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredVisits) { visit ->
                    DungeonVisitCard(
                        visit = visit,
                        onDeleteClick = { viewModel.deleteVisit(visit) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DungeonVisitCard(
    visit: DungeonVisit,
    onDeleteClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = visit.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${visit.mode} • ${DateTimeUtils.formatFullDateTime(visit.startTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (visit.completed) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = "Failed",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Floor",
                    value = visit.floor.toString(),
                    icon = Icons.Default.Layers
                )

                StatItem(
                    label = "Orns",
                    value = formatNumber(visit.orns),
                    icon = Icons.Default.MonetizationOn
                )

                StatItem(
                    label = "Gold",
                    value = formatNumber(visit.gold),
                    icon = Icons.Default.AttachMoney
                )

                StatItem(
                    label = "XP",
                    value = formatNumber(visit.experience),
                    icon = Icons.Default.TrendingUp
                )
            }

            if (visit.durationSeconds > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Duration: ${DateTimeUtils.formatDuration(visit.startTime, visit.startTime.plusSeconds(visit.durationSeconds))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Visit") },
            text = { Text("Are you sure you want to delete this dungeon visit? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> "%.1fM".format(number / 1_000_000.0)
        number >= 1_000 -> "%.1fK".format(number / 1_000.0)
        else -> number.toString()
    }
}

// Extension for TimeRange display names
private val TimeRange.displayName: String
    get() = when (this) {
        TimeRange.DAY -> "Last 24 Hours"
        TimeRange.WEEK -> "Last Week"
        TimeRange.MONTH -> "Last Month"
        TimeRange.ALL -> "All Time"
    }