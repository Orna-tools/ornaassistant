package com.lloir.ornaassistant.presentation.ui.drops

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.presentation.viewmodel.DropsStatisticsViewModel
import com.lloir.ornaassistant.presentation.viewmodel.DropsStatistics
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropsStatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DropsStatisticsViewModel = hiltViewModel()
) {
    val stats by viewModel.dropsStats.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drops Statistics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::exportAssessments,
                        enabled = !isExporting
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Download, contentDescription = "Export")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DropsOverviewCard(stats)
            }
            
            item {
                TimeTrackerCard(
                    title = "Last Ornate",
                    lastDropTime = stats.lastOrnateTime,
                    totalCount = stats.ornateCount,
                    icon = Icons.Default.StarOutline,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            item {
                TimeTrackerCard(
                    title = "Last Godforge",
                    lastDropTime = stats.lastGodforgeTime,
                    totalCount = stats.godforgeCount,
                    icon = Icons.Default.Star,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                DropRateCard(stats)
            }
        }
    }
}

@Composable
private fun DropsOverviewCard(stats: DropsStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total Assessments",
                    value = stats.totalAssessments.toString(),
                    icon = Icons.Default.Assessment
                )
                
                StatItem(
                    label = "Ornates",
                    value = stats.ornateCount.toString(),
                    icon = Icons.Default.StarOutline
                )
                
                StatItem(
                    label = "Godforges",
                    value = stats.godforgeCount.toString(),
                    icon = Icons.Default.Star
                )
            }
        }
    }
}

@Composable
private fun TimeTrackerCard(
    title: String,
    lastDropTime: LocalDateTime?,
    totalCount: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = color
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (lastDropTime != null) {
                val timeSince = formatTimeSince(lastDropTime)
                Text(
                    text = timeSince,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "since last drop",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "No drops yet",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Total: $totalCount",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun DropRateCard(stats: DropsStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Drop Rates",
                style = MaterialTheme.typography.titleMedium
            )
            
            DropRateRow(
                label = "Ornate Rate",
                rate = if (stats.totalAssessments > 0) {
                    (stats.ornateCount.toFloat() / stats.totalAssessments) * 100
                } else 0f
            )
            
            DropRateRow(
                label = "Godforge Rate",
                rate = if (stats.totalAssessments > 0) {
                    (stats.godforgeCount.toFloat() / stats.totalAssessments) * 100
                } else 0f
            )
        }
    }
}

@Composable
private fun DropRateRow(label: String, rate: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Text(
            text = "${String.format("%.2f", rate)}%",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
    
    LinearProgressIndicator(
        progress = rate / 100f,
        modifier = Modifier.fillMaxWidth()
    )
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
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTimeSince(time: LocalDateTime): String {
    val now = LocalDateTime.now()
    val minutes = ChronoUnit.MINUTES.between(time, now)
    val hours = ChronoUnit.HOURS.between(time, now)
    val days = ChronoUnit.DAYS.between(time, now)
    val weeks = ChronoUnit.WEEKS.between(time, now)


    return when {
        weeks > 0 -> {
            val remainingDays = ChronoUnit.DAYS.between(time.plusWeeks(weeks), now)
            if (remainingDays > 0) {
                "$weeks week${if (weeks > 1) "s" else ""}, $remainingDays day${if (remainingDays > 1) "s" else ""} ago"
            } else {
                "$weeks week${if (weeks > 1) "s" else ""} ago"
            }
        }

        days > 0 -> {
            val remainingHours = ChronoUnit.HOURS.between(time.plusDays(days), now)
            if (remainingHours > 0) {
                "$days day${if (days > 1) "s" else ""}, $remainingHours hour${if (remainingHours > 1) "s" else ""} ago"
            } else {
                "$days day${if (days > 1) "s" else ""} ago"
            }
        }

        hours > 0 -> {
            val remainingMinutes = ChronoUnit.MINUTES.between(time.plusHours(hours), now)
            if (remainingMinutes > 0) {
                "$hours hour${if (hours > 1) "s" else ""}, $remainingMinutes minute${if (remainingMinutes > 1) "s" else ""} ago"
            } else {
                "$hours hour${if (hours > 1) "s" else ""} ago"
            }
        }

        minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
        else -> "Just now"
    }
}