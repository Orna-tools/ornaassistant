package com.lloir.ornaassistant.presentation.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.BuildConfig
import com.lloir.ornaassistant.domain.model.ThemeMode
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsDropdown(
    title: String,
    description: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    optionToString: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = optionToString(selectedOption),
                onValueChange = { },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionToString(option)) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
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

            // Appearance Section
            SettingsSection(title = "Appearance") {
                SettingsDropdown(
                    title = "Theme Mode",
                    description = "Choose between light, dark, or system default theme",
                    options = ThemeMode.values().toList(),
                    selectedOption = settings.themeMode,
                    onOptionSelected = viewModel::updateThemeMode,
                    optionToString = { 
                        when (it) {
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                            ThemeMode.SYSTEM -> "System Default"
                        }
                    }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsSwitch(
                        title = "Dynamic Colors",
                        description = "Use colors from your wallpaper for the app theme (Android 12+)",
                        checked = settings.useDynamicColors,
                        onCheckedChange = viewModel::updateUseDynamicColors
                    )
                }
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
            }

            // Android 16 specific settings
            SettingsSection(title = "Android 16 Compatibility") {
                SettingsSwitch(
                    title = "Adaptive Layouts",
                    description = "Enable responsive layouts that adapt to different screen sizes and orientations (Android 16+)",
                    checked = settings.useAdaptiveLayouts,
                    onCheckedChange = viewModel::updateUseAdaptiveLayouts
                )
            }

            // App Information
            SettingsSection(title = "About") {
                val uriHandler = LocalUriHandler.current

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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Discord Support Link
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Need help or want to join the community?",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Button(
                                onClick = {
                                    uriHandler.openUri("https://discord.gg/4jCZwmya3c")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Join our Discord community")
                            }
                        }
                    }
                }
            }
        }
    }
}
