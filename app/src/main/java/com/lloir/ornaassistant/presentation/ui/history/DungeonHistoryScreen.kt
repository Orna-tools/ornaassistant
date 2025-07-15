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
import android.util.Log
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.domain.model.FloorReward
import com.lloir.ornaassistant.domain.model.DungeonVisit
import com.lloir.ornaassistant.presentation.viewmodel.DungeonHistoryViewModel
import com.lloir.ornaassistant.presentation.viewmodel.TimeRange
import com.lloir.ornaassistant.presentation.ui.settings.SettingsViewModel
import com.lloir.ornaassistant.presentation.ui.components.AdaptiveContainer
import com.lloir.ornaassistant.presentation.ui.components.FeatureTutorialCard
import com.lloir.ornaassistant.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DungeonHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: DungeonHistoryViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val filteredVisits by viewModel.filteredVisits.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasMoreData by viewModel.hasMoreData.collectAsState()
    val totalItems by viewModel.totalItems.collectAsState()

    LaunchedEffect(filteredVisits) {
        Log.d("DungeonHistoryScreen", "Displaying ${filteredVisits.size} visits out of $totalItems total")
    }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Tutorial card for new users
                if (settings?.hasCompletedTutorial == false || settings?.showFeatureTutorials == true) {
                    item {
                        var showDungeonHistoryTutorial by remember { mutableStateOf(true) }

                        if (showDungeonHistoryTutorial) {
                            FeatureTutorialCard(
                                title = "Dungeon History",
                                description = "Track your dungeon runs over time. See statistics for each dungeon, including orns, gold, and experience earned. Filter by time period using the dropdown menu.",
                                icon = Icons.Default.History,
                                onDismiss = { showDungeonHistoryTutorial = false }
                            )
                        }
                    }
                }

                // Show total count if we have items
                if (totalItems > 0) {
                    item {
                        Text(
                            text = "Showing ${filteredVisits.size} of $totalItems visits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                if (filteredVisits.isEmpty() && !isLoading) {
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

                    // Load more indicator
                    if (hasMoreData || isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp)
                                    )
                                } else {
                                    Button(
                                        onClick = { viewModel.loadNextPage() },
                                        modifier = Modifier.fillMaxWidth(0.7f)
                                    ) {
                                        Text("Load More")
                                    }
                                }
                            }

                            // Trigger loading more data when this item becomes visible
                            LaunchedEffect(Unit) {
                                if (!isLoading && hasMoreData) {
                                    viewModel.loadNextPage()
                                }
                            }
                        }
                    }
                }
            }

            // Refresh button
            if (isLoading && filteredVisits.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
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
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(visit) {
        Log.d("DungeonVisitCard", "Rendering visit: ${visit.name}")
        Log.d("DungeonVisitCard", "  - Total rewards: orns=${visit.orns}, gold=${visit.gold}, exp=${visit.experience}")
        Log.d("DungeonVisitCard", "  - Floor rewards count: ${visit.floorRewards.size}")
    }

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

            // Show floor breakdown if available
            if (visit.floorRewards.isNotEmpty()) {
                // Expandable floor breakdown
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (expanded) "Hide Floor Details" else "Show Floor Details",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (expanded) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            visit.floorRewards.sortedBy { it.floor }.forEach { floorReward ->
                                FloorRewardRow(floorReward)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Total Stats Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Total Rewards",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
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
                }
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
private fun FloorRewardRow(floorReward: FloorReward) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Floor ${floorReward.floor}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${formatNumber(floorReward.orns)} orns",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${formatNumber(floorReward.gold)} gold",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${formatNumber(floorReward.experience)} xp",
                style = MaterialTheme.typography.bodySmall
            )
        }
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
