package com.lloir.ornaassistant.presentation.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.domain.model.DashboardLayout
import com.lloir.ornaassistant.domain.model.DashboardWidget

@Composable
fun DashboardSettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: DashboardSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    
    DashboardSettingsScreen(
        settings = settings,
        onNavigateBack = onNavigateBack,
        onUpdateDashboardLayout = viewModel::updateDashboardLayout,
        onUpdateEnabledWidgets = viewModel::updateEnabledWidgets,
        onUpdateWidgetOrder = viewModel::updateWidgetOrder
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardSettingsScreen(
    settings: AppSettings,
    onNavigateBack: () -> Unit,
    onUpdateDashboardLayout: (DashboardLayout) -> Unit,
    onUpdateEnabledWidgets: (Set<DashboardWidget>) -> Unit,
    onUpdateWidgetOrder: (List<DashboardWidget>) -> Unit
) {
    var enabledWidgets by remember { mutableStateOf(settings.enabledWidgets) }
    var widgetOrder by remember { mutableStateOf(settings.widgetOrder) }
    
    // Function to move a widget up or down in the order
    fun moveWidget(widget: DashboardWidget, moveUp: Boolean) {
        val currentIndex = widgetOrder.indexOf(widget)
        if ((moveUp && currentIndex > 0) || (!moveUp && currentIndex < widgetOrder.size - 1)) {
            val newIndex = if (moveUp) currentIndex - 1 else currentIndex + 1
            val newOrder = widgetOrder.toMutableList()
            newOrder.removeAt(currentIndex)
            newOrder.add(newIndex, widget)
            widgetOrder = newOrder
            onUpdateWidgetOrder(newOrder)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Layout selection
            SettingsSection(title = "Dashboard Layout") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardLayout.values().forEach { layout ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onUpdateDashboardLayout(layout) }
                                .background(
                                    if (settings.dashboardLayout == layout)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (settings.dashboardLayout == layout)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.dashboardLayout == layout,
                                onClick = { onUpdateDashboardLayout(layout) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = when (layout) {
                                        DashboardLayout.STANDARD -> "Standard"
                                        DashboardLayout.COMPACT -> "Compact"
                                        DashboardLayout.DETAILED -> "Detailed"
                                        DashboardLayout.GRID -> "Grid"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = when (layout) {
                                        DashboardLayout.STANDARD -> "Default layout with balanced information"
                                        DashboardLayout.COMPACT -> "Condensed view with minimal spacing"
                                        DashboardLayout.DETAILED -> "Expanded view with more information"
                                        DashboardLayout.GRID -> "Grid layout for larger screens"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // Widget visibility and order
            SettingsSection(title = "Widget Visibility & Order") {
                Text(
                    text = "Use arrows to reorder widgets or toggle visibility",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    widgetOrder.forEachIndexed { index, widget ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = when (widget) {
                                            DashboardWidget.PERMISSION_STATUS -> "Permission Status"
                                            DashboardWidget.WEEKLY_CHART -> "Weekly Chart"
                                            DashboardWidget.STATISTICS -> "Statistics"
                                            DashboardWidget.DEVELOPER_SUPPORT -> "Developer Support"
                                            DashboardWidget.RECENT_DUNGEONS -> "Recent Dungeons"
                                            DashboardWidget.MATERIAL_TRACKER -> "Material Tracker"
                                        },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    
                                    Text(
                                        text = when (widget) {
                                            DashboardWidget.PERMISSION_STATUS -> "Shows current permission status"
                                            DashboardWidget.WEEKLY_CHART -> "Shows dungeon visits from past 7 days"
                                            DashboardWidget.STATISTICS -> "Shows dungeon statistics"
                                            DashboardWidget.DEVELOPER_SUPPORT -> "Support the developer"
                                            DashboardWidget.RECENT_DUNGEONS -> "Shows recently visited dungeons"
                                            DashboardWidget.MATERIAL_TRACKER -> "Track materials for crafting"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Move up button
                                IconButton(
                                    onClick = { moveWidget(widget, true) },
                                    enabled = index > 0
                                ) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = "Move up",
                                        tint = if (index > 0) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                }
                                
                                // Move down button
                                IconButton(
                                    onClick = { moveWidget(widget, false) },
                                    enabled = index < widgetOrder.size - 1
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = "Move down",
                                        tint = if (index < widgetOrder.size - 1) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                }
                                
                                // Visibility toggle
                                IconButton(
                                    onClick = {
                                        val newEnabledWidgets = enabledWidgets.toMutableSet()
                                        if (widget in enabledWidgets) {
                                            newEnabledWidgets.remove(widget)
                                        } else {
                                            newEnabledWidgets.add(widget)
                                        }
                                        enabledWidgets = newEnabledWidgets
                                        onUpdateEnabledWidgets(newEnabledWidgets)
                                    }
                                ) {
                                    Icon(
                                        if (widget in enabledWidgets) 
                                            Icons.Default.Visibility 
                                        else 
                                            Icons.Default.VisibilityOff,
                                        contentDescription = if (widget in enabledWidgets) 
                                            "Hide widget" 
                                        else 
                                            "Show widget",
                                        tint = if (widget in enabledWidgets)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Preview section
            SettingsSection(title = "Preview") {
                Text(
                    text = "This is how your dashboard will look with the selected layout and widgets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Simple preview of the dashboard layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Dashboard Preview\n${settings.dashboardLayout} layout with ${enabledWidgets.size} widgets",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}