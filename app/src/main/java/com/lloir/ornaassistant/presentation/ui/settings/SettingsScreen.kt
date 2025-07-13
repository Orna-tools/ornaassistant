package com.lloir.ornaassistant.presentation.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.BuildConfig
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.domain.model.ThemeMode
import com.lloir.ornaassistant.presentation.theme.OrnaAssistantTheme

@Composable
fun SettingsSection(
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
fun SettingsSwitch(
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
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToAssessmentOverlaySettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    SettingsScreen(
        settings = settings,
        onNavigateBack = onNavigateBack,
        onUpdateAssessOverlay = viewModel::updateAssessOverlay,
        onUpdateAutoHideOverlays = viewModel::updateAutoHideOverlays,
        onUpdateOverlayTransparency = viewModel::updateOverlayTransparency,
        onUpdateThemeMode = viewModel::updateThemeMode,
        onUpdateUseDynamicColors = viewModel::updateUseDynamicColors,
        onUpdateUseHighContrastMode = viewModel::updateUseHighContrastMode,
        // Accessibility settings
        onUpdateUseLargerFontSize = viewModel::updateUseLargerFontSize,
        onUpdateUseTextToSpeech = viewModel::updateUseTextToSpeech,
        onUpdateUseReducedMotion = viewModel::updateUseReducedMotion,
        // Tutorial settings
        onUpdateShowFeatureTutorials = viewModel::updateShowFeatureTutorials,
        onRestartTutorial = viewModel::restartTutorial,
        onNavigateToAssessmentOverlaySettings = onNavigateToAssessmentOverlaySettings,
        // Dungeon overlay settings
        onUpdateShowDungeonOverlay = viewModel::updateShowDungeonOverlay,
        onUpdateShowFloorProgress = viewModel::updateShowFloorProgress,
        onUpdateColorCodeDungeons = viewModel::updateColorCodeDungeons,
        onUpdateShowRewardsEstimate = viewModel::updateShowRewardsEstimate,
        onUpdateShowDungeonSpecialInfo = viewModel::updateShowDungeonSpecialInfo,
        onUpdateFlashOnFloorChange = viewModel::updateFlashOnFloorChange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onNavigateBack: () -> Unit,
    onUpdateAssessOverlay: (Boolean) -> Unit,
    onUpdateAutoHideOverlays: (Boolean) -> Unit,
    onUpdateOverlayTransparency: (Float) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateUseDynamicColors: (Boolean) -> Unit,
    onUpdateUseHighContrastMode: (Boolean) -> Unit,
    // Accessibility settings
    onUpdateUseLargerFontSize: (Boolean) -> Unit = {},
    onUpdateUseTextToSpeech: (Boolean) -> Unit = {},
    onUpdateUseReducedMotion: (Boolean) -> Unit = {},
    // Tutorial settings
    onUpdateShowFeatureTutorials: (Boolean) -> Unit = {},
    onRestartTutorial: () -> Unit = {},
    onNavigateToAssessmentOverlaySettings: () -> Unit = {},
    // Dungeon overlay settings
    onUpdateShowDungeonOverlay: (Boolean) -> Unit = {},
    onUpdateShowFloorProgress: (Boolean) -> Unit = {},
    onUpdateColorCodeDungeons: (Boolean) -> Unit = {},
    onUpdateShowRewardsEstimate: (Boolean) -> Unit = {},
    onUpdateShowDungeonSpecialInfo: (Boolean) -> Unit = {},
    onUpdateFlashOnFloorChange: (Boolean) -> Unit = {}
) {
    Scaffold(
        topBar = {
            SettingsTopAppBar(onBackClicked = onNavigateBack)
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
                    onCheckedChange = onUpdateAssessOverlay
                )

                if (settings.showAssessOverlay) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNavigateToAssessmentOverlaySettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Customize Assessment Overlay")
                    }
                }

                SettingsSwitch(
                    title = "Auto-hide Overlays",
                    description = "Automatically hide overlays when not relevant",
                    checked = settings.autoHideOverlays,
                    onCheckedChange = onUpdateAutoHideOverlays
                )
            }

            // Dungeon Overlay Section
            SettingsSection(title = "Dungeon Overlay") {
                SettingsSwitch(
                    title = "Dungeon Tracking Overlay",
                    description = "Show dungeon information while in dungeons",
                    checked = settings.showDungeonOverlay,
                    onCheckedChange = onUpdateShowDungeonOverlay
                )

                if (settings.showDungeonOverlay) {
                    SettingsSwitch(
                        title = "Show Floor Progress",
                        description = "Display progress bar for dungeon floors",
                        checked = settings.showFloorProgress,
                        onCheckedChange = onUpdateShowFloorProgress
                    )

                    SettingsSwitch(
                        title = "Color-code Dungeons",
                        description = "Use different colors for different dungeon types",
                        checked = settings.colorCodeDungeons,
                        onCheckedChange = onUpdateColorCodeDungeons
                    )

                    SettingsSwitch(
                        title = "Show Rewards Estimate",
                        description = "Show estimated rewards based on dungeon type and floor",
                        checked = settings.showRewardsEstimate,
                        onCheckedChange = onUpdateShowRewardsEstimate
                    )

                    SettingsSwitch(
                        title = "Show Special Information",
                        description = "Show special information about the dungeon type",
                        checked = settings.showDungeonSpecialInfo,
                        onCheckedChange = onUpdateShowDungeonSpecialInfo
                    )

                    SettingsSwitch(
                        title = "Flash on Floor Change",
                        description = "Visual feedback when floor changes",
                        checked = settings.flashOnFloorChange,
                        onCheckedChange = onUpdateFlashOnFloorChange
                    )
                }
            }

            // Overlay Transparency
            SettingsSection(title = "Overlay Appearance") {
                SettingsSlider(
                    title = "Overlay Transparency",
                    description = "Adjust how transparent the overlays appear",
                    value = settings.overlayTransparency,
                    onValueChange = onUpdateOverlayTransparency,
                    valueRange = 0.1f..1.0f,
                    valueLabel = { "${(it * 100).toInt()}%" }
                )
            }

            // Appearance Section
            SettingsSection(title = "Appearance") {
                SettingsDropdown(
                    title = "Theme Mode",
                    description = "Choose between light, dark, or system default theme",
                    options = ThemeMode.entries,
                    selectedOption = settings.themeMode,
                    onOptionSelected = onUpdateThemeMode,
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
                        onCheckedChange = onUpdateUseDynamicColors
                    )
                }
            }

            // Accessibility Section
            SettingsSection(title = "Accessibility") {
                SettingsSwitch(
                    title = "High Contrast Mode",
                    description = "Increase contrast for better readability",
                    checked = settings.useHighContrastMode,
                    onCheckedChange = onUpdateUseHighContrastMode
                )

                SettingsSwitch(
                    title = "Larger Font Size",
                    description = "Increase text size throughout the app",
                    checked = settings.useLargerFontSize,
                    onCheckedChange = onUpdateUseLargerFontSize
                )

                SettingsSwitch(
                    title = "Text-to-Speech",
                    description = "Enable screen reading for important elements",
                    checked = settings.useTextToSpeech,
                    onCheckedChange = onUpdateUseTextToSpeech
                )

                SettingsSwitch(
                    title = "Reduced Motion",
                    description = "Minimize animations for motion sensitivity",
                    checked = settings.useReducedMotion,
                    onCheckedChange = onUpdateUseReducedMotion
                )
            }

            // Tutorial Settings Section
            SettingsSection(title = "Tutorial") {
                SettingsSwitch(
                    title = "Feature Tutorials",
                    description = "Show tutorial cards for app features",
                    checked = settings.showFeatureTutorials,
                    onCheckedChange = onUpdateShowFeatureTutorials
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onRestartTutorial,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restart Tutorial")
                }
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
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopAppBar(onBackClicked: () -> Unit) {
    TopAppBar(
        title = { Text("Settings") },
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    )
}


@Preview
@Composable
private fun SettingsScreenPreview() {
    val appSettings = AppSettings(
        showAssessOverlay = true,
        autoHideOverlays = false,
        overlayTransparency = 0.75f,
        themeMode = ThemeMode.DARK,
        useDynamicColors = true,
        useHighContrastMode = false,
        useLargerFontSize = true,
        useTextToSpeech = false,
        useReducedMotion = true
    )
    OrnaAssistantTheme(darkTheme = true) {
        SettingsScreen(
            settings = appSettings,
            onNavigateBack = {},
            onUpdateAssessOverlay = {},
            onUpdateAutoHideOverlays = {},
            onUpdateOverlayTransparency = {},
            onUpdateThemeMode = {},
            onUpdateUseDynamicColors = {},
            onUpdateUseHighContrastMode = {},
            onUpdateUseLargerFontSize = {},
            onUpdateUseTextToSpeech = {},
            onUpdateUseReducedMotion = {})
    }
}
