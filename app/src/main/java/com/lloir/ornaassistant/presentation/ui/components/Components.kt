package com.lloir.ornaassistant.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
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

/**
 * Enhanced weekly chart component with animations, labels, and better visualization.
 * 
 * Features:
 * - Animated bar transitions
 * - Day labels and value tooltips
 * - Gradient fills for bars
 * - Grid lines for better readability
 * - Accessibility improvements
 */
@Composable
fun WeeklyChart(
    chartData: ChartData,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    // Animation states for each bar
    val animatedVisitValues = remember(chartData.visits) {
        chartData.visits.map { Animatable(0f) }
    }

    val animatedOrnValues = remember(chartData.orns) {
        chartData.orns.map { Animatable(0f) }
    }

    // Animate the bars when data changes
    LaunchedEffect(chartData) {
        chartData.visits.forEachIndexed { index, value ->
            launch {
                animatedVisitValues[index].animateTo(
                    targetValue = value.toFloat(),
                    animationSpec = tween(
                        durationMillis = 1000,
                        easing = FastOutSlowInEasing,
                        delayMillis = index * 50
                    )
                )
            }
        }

        chartData.orns.forEachIndexed { index, value ->
            launch {
                animatedOrnValues[index].animateTo(
                    targetValue = value.toFloat(),
                    animationSpec = tween(
                        durationMillis = 1000,
                        easing = FastOutSlowInEasing,
                        delayMillis = index * 50 + 200
                    )
                )
            }
        }
    }

    // Chart container with padding for labels
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = 24.dp,
                bottom = 32.dp,
                start = 16.dp,
                end = 16.dp
            )
    ) {
        // Chart title and legend
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Weekly Activity",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem(
                    color = primaryColor,
                    label = "Dungeon Visits"
                )

                LegendItem(
                    color = secondaryColor,
                    label = "Orns Earned"
                )
            }
        }

        // Main chart canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(top = 60.dp) // Space for title and legend
                .semantics {
                    contentDescription = "Weekly chart showing dungeon visits and orns earned over the past 7 days"
                }
        ) {
            val maxVisits = chartData.visits.maxOrNull()?.toFloat() ?: 1f
            val maxOrns = chartData.orns.maxOrNull()?.toFloat() ?: 1f

            val chartWidth = size.width
            val chartHeight = size.height
            val barSpacing = 12f
            val barWidth = (chartWidth - (chartData.days.size - 1) * barSpacing) / (chartData.days.size * 2)

            // Draw horizontal grid lines
            val gridLineCount = 5
            val gridLineSpacing = chartHeight / gridLineCount

            repeat(gridLineCount + 1) { i ->
                val y = chartHeight - (i * gridLineSpacing)
                drawLine(
                    color = outlineVariantColor.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1f
                )

                // Draw grid line labels (values)
                if (i > 0) {
                    val visitValue = (maxVisits * i / gridLineCount).toInt()
                    drawContext.canvas.nativeCanvas.drawText(
                        visitValue.toString(),
                        8f,
                        y - 8f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor(
                                onSurfaceColor.copy(alpha = 0.7f).toArgb().toHexString()
                            )
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                    )
                }
            }

            // Draw bars for each day
            chartData.days.forEachIndexed { index, day ->
                val x = index * (barWidth * 2 + barSpacing)

                // Visits bar with gradient
                val visitValue = animatedVisitValues[index].value
                val visitHeight = (visitValue / maxVisits) * chartHeight

                // Draw visit bar with gradient
                // Ensure minimum height for gradient to avoid IllegalArgumentException
                val safeVisitHeight = maxOf(visitHeight, 2f)  // Minimum 2f to ensure startY and endY are different

                // Only draw if we have valid values
                if (barWidth > 0 && safeVisitHeight > 0) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.8f),
                                primaryColor.copy(alpha = 0.4f)
                            ),
                            startY = chartHeight - safeVisitHeight,
                            endY = chartHeight
                        ),
                        topLeft = Offset(x, chartHeight - safeVisitHeight),
                        size = Size(barWidth, safeVisitHeight),
                        alpha = 0.9f
                    )
                }

                // Orns bar with gradient
                val ornValue = animatedOrnValues[index].value
                val ornHeight = (ornValue / maxOrns) * chartHeight

                // Ensure minimum height for gradient to avoid IllegalArgumentException
                val safeOrnHeight = maxOf(ornHeight, 2f)  // Minimum 2f to ensure startY and endY are different

                // Only draw if we have valid values
                if (barWidth > 0 && safeOrnHeight > 0) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                secondaryColor.copy(alpha = 0.8f),
                                secondaryColor.copy(alpha = 0.4f)
                            ),
                            startY = chartHeight - safeOrnHeight,
                            endY = chartHeight
                        ),
                        topLeft = Offset(x + barWidth, chartHeight - safeOrnHeight),
                        size = Size(barWidth, safeOrnHeight),
                        alpha = 0.9f
                    )
                }

                // Draw day label
                drawContext.canvas.nativeCanvas.drawText(
                    day,
                    x + barWidth,
                    chartHeight + 16f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor(
                            onSurfaceColor.toArgb().toHexString()
                        )
                        textSize = 11.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )

                // Draw value labels above bars
                if (visitValue > 0 && safeVisitHeight > 2f) {  // Only draw label if bar is tall enough
                    // Ensure text is drawn at a valid position
                    val textY = maxOf(chartHeight - safeVisitHeight - 8f, 10f)  // Ensure text is not drawn too high
                    drawContext.canvas.nativeCanvas.drawText(
                        visitValue.toInt().toString(),
                        x + barWidth / 2,
                        textY,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor(
                                primaryColor.toArgb().toHexString()
                            )
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }

                if (ornValue > 0 && safeOrnHeight > 2f) {  // Only draw label if bar is tall enough
                    val ornText = formatCompactNumber(ornValue.toLong())
                    // Ensure text is drawn at a valid position
                    val textY = maxOf(chartHeight - safeOrnHeight - 8f, 10f)  // Ensure text is not drawn too high
                    drawContext.canvas.nativeCanvas.drawText(
                        ornText,
                        x + barWidth * 1.5f,
                        textY,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor(
                                secondaryColor.toArgb().toHexString()
                            )
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

/**
 * Legend item for the chart
 */
@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * Convert Color to hex string for Canvas text
 * Safely handles any integer value
 */
private fun Int.toHexString(): String {
    return try {
        String.format("#%08X", this)
    } catch (e: Exception) {
        // Fallback to a safe default color if there's any issue
        "#FF000000"  // Black color as fallback
    }
}

/**
 * Format large numbers in a compact way (K, M, etc.)
 * Safely handles any numeric value
 */
private fun formatCompactNumber(number: Long): String {
    return try {
        when {
            number >= 1_000_000 -> "${(number / 100_000) / 10.0}M"
            number >= 1_000 -> "${(number / 100) / 10.0}K"
            else -> number.toString()
        }
    } catch (e: Exception) {
        // Return a safe default if there's any arithmetic issue
        "0"
    }
}

// Helper functions
/**
 * Format numbers with K/M suffixes for better readability
 * Safely handles any numeric value
 */
private fun formatNumber(number: Long): String {
    return try {
        when {
            number >= 1_000_000 -> "%.1fM".format(number / 1_000_000.0)
            number >= 1_000 -> "%.1fK".format(number / 1_000.0)
            else -> number.toString()
        }
    } catch (e: Exception) {
        // Return a safe default if there's any formatting issue
        "0"
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
