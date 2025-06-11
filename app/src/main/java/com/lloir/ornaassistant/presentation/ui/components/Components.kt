package com.lloir.ornaassistant.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lloir.ornaassistant.domain.model.DungeonStatistics
import com.lloir.ornaassistant.presentation.viewmodel.ChartData
import com.lloir.ornaassistant.presentation.viewmodel.PermissionStatus

@Composable
fun StatisticsCard(
    statistics: DungeonStatistics,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Statistics (Last 7 Days)",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    title = "Total Visits",
                    value = statistics.totalVisits.toString(),
                    icon = Icons.Default.Assignment
                )

                StatisticItem(
                    title = "Completed",
                    value = statistics.completedVisits.toString(),
                    icon = Icons.Default.CheckCircle
                )

                StatisticItem(
                    title = "Failed",
                    value = statistics.failedVisits.toString(),
                    icon = Icons.Default.Cancel
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    title = "Total Orns",
                    value = formatNumber(statistics.totalOrns),
                    icon = Icons.Default.MonetizationOn
                )

                StatisticItem(
                    title = "Completion Rate",
                    value = "${(statistics.completionRate * 100).toInt()}%",
                    icon = Icons.Default.TrendingUp
                )

                StatisticItem(
                    title = "Favorite Mode",
                    value = statistics.favoriteMode.name,
                    icon = Icons.Default.Star
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PermissionCard(
    permissionStatus: PermissionStatus,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, description, buttonText, icon, colors, action) = when (permissionStatus) {
        PermissionStatus.GRANTED -> {
            Tuple6(
                "Permissions Granted",
                "All required permissions are granted. The app is ready to use!",
                null,
                Icons.Default.CheckCircle,
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                null
            )
        }
        PermissionStatus.NOT_GRANTED -> {
            Tuple6(
                "Permissions Required",
                "This app requires accessibility and overlay permissions to function properly. Grant both permissions to continue.",
                "Grant Accessibility",
                Icons.Default.Security,
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                { onRequestAccessibilityPermission() }
            )
        }
        PermissionStatus.DENIED -> {
            Tuple6(
                "Permissions Denied",
                "Some permissions were denied. Please enable them in Settings > Accessibility.",
                "Open Settings",
                Icons.Default.Warning,
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                { onRequestAccessibilityPermission() }
            )
        }
    }

    Card(
        modifier = modifier,
        colors = colors
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (buttonText != null && action != null) {
                    Button(
                        onClick = action,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(buttonText)
                    }
                    
                    // Add overlay permission button if needed
                    if (permissionStatus == PermissionStatus.NOT_GRANTED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRequestOverlayPermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Overlay Permission")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyChart(
    chartData: ChartData,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier.fillMaxWidth()) {
        drawWeeklyBarChart(
            chartData = chartData,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            tertiaryColor = tertiaryColor
        )
    }
}

private fun DrawScope.drawWeeklyBarChart(
    chartData: ChartData,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color
) {
    val barWidth = size.width / (chartData.days.size * 3 + 1) // 3 bars per day + spacing
    val maxVisits = chartData.visits.maxOrNull() ?: 1
    val maxOrns = chartData.orns.maxOrNull() ?: 1L

    chartData.days.forEachIndexed { index, _ ->
        val x = barWidth * (index * 3 + 1)

        // Visits bar
        val visitsHeight = (chartData.visits[index].toFloat() / maxVisits) * size.height * 0.8f
        drawRect(
            color = primaryColor,
            topLeft = Offset(x, size.height - visitsHeight),
            size = androidx.compose.ui.geometry.Size(barWidth * 0.8f, visitsHeight)
        )

        // Orns bar (scaled down)
        val ornsHeight = (chartData.orns[index].toFloat() / maxOrns) * size.height * 0.4f
        drawRect(
            color = secondaryColor,
            topLeft = Offset(x + barWidth, size.height - ornsHeight),
            size = androidx.compose.ui.geometry.Size(barWidth * 0.8f, ornsHeight)
        )
    }
}

// Helper functions
private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> "%.1fM".format(number / 1_000_000.0)
        number >= 1_000 -> "%.1fK".format(number / 1_000.0)
        else -> number.toString()
    }
}

// Helper data class for pattern matching
private data class Tuple6<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)