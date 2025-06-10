package com.lloir.ornaassistant.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Overlays Section
            SettingsSection(title = "Overlays") {
                SettingsSwitch(
                    title = "Session Overlay",
                    description = "Shows dungeon and wayvessel session statistics",
                    checked = settings.showSessionOverlay,
                    onCheckedChange = viewModel::updateSessionOverlay
                )

                SettingsSwitch(
                    title = "Invites Overlay",
                    description = "Shows party invites with dungeon cooldown information",
                    checked = settings.showInvitesOverlay,
                    onCheckedChange = viewModel::updateInvitesOverlay
                )

                SettingsSwitch(
                    title = "Item Assessment Overlay",
                    description = "Automatically assess items when viewing them",
                    checked = settings.showAssessOverlay,
                    onCheckedChange = viewModel::updateAssessOverlay
                )

                SettingsSwitch(
                    title = "Auto-hide Overlays",
                    description = "Automatically hide overlays when not relevant",
                    checked = settings.autoHideOverlays,
                    onCheckedChange = viewModel::updateAutoHideOverlays
                )
            }

            // Overlay Transparency
            SettingsSection(title = "Overlay Appearance") {
                SettingsSlider(
                    title = "Overlay Transparency",
                    description = "Adjust how transparent the overlays appear",
                    value = settings.overlayTransparency,
                    onValueChange = viewModel::updateOverlayTransparency,
                    valueRange = 0.1f..1.0f,
                    valueLabel = { "${(it * 100).toInt()}%" }
                )
            }

            // Notifications Section
            SettingsSection(title = "Notifications") {
                SettingsSwitch(
                    title = "Wayvessel Notifications",
                    description = "Get notified when wayvessel cooldown ends",
                    checked = settings.wayvesselNotifications,
                    onCheckedChange = viewModel::updateWayvesselNotifications
                )

                SettingsSwitch(
                    title = "Notification Sounds",
                    description = "Play sounds with notifications",
                    checked = settings.notificationSounds,
                    onCheckedChange = viewModel::updateNotificationSounds
                )
            }

            // App Information
            SettingsSection(title = "About") {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Orna Assistant",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Version 2.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A modern assistant app for Orna RPG players. Tracks dungeon visits, wayvessel sessions, and provides helpful overlays.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsSlider(
    title: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = valueLabel(value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}