package com.lloir.ornaassistant.presentation.ui.settings

import android.os.Build
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
import com.lloir.ornaassistant.BuildConfig
import com.lloir.ornaassistant.presentation.viewmodel.SettingsViewModel

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
                    title = "Notification Sounds",
                    description = "Play sounds with notifications",
                    checked = settings.notificationSounds,
                    onCheckedChange = viewModel::updateNotificationSounds
                )
            }

            // Theme Section
            SettingsSection(title = "Appearance") {
                // Theme Mode Selection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Choose between light, dark, or system theme",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.lloir.ornaassistant.domain.model.ThemeMode.values().forEach { themeMode ->
                            val isSelected = settings.themeMode == themeMode
                            val label = when(themeMode) {
                                com.lloir.ornaassistant.domain.model.ThemeMode.LIGHT -> "Light"
                                com.lloir.ornaassistant.domain.model.ThemeMode.DARK -> "Dark"
                                com.lloir.ornaassistant.domain.model.ThemeMode.SYSTEM -> "System"
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateThemeMode(themeMode) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Dynamic Colors Switch
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsSwitch(
                        title = "Dynamic Colors",
                        description = "Use colors extracted from your wallpaper (Android 12+ only)",
                        checked = settings.useDynamicColors,
                        onCheckedChange = viewModel::updateUseDynamicColors
                    )
                }
            }

            // Features Section
            SettingsSection(title = "Features") {
                SettingsSwitch(
                    title = "Material Tracking",
                    description = "Enable tracking of materials and get notifications when targets are reached",
                    checked = settings.enableMaterialTracking,
                    onCheckedChange = viewModel::updateEnableMaterialTracking
                )

                if (settings.enableMaterialTracking) {
                    Text(
                        text = "To track materials: Go to Materials screen, search for a material, and set a target quantity.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Developer Section
            SettingsSection(title = "Developer") {
                SettingsSwitch(
                    title = "Debug Mode",
                    description = "⚠️ WARNING: Enables verbose logging. May impact performance and battery life. Only enable for troubleshooting.",
                    checked = settings.debugMode,
                    onCheckedChange = viewModel::updateDebugMode
                )

                if (settings.debugMode) {
                    Text(
                        text = "⚠️ Debug mode is active. This will generate extensive logs and may impact performance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SettingsSwitch(
                    title = "ML Kit Screen Reader (Experimental)",
                    description = "⚠️ WARNING: Uses Google ML Kit for text recognition. This is an experimental feature and may impact performance, battery life, and accuracy.",
                    checked = settings.useMlKit,
                    onCheckedChange = viewModel::updateUseMlKit
                )

                if (settings.useMlKit) {
                    Text(
                        text = "⚠️ ML Kit Screen Reader is active. This experimental feature may cause inaccurate readings and reduced performance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Android 16 specific settings
            SettingsSection(title = "Android 16 Compatibility") {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Adaptive Layouts",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "App automatically adapts to different screen sizes and orientations as required by Android 16",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                            text = "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A modern assistant app for Orna RPG players. Tracks dungeon visits, wayvessel sessions, and provides helpful overlays.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
