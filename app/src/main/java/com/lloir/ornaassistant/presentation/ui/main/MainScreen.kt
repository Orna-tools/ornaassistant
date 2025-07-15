package com.lloir.ornaassistant.presentation.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.presentation.ui.components.*
import com.lloir.ornaassistant.presentation.viewmodel.MainViewModel
import com.lloir.ornaassistant.presentation.viewmodel.AccessibilityServiceViewModel
import com.lloir.ornaassistant.presentation.viewmodel.ChartViewModel
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.domain.model.DashboardLayout
import com.lloir.ornaassistant.domain.model.DashboardWidget
import com.lloir.ornaassistant.utils.PermissionHelper
import androidx.lifecycle.Lifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToMaterials: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    mainViewModel: MainViewModel = hiltViewModel(),
    serviceViewModel: AccessibilityServiceViewModel = hiltViewModel(),
    chartViewModel: ChartViewModel = hiltViewModel()
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val settings by mainViewModel.settings.collectAsState()
    val permissionStatus by serviceViewModel.permissionStatus.collectAsState()
    val chartData by chartViewModel.chartData.collectAsState()
    val weeklyStats by mainViewModel.weeklyStats.collectAsState()

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Check permissions on composition and when returning to screen
    LaunchedEffect(Unit) {
        val hasOverlay = PermissionHelper.hasOverlayPermission(context)
        val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(context)
        serviceViewModel.checkAndUpdatePermissions(hasOverlay, hasAccessibility)
    }

    // Re-check permissions when lifecycle resumes
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasOverlay = PermissionHelper.hasOverlayPermission(context)
                val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(context)
                serviceViewModel.checkAndUpdatePermissions(hasOverlay, hasAccessibility)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        // Remove the topBar parameter completely
    ) { paddingValues ->
        // Apply dashboard layout based on settings
        val dashboardLayout = settings?.dashboardLayout ?: DashboardLayout.STANDARD
        val enabledWidgets = settings?.enabledWidgets ?: DashboardWidget.values().toSet()
        val widgetOrder = settings?.widgetOrder ?: DashboardWidget.values().toList()

        // Apply consistent padding to ensure proper spacing
        val modifiedPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + 16.dp,
            end = paddingValues.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + 16.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp,
            top = paddingValues.calculateTopPadding() + 16.dp
        )

        // Filter and sort widgets based on settings
        // Remove RECENT_DUNGEONS and MATERIAL_TRACKER as requested
        val orderedEnabledWidgets = widgetOrder
            .filter { it in enabledWidgets }
            .filter { it != DashboardWidget.RECENT_DUNGEONS && it != DashboardWidget.MATERIAL_TRACKER }

        when (dashboardLayout) {
            DashboardLayout.GRID -> {
                // Grid layout for larger screens
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(modifiedPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tutorial card for new users
                    if (settings?.hasCompletedTutorial == false || settings?.showFeatureTutorials == true) {
                        item {
                            var showMainTutorial by remember { mutableStateOf(true) }
                            if (showMainTutorial) {
                                FeatureTutorialCard(
                                    title = "Welcome to Orna Assistant",
                                    description = "This is your dashboard for tracking dungeon runs, item assessments, and more. Enable permissions to get started with all features.",
                                    icon = Icons.Default.Info,
                                    onDismiss = { showMainTutorial = false }
                                )
                            }
                        }
                    }

                    // Render widgets based on ordered enabled widgets
                    items(orderedEnabledWidgets) { widget ->
                        when (widget) {
                            DashboardWidget.PERMISSION_STATUS -> {
                                PermissionCard(
                                    permissionStatus = permissionStatus,
                                    onRequestOverlayPermission = onRequestOverlayPermission,
                                    onRequestAccessibilityPermission = onRequestAccessibilityPermission
                                )
                            }
                            DashboardWidget.WEEKLY_CHART -> {
                                if (chartData != null) {
                                    OrnaCard {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                text = "Dungeon visits from past 7 days",
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.padding(bottom = 16.dp)
                                            )

                                            WeeklyChart(
                                                chartData = chartData!!,
                                                modifier = Modifier.height(300.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            DashboardWidget.STATISTICS -> {
                                if (uiState.dungeonStatistics != null) {
                                    StatisticsCard(statistics = uiState.dungeonStatistics!!)
                                }
                            }
                            DashboardWidget.DEVELOPER_SUPPORT -> {
                                DeveloperSupportCard {
                                    uriHandler.openUri("https://buymeacoffee.com/lloir")
                                }
                            }
                            DashboardWidget.RECENT_DUNGEONS -> {
                                // Placeholder for Recent Dungeons widget
                                // This would be implemented in a future update
                                OrnaCard {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Recent Dungeons",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        Text("No recent dungeons to display")
                                    }
                                }
                            }
                            DashboardWidget.MATERIAL_TRACKER -> {
                                // Placeholder for Material Tracker widget
                                // This would be implemented in a future update
                                OrnaCard {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Material Tracker",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        Text("No tracked materials to display")
                                    }
                                }
                            }
                        }
                    }

                    // Error Handling
                    item {
                        uiState.error?.let { error ->
                            OrnaCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .background(MaterialTheme.colorScheme.errorContainer),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = mainViewModel::clearError) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Loading Indicator
                    item {
                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                // Standard, Compact, or Detailed layouts use AdaptiveContainer with different spacing
                AdaptiveContainer(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(modifiedPadding)
                        .verticalScroll(rememberScrollState()),
                    useAdaptiveLayouts = settings?.useAdaptiveLayouts ?: true
                ) {
                    // Content automatically adapts to screen size using AdaptiveContainer

                    // Tutorial card for new users
                    if (settings?.hasCompletedTutorial == false || settings?.showFeatureTutorials == true) {
                        var showMainTutorial by remember { mutableStateOf(true) }

                        if (showMainTutorial) {
                            FeatureTutorialCard(
                                title = "Welcome to Orna Assistant",
                                description = "This is your dashboard for tracking dungeon runs, item assessments, and more. Enable permissions to get started with all features.",
                                icon = Icons.Default.Info,
                                onDismiss = { showMainTutorial = false }
                            )
                        }
                    }

                    // Apply consistent spacing based on layout
                    val widgetSpacing = when (dashboardLayout) {
                        DashboardLayout.COMPACT -> 8.dp
                        DashboardLayout.DETAILED -> 24.dp
                        else -> 16.dp // STANDARD
                    }

                    // Render widgets based on ordered enabled widgets
                    for (widget in orderedEnabledWidgets) {
                        when (widget) {
                            DashboardWidget.PERMISSION_STATUS -> {
                                PermissionCard(
                                    permissionStatus = permissionStatus,
                                    onRequestOverlayPermission = onRequestOverlayPermission,
                                    onRequestAccessibilityPermission = onRequestAccessibilityPermission
                                )
                                Spacer(modifier = Modifier.height(widgetSpacing))
                            }
                            DashboardWidget.WEEKLY_CHART -> {
                                if (chartData != null) {
                                    OrnaCard {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                text = "Dungeon visits from past 7 days",
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.padding(bottom = 16.dp)
                                            )

                                            WeeklyChart(
                                                chartData = chartData!!,
                                                modifier = Modifier.height(
                                                    when (dashboardLayout) {
                                                        DashboardLayout.COMPACT -> 200.dp
                                                        DashboardLayout.DETAILED -> 400.dp
                                                        else -> 300.dp
                                                    }
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(widgetSpacing))
                                }
                            }
                            DashboardWidget.STATISTICS -> {
                                if (uiState.dungeonStatistics != null) {
                                    StatisticsCard(statistics = uiState.dungeonStatistics!!)
                                    Spacer(modifier = Modifier.height(widgetSpacing))
                                }
                            }
                            DashboardWidget.DEVELOPER_SUPPORT -> {
                                DeveloperSupportCard {
                                    uriHandler.openUri("https://buymeacoffee.com/lloir")
                                }
                                Spacer(modifier = Modifier.height(widgetSpacing))
                            }
                            DashboardWidget.RECENT_DUNGEONS -> {
                                // Placeholder for Recent Dungeons widget
                                OrnaCard {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Recent Dungeons",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        Text("No recent dungeons to display")
                                    }
                                }
                                Spacer(modifier = Modifier.height(widgetSpacing))
                            }
                            DashboardWidget.MATERIAL_TRACKER -> {
                                // Placeholder for Material Tracker widget
                                OrnaCard {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Material Tracker",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        Text("No tracked materials to display")
                                    }
                                }
                                Spacer(modifier = Modifier.height(widgetSpacing))
                            }
                        }
                    }

                    // Error Handling
                    uiState.error?.let { error ->
                        OrnaCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = mainViewModel::clearError) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(widgetSpacing))
                    }

                    // Loading Indicator
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }
        }
    } // End AdaptiveContainer
}
