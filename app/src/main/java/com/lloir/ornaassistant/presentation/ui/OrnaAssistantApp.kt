package com.lloir.ornaassistant.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.presentation.ui.components.OrnaBottomNavigation
import com.lloir.ornaassistant.presentation.ui.main.MainScreen
import com.lloir.ornaassistant.presentation.ui.settings.SettingsRoute
import com.lloir.ornaassistant.presentation.ui.settings.AssessmentOverlaySettingsRoute
import com.lloir.ornaassistant.presentation.ui.settings.BackupRestoreScreen
import com.lloir.ornaassistant.presentation.ui.settings.DashboardSettingsRoute
import com.lloir.ornaassistant.presentation.ui.settings.DungeonOverlaySettingsRoute
import com.lloir.ornaassistant.presentation.ui.history.DungeonHistoryScreen
import com.lloir.ornaassistant.presentation.ui.materials.MaterialsScreen
import com.lloir.ornaassistant.presentation.ui.tutorial.TutorialScreen
import com.lloir.ornaassistant.presentation.ui.settings.SettingsViewModel

@Composable
fun OrnaAssistantApp(
    navController: NavHostController,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    // Get current route for bottom navigation
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "main"

    // Get settings to check if tutorial has been completed
    val settings by settingsViewModel.settings.collectAsState()

    // Determine if we should show the bottom navigation
    // Don't show it on assessment_overlay_settings screen
    val showBottomNav = when (currentRoute) {
        "assessment_overlay_settings", "dungeon_overlay_settings" -> false
        else -> true
    }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                OrnaBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        // Avoid navigating to the same destination
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                // Pop up to the start destination to avoid building up a stack
                                popUpTo("main") {
                                    saveState = true
                                    inclusive = route == "main"
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        // Content with padding for the bottom navigation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = if (settings.hasCompletedTutorial) "main" else "tutorial"
            ) {
                composable("tutorial") {
                    TutorialScreen(
                        onFinish = {
                            navController.navigate("main") {
                                popUpTo("tutorial") { inclusive = true }
                            }
                        }
                    )
                }
                composable("main") {
                    MainScreen(
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToHistory = { navController.navigate("history") },
                        onNavigateToMaterials = { navController.navigate("materials") },
                        onRequestOverlayPermission = onRequestOverlayPermission,
                        onRequestAccessibilityPermission = onRequestAccessibilityPermission
                    )
                }

                composable("settings") {
                    SettingsRoute(
                        onNavigateBack = {
                            navController.navigate("main") {
                                // Pop up to the start destination to avoid building up a stack
                                popUpTo("main") {
                                    saveState = true
                                    inclusive = true
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        },
                        onNavigateToAssessmentOverlaySettings = { navController.navigate("assessment_overlay_settings") },
                        onNavigateToDungeonOverlaySettings = { navController.navigate("dungeon_overlay_settings") },
                        onNavigateToDashboardSettings = { navController.navigate("dashboard_settings") },
                        onNavigateToBackupRestore = { navController.navigate("backup_restore") }
                    )
                }

                composable("history") {
                    DungeonHistoryScreen(
                        onNavigateBack = {
                            navController.navigate("main") {
                                // Pop up to the start destination to avoid building up a stack
                                popUpTo("main") {
                                    saveState = true
                                    inclusive = true
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }

                composable("materials") {
                    MaterialsScreen(
                        onNavigateBack = {
                            navController.navigate("main") {
                                // Pop up to the start destination to avoid building up a stack
                                popUpTo("main") {
                                    saveState = true
                                    inclusive = true
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }

                composable("assessment_overlay_settings") {
                    AssessmentOverlaySettingsRoute(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("dungeon_overlay_settings") {
                    DungeonOverlaySettingsRoute(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }


                composable("dashboard_settings") {
                    DashboardSettingsRoute(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("backup_restore") {
                    BackupRestoreScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
