package com.lloir.ornaassistant.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
 * Enhanced adaptive layout helper for Android 16+
 * 
 * Features:
 * - Responsive layouts for different screen sizes
 * - Smooth animations when layout changes
 * - Support for different content arrangements based on screen size
 * - Optimized for tablets, foldables, and desktop environments
 */
@Composable
fun AdaptiveContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // More granular screen size classification
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    // Define screen size breakpoints
    val isCompact = screenWidth < 600
    val isMedium = screenWidth in 600..839
    val isExpanded = screenWidth >= 840
    val isLandscape = screenWidth > screenHeight

    // Calculate appropriate spacing based on screen size
    val horizontalSpacing = remember(screenWidth) {
        when {
            isExpanded -> 32.dp
            isMedium -> 24.dp
            else -> 16.dp
        }
    }

    val verticalSpacing = remember(screenWidth) {
        when {
            isExpanded -> 24.dp
            isMedium -> 20.dp
            else -> 16.dp
        }
    }

    // Apply different layouts based on screen size
    if (isCompact) {
        // Compact layout (phones)
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(spring(stiffness = Spring.StiffnessLow)),
            exit = fadeOut(spring(stiffness = Spring.StiffnessLow))
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalSpacing, vertical = verticalSpacing / 2),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing)
            ) {
                content()
            }
        }
    } else {
        // Medium to large screen layout (tablets, foldables, desktop)
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(spring(stiffness = Spring.StiffnessLow)),
            exit = fadeOut(spring(stiffness = Spring.StiffnessLow))
        ) {
            Row(
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            horizontal = horizontalSpacing, 
                            vertical = verticalSpacing / 2
                        ),
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Enhanced responsive spacing based on screen size and orientation
 * 
 * @return PaddingValues appropriate for the current screen configuration
 */
@Composable
fun adaptiveSpacing(): PaddingValues {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    return when {
        screenWidth >= 840 -> PaddingValues(
            horizontal = 32.dp, 
            vertical = if (isLandscape) 24.dp else 28.dp
        )
        screenWidth >= 600 -> PaddingValues(
            horizontal = 24.dp, 
            vertical = if (isLandscape) 20.dp else 24.dp
        )
        else -> PaddingValues(
            horizontal = 16.dp, 
            vertical = 16.dp
        )
    }
}

/**
 * Check if device is in landscape orientation
 * 
 * @return true if the device is in landscape orientation
 */
@Composable
fun isLandscape(): Boolean = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp

/**
 * Get appropriate spacing for the current screen size
 * 
 * @param compact spacing for compact screens
 * @param medium spacing for medium screens
 * @param expanded spacing for expanded screens
 * @return the appropriate spacing based on screen width
 */
@Composable
fun adaptiveSize(compact: Dp, medium: Dp, expanded: Dp): Dp {
    val screenWidth = LocalConfiguration.current.screenWidthDp

    return when {
        screenWidth >= 840 -> expanded
        screenWidth >= 600 -> medium
        else -> compact
    }
}
