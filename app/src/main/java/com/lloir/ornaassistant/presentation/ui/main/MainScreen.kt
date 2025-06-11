package com.lloir.ornaassistant.presentation.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.presentation.ui.components.WeeklyChart
import com.lloir.ornaassistant.presentation.ui.components.StatisticsCard
import com.lloir.ornaassistant.presentation.ui.components.PermissionCard
import com.lloir.ornaassistant.presentation.viewmodel.MainViewModel
import com.lloir.ornaassistant.presentation.viewmodel.AccessibilityServiceViewModel
import com.lloir.ornaassistant.presentation.viewmodel.ChartViewModel
import com.lloir.ornaassistant.presentation.viewmodel.PermissionStatus
import com.lloir.ornaassistant.utils.PermissionHelper
import androidx.lifecycle.Lifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToDrops: () -> Unit,
    onNavigateToEfficiency: () -> Unit,
    onNavigateToCombatLog: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToCooldowns: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
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
        topBar = {
            TopAppBar(
                title = { Text("Orna Assistant") },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History") 
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Permission Status Card
            PermissionCard(
                permissionStatus = permissionStatus,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onRequestAccessibilityPermission = onRequestAccessibilityPermission
            )

            // Feature Cards
            item {
                FeatureCard(
                    title = "Efficiency",
                    subtitle = "Track rates",
                    icon = Icons.Default.TrendingUp,
                    onClick = onNavigateToEfficiency,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                FeatureCard(
                    title = "Combat Log",
                    subtitle = "Recent activity",
                    icon = Icons.Default.FormatListBulleted,
                    onClick = onNavigateToCombatLog,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            item {
                FeatureCard(
                    title = "Daily Goals",
                    subtitle = "Track progress",
                    icon = Icons.Default.CheckCircle,
                    onClick = onNavigateToGoals,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            item {
                FeatureCard(
                    title = "Cooldowns",
                    subtitle = "Dungeon timers",
                    icon = Icons.Default.Timer,
                    onClick = onNavigateToCooldowns,
                    color = MaterialTheme.colorScheme.error
                )
            }

            item {
                FeatureCard(
                    title = "History",
                    subtitle = "Past runs",
                    icon = Icons.Default.History,
                    onClick = onNavigateToHistory
                )
            }

            item {
                FeatureCard(
                    title = "Drops",
                    subtitle = "Statistics",
                    icon = Icons.Default.Assessment,
                    onClick = onNavigateToDrops
                )
            }

            item {
                FeatureCard(
                    title = "Analytics",
                    subtitle = "Patterns",
                    icon = Icons.Default.Analytics,
                    onClick = onNavigateToAnalytics
                )
            }

            item {
                FeatureCard(
                    title = "Party",
                    subtitle = "Auto-accept",
                    icon = Icons.Default.Group,
                    onClick = { /* Navigate to party */ }
                )
            }

            // Weekly Statistics Chart
            if (chartData != null) {
                Card {
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

            // Statistics Overview
            if (uiState.dungeonStatistics != null) {
                StatisticsCard(statistics = uiState.dungeonStatistics!!)
            }

            // Developer Support Section
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Developed by lloir. If you wish, you can support the development by donating!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            uriHandler.openUri("https://www.paypal.com/donate/?business=L7Q94HMXMHA5A&no_recurring=0&item_name=Orna+assistant+development&currency_code=EUR")
                        },
                        modifier = Modifier.size(width = 200.dp, height = 48.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Donate via PayPal")
                    }
                }
            }

            // Error Handling
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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

            // Loading Indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
