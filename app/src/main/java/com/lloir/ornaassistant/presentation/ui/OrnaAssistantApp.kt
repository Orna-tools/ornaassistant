package com.lloir.ornaassistant.presentation.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lloir.ornaassistant.presentation.ui.main.MainScreen
import com.lloir.ornaassistant.presentation.ui.settings.SettingsScreen
import com.lloir.ornaassistant.presentation.ui.history.DungeonHistoryScreen
import com.lloir.ornaassistant.presentation.ui.drops.DropsStatisticsScreen

@Composable
fun OrnaAssistantApp(
    navController: NavHostController,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToDrops = { navController.navigate("drops") },
                onRequestOverlayPermission = onRequestOverlayPermission,
                onRequestAccessibilityPermission = onRequestAccessibilityPermission
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("history") {
            DungeonHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("drops") {
            DropsStatisticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}