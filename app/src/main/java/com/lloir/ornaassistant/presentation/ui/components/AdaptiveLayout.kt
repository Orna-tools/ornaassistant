package com.lloir.ornaassistant.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Android 16 adaptive layout helper
 * Handles orientation and resizability changes for large screens
 */
@Composable
fun AdaptiveContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLargeScreen = configuration.screenWidthDp >= 600
    
    if (isLargeScreen) {
        // Large screen layout (tablets, foldables, desktop)
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
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
    } else {
        // Compact screen layout (phones)
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

/**
 * Responsive spacing based on screen size
 */
@Composable
fun adaptiveSpacing(): PaddingValues {
    val configuration = LocalConfiguration.current
    val isLargeScreen = configuration.screenWidthDp >= 600
    
    return if (isLargeScreen) {
        PaddingValues(horizontal = 32.dp, vertical = 24.dp)
    } else {
        PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    }
}

/**
 * Check if device is in landscape orientation
 */
@Composable
fun isLandscape(): Boolean = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
