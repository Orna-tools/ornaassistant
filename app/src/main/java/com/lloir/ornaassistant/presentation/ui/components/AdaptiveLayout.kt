package com.lloir.ornaassistant.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Screen size breakpoints for responsive design
 */
enum class ScreenSizeClass {
    COMPACT,    // Phones (< 600dp width)
    MEDIUM,     // Small tablets and foldables (600dp - 839dp width)
    EXPANDED,   // Tablets (840dp - 1199dp width)
    DESKTOP     // Large tablets and desktops (≥ 1200dp width)
}

/**
 * Determines the screen size class based on width
 */
@Composable
fun getScreenSizeClass(): ScreenSizeClass {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    return when {
        screenWidth < 600 -> ScreenSizeClass.COMPACT
        screenWidth < 840 -> ScreenSizeClass.MEDIUM
        screenWidth < 1200 -> ScreenSizeClass.EXPANDED
        else -> ScreenSizeClass.DESKTOP
    }
}

/**
 * Enhanced adaptive layout helper
 * Handles orientation and resizability changes for different screen sizes
 * with smooth animations for layout changes
 * 
 * @param modifier Modifier to be applied to the layout
 * @param useAdaptiveLayouts Whether to use adaptive layouts or a simple layout
 * @param content The content to be displayed in the layout
 */
@Composable
fun AdaptiveContainer(
    modifier: Modifier = Modifier,
    useAdaptiveLayouts: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    // If adaptive layouts are disabled, use a simple layout
    if (!useAdaptiveLayouts) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
        return
    }

    // Otherwise, use the adaptive layout based on screen size
    val screenSizeClass = getScreenSizeClass()
    val density = LocalDensity.current

    when (screenSizeClass) {
        ScreenSizeClass.COMPACT -> {
            // Compact layout (phones)
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
        ScreenSizeClass.MEDIUM -> {
            // Medium layout (small tablets and foldables)
            Row(
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(300)) +
                            slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                initialOffsetX = { with(density) { -100.dp.roundToPx() } }
                            ),
                    exit = fadeOut(animationSpec = tween(300)) +
                            slideOutHorizontally(
                                animationSpec = tween(300),
                                targetOffsetX = { with(density) { -100.dp.roundToPx() } }
                            )
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        content()
                    }
                }
            }
        }
        ScreenSizeClass.EXPANDED, ScreenSizeClass.DESKTOP -> {
            // Expanded layout (tablets and desktops)
            Row(
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(300)) +
                            slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                initialOffsetX = { with(density) { -100.dp.roundToPx() } }
                            ),
                    exit = fadeOut(animationSpec = tween(300)) +
                            slideOutHorizontally(
                                animationSpec = tween(300),
                                targetOffsetX = { with(density) { -100.dp.roundToPx() } }
                            )
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * Enhanced responsive spacing based on screen size
 * 
 * @param useAdaptiveLayouts Whether to use adaptive layouts or a simple layout
 */
@Composable
fun adaptiveSpacing(useAdaptiveLayouts: Boolean = true): PaddingValues {
    // If adaptive layouts are disabled, use a simple padding
    if (!useAdaptiveLayouts) {
        return PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    }

    val screenSizeClass = getScreenSizeClass()

    return when (screenSizeClass) {
        ScreenSizeClass.COMPACT -> PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ScreenSizeClass.MEDIUM -> PaddingValues(horizontal = 24.dp, vertical = 20.dp)
        ScreenSizeClass.EXPANDED -> PaddingValues(horizontal = 32.dp, vertical = 24.dp)
        ScreenSizeClass.DESKTOP -> PaddingValues(horizontal = 48.dp, vertical = 32.dp)
    }
}

/**
 * Responsive size helper for UI elements
 * 
 * @param compactSize The size to use for compact screens (phones)
 * @param mediumSize The size to use for medium screens (small tablets and foldables)
 * @param expandedSize The size to use for expanded screens (tablets)
 * @param desktopSize The size to use for desktop screens (large tablets and desktops)
 * @param useAdaptiveLayouts Whether to use adaptive layouts or a simple layout
 */
@Composable
fun adaptiveSize(
    compactSize: Dp,
    mediumSize: Dp = compactSize * 1.25f,
    expandedSize: Dp = compactSize * 1.5f,
    desktopSize: Dp = compactSize * 1.75f,
    useAdaptiveLayouts: Boolean = true
): Dp {
    // If adaptive layouts are disabled, use the compact size
    if (!useAdaptiveLayouts) {
        return compactSize
    }

    val screenSizeClass = getScreenSizeClass()

    return when (screenSizeClass) {
        ScreenSizeClass.COMPACT -> compactSize
        ScreenSizeClass.MEDIUM -> mediumSize
        ScreenSizeClass.EXPANDED -> expandedSize
        ScreenSizeClass.DESKTOP -> desktopSize
    }
}

/**
 * Check if device is in landscape orientation
 */
@Composable
fun isLandscape(): Boolean = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
