package com.lloir.ornaassistant.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Data class for weekly chart data
 */
data class WeeklyChartData(
    val days: List<String>,
    val values: List<Float>,
    val maxValue: Float = values.maxOrNull() ?: 0f,
    val valueFormatter: (Float) -> String = { it.roundToInt().toString() }
)

/**
 * A modern, animated weekly chart component with gradient bars, grid lines, and tooltips
 */
@Composable
fun WeeklyChart(
    data: WeeklyChartData,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    barWidth: Float = 0.6f, // 0.0 to 1.0, percentage of available width
    showGridLines: Boolean = true,
    gridLineColor: Color = MaterialTheme.colorScheme.outlineVariant,
    animationDuration: Int = 1000
) {
    // State for animations and interactions
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    val animatedValues = remember(data.values) {
        data.values.map { Animatable(0f) }
    }

    // Animation effect
    LaunchedEffect(data.values) {
        data.values.forEachIndexed { index, targetValue ->
            animatedValues[index].animateTo(
                targetValue = targetValue / data.maxValue,
                animationSpec = tween(
                    durationMillis = animationDuration,
                    easing = FastOutSlowInEasing,
                    delayMillis = index * 50 // Staggered animation
                )
            )
        }
    }

    // Create gradient for bars
    val barGradient = remember(barColor) {
        Brush.verticalGradient(
            colors = listOf(
                barColor,
                barColor.copy(alpha = 0.7f)
            )
        )
    }

    // Text style for labels
    val labelTextStyle = MaterialTheme.typography.bodySmall
    val labelTextStylePx = with(LocalDensity.current) {
        TextStyle(
            fontSize = labelTextStyle.fontSize.toPx().sp,
            fontWeight = labelTextStyle.fontWeight,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    // Text style for values
    val valueTextStyle = MaterialTheme.typography.bodySmall.copy(
        fontWeight = FontWeight.Medium
    )
    val valueTextStylePx = with(LocalDensity.current) {
        TextStyle(
            fontSize = valueTextStyle.fontSize.toPx().sp,
            fontWeight = valueTextStyle.fontWeight,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    // Text measurer for calculating text dimensions
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 24.dp,
                bottom = 16.dp
            )
    ) {
        // Chart title
        Text(
            text = "Weekly Activity",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(bottom = 16.dp)
        )

        // Chart canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp) // Space for title
                .pointerInput(data.values) {
                    detectTapGestures { offset ->
                        // Calculate which bar was tapped
                        val barCount = data.values.size
                        val canvasWidth = size.width
                        val barSpacing = canvasWidth / barCount
                        val tappedIndex = (offset.x / barSpacing).toInt()

                        if (tappedIndex in data.values.indices) {
                            selectedBarIndex = if (selectedBarIndex == tappedIndex) null else tappedIndex
                        } else {
                            selectedBarIndex = null
                        }
                    }
                }
        ) {
            val canvasHeight = size.height
            val canvasWidth = size.width
            val barCount = data.values.size

            // Available height for bars (excluding space for labels)
            val availableHeight = canvasHeight - 40f

            // Calculate bar width and spacing
            val totalBarWidth = canvasWidth / barCount
            val actualBarWidth = totalBarWidth * barWidth
            val barSpacing = (totalBarWidth - actualBarWidth) / 2

            // Draw grid lines
            if (showGridLines) {
                val gridLineCount = 5
                val gridLineSpacing = availableHeight / gridLineCount

                for (i in 0..gridLineCount) {
                    val y = availableHeight - (i * gridLineSpacing)

                    // Draw grid line
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                    )

                    // Draw grid value (only for non-zero values)
                    if (i > 0) {
                        val gridValue = (i.toFloat() / gridLineCount) * data.maxValue
                        val gridValueText = data.valueFormatter(gridValue)

                        val textLayoutResult = textMeasurer.measure(
                            text = AnnotatedString(gridValueText),
                            style = valueTextStylePx.copy(
                                color = gridLineColor,
                                fontSize = (valueTextStylePx.fontSize.value * 0.8f).sp
                            )
                        )

                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(
                                0f,
                                y - textLayoutResult.size.height - 2.dp.toPx()
                            )
                        )
                    }
                }
            }

            // Draw bars and labels
            data.values.forEachIndexed { index, value ->
                val animatedValue = animatedValues[index].value
                val barHeight = animatedValue * availableHeight

                val isSelected = selectedBarIndex == index
                val barAlpha = if (selectedBarIndex != null && !isSelected) 0.5f else 1f

                // Bar position
                val barLeft = index * totalBarWidth + barSpacing
                val barTop = availableHeight - barHeight
                val barRight = barLeft + actualBarWidth
                val barBottom = availableHeight

                // Draw bar with rounded corners at the top
                drawRoundRect(
                    brush = barGradient,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(actualBarWidth, barHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    alpha = barAlpha
                )

                // Draw bar outline for selected bar
                if (isSelected) {
                    drawRoundRect(
                        color = barColor.copy(alpha = 0.8f),
                        topLeft = Offset(barLeft, barTop),
                        size = Size(actualBarWidth, barHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Draw day label
                val dayLabel = data.days[index]
                val dayLabelResult = textMeasurer.measure(
                    text = AnnotatedString(dayLabel),
                    style = labelTextStylePx
                )

                drawText(
                    textLayoutResult = dayLabelResult,
                    topLeft = Offset(
                        barLeft + (actualBarWidth - dayLabelResult.size.width) / 2,
                        availableHeight + 8.dp.toPx()
                    )
                )

                // Draw value tooltip for selected bar
                if (isSelected && barHeight > 0) {
                    val tooltipText = data.valueFormatter(value)
                    val tooltipResult = textMeasurer.measure(
                        text = AnnotatedString(tooltipText),
                        style = valueTextStylePx.copy(
                            textAlign = TextAlign.Center
                        )
                    )

                    val tooltipWidth = tooltipResult.size.width + 16.dp.toPx()
                    val tooltipHeight = tooltipResult.size.height + 8.dp.toPx()
                    val tooltipX = barLeft + (actualBarWidth - tooltipWidth) / 2
                    val tooltipY = barTop - tooltipHeight - 4.dp.toPx()

                    // Draw tooltip background
                    drawRoundRect(
                        color = gridLineColor.copy(alpha = 0.3f),
                        topLeft = Offset(tooltipX, tooltipY),
                        size = Size(tooltipWidth, tooltipHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Draw tooltip text
                    drawText(
                        textLayoutResult = tooltipResult,
                        topLeft = Offset(
                            tooltipX + (tooltipWidth - tooltipResult.size.width) / 2,
                            tooltipY + (tooltipHeight - tooltipResult.size.height) / 2
                        )
                    )

                    // Draw tooltip pointer
                    val pointerPath = Path().apply {
                        moveTo(barLeft + actualBarWidth / 2 - 4.dp.toPx(), tooltipY + tooltipHeight)
                        lineTo(barLeft + actualBarWidth / 2 + 4.dp.toPx(), tooltipY + tooltipHeight)
                        lineTo(barLeft + actualBarWidth / 2, tooltipY + tooltipHeight + 4.dp.toPx())
                        close()
                    }

                    drawPath(
                        path = pointerPath,
                        color = gridLineColor.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeeklyChartPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            WeeklyChartData(
                days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                values = listOf(120f, 270f, 180f, 90f, 310f, 240f, 180f),
                valueFormatter = { "${it.roundToInt()} orns" }
            ).let { data ->
                WeeklyChart(
                    data = data,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(16.dp)
                )
            }
        }
    }
}
