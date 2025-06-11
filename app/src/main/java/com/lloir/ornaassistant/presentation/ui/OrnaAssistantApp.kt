package com.lloir.ornaassistant.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
                onNavigateToEfficiency = { navController.navigate("efficiency") },
                onNavigateToCombatLog = { navController.navigate("combatlog") },
                onNavigateToGoals = { navController.navigate("goals") },
                onNavigateToCooldowns = { navController.navigate("cooldowns") },
                onNavigateToAnalytics = { navController.navigate("analytics") },
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

        composable("efficiency") {
            EfficiencyScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("combatlog") {
            CombatLogScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("goals") {
            GoalsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("cooldowns") {
            DungeonCooldownsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("analytics") {
            AnalyticsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("party") {
            PartyManagementScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("screenshots") {
            ScreenshotGalleryScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
