package com.lloir.ornaassistant.presentation.ui.settings

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.domain.model.AppSettings

@Composable
fun DungeonOverlaySettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    
    DungeonOverlaySettingsScreen(
        settings = settings,
        onNavigateBack = onNavigateBack,
        onUpdateTitleColor = viewModel::updateDungeonOverlayTitleColor,
        onUpdateModeColor = viewModel::updateDungeonOverlayModeColor,
        onUpdateFloorColor = viewModel::updateDungeonOverlayFloorColor,
        onUpdateRewardsColor = viewModel::updateDungeonOverlayRewardsColor,
        onUpdateCooldownColor = viewModel::updateDungeonOverlayCooldownColor,
        onUpdateSpecialInfoColor = viewModel::updateDungeonOverlaySpecialInfoColor,
        onUpdateTitleSize = viewModel::updateDungeonOverlayTitleSize,
        onUpdateModeSize = viewModel::updateDungeonOverlayModeSize,
        onUpdateFloorSize = viewModel::updateDungeonOverlayFloorSize,
        onUpdateRewardsSize = viewModel::updateDungeonOverlayRewardsSize,
        onUpdateCooldownSize = viewModel::updateDungeonOverlayCooldownSize,
        onUpdateSpecialInfoSize = viewModel::updateDungeonOverlaySpecialInfoSize,
        onUpdateShowFloorProgress = viewModel::updateShowFloorProgress,
        onUpdateColorCodeDungeons = viewModel::updateColorCodeDungeons,
        onUpdateShowRewardsEstimate = viewModel::updateShowRewardsEstimate,
        onUpdateShowDungeonSpecialInfo = viewModel::updateShowDungeonSpecialInfo,
        onUpdateFlashOnFloorChange = viewModel::updateFlashOnFloorChange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DungeonOverlaySettingsScreen(
    settings: AppSettings,
    onNavigateBack: () -> Unit,
    onUpdateTitleColor: (Int) -> Unit,
    onUpdateModeColor: (Int) -> Unit,
    onUpdateFloorColor: (Int) -> Unit,
    onUpdateRewardsColor: (Int) -> Unit,
    onUpdateCooldownColor: (Int) -> Unit,
    onUpdateSpecialInfoColor: (Int) -> Unit,
    onUpdateTitleSize: (Float) -> Unit,
    onUpdateModeSize: (Float) -> Unit,
    onUpdateFloorSize: (Float) -> Unit,
    onUpdateRewardsSize: (Float) -> Unit,
    onUpdateCooldownSize: (Float) -> Unit,
    onUpdateSpecialInfoSize: (Float) -> Unit,
    onUpdateShowFloorProgress: (Boolean) -> Unit,
    onUpdateColorCodeDungeons: (Boolean) -> Unit,
    onUpdateShowRewardsEstimate: (Boolean) -> Unit,
    onUpdateShowDungeonSpecialInfo: (Boolean) -> Unit,
    onUpdateFlashOnFloorChange: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dungeon Overlay Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                .verticalScroll(rememberScrollState())
        ) {
            // Preview of the dungeon overlay
            DungeonOverlayPreview(settings)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Content Settings
            SettingsSection(title = "Content Settings") {
                SettingsSwitch(
                    title = "Show Floor Progress",
                    description = "Display progress percentage for dungeon floors",
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Font size settings
            SettingsSection(title = "Font Size Settings") {
                FontSizeSlider(
                    title = "Title Size",
                    value = settings.dungeonOverlayTitleSize,
                    onValueChange = onUpdateTitleSize
                )
                
                FontSizeSlider(
                    title = "Mode Size",
                    value = settings.dungeonOverlayModeSize,
                    onValueChange = onUpdateModeSize
                )
                
                FontSizeSlider(
                    title = "Floor Size",
                    value = settings.dungeonOverlayFloorSize,
                    onValueChange = onUpdateFloorSize
                )
                
                FontSizeSlider(
                    title = "Rewards Size",
                    value = settings.dungeonOverlayRewardsSize,
                    onValueChange = onUpdateRewardsSize
                )
                
                FontSizeSlider(
                    title = "Cooldown Size",
                    value = settings.dungeonOverlayCooldownSize,
                    onValueChange = onUpdateCooldownSize
                )
                
                FontSizeSlider(
                    title = "Special Info Size",
                    value = settings.dungeonOverlaySpecialInfoSize,
                    onValueChange = onUpdateSpecialInfoSize
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Color settings
            SettingsSection(title = "Color Settings") {
                ColorPicker(
                    title = "Title Color",
                    currentColor = settings.dungeonOverlayTitleColor,
                    onColorSelected = onUpdateTitleColor
                )
                
                ColorPicker(
                    title = "Mode Color",
                    currentColor = settings.dungeonOverlayModeColor,
                    onColorSelected = onUpdateModeColor
                )
                
                ColorPicker(
                    title = "Floor Color",
                    currentColor = settings.dungeonOverlayFloorColor,
                    onColorSelected = onUpdateFloorColor
                )
                
                ColorPicker(
                    title = "Rewards Color",
                    currentColor = settings.dungeonOverlayRewardsColor,
                    onColorSelected = onUpdateRewardsColor
                )
                
                ColorPicker(
                    title = "Cooldown Color",
                    currentColor = settings.dungeonOverlayCooldownColor,
                    onColorSelected = onUpdateCooldownColor
                )
                
                ColorPicker(
                    title = "Special Info Color",
                    currentColor = settings.dungeonOverlaySpecialInfoColor,
                    onColorSelected = onUpdateSpecialInfoColor
                )
            }
        }
    }
}

@Composable
fun DungeonOverlayPreview(settings: AppSettings) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "🐉 Dragon Roost",
                    fontSize = settings.dungeonOverlayTitleSize.sp,
                    color = androidx.compose.ui.graphics.Color(settings.dungeonOverlayTitleColor),
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Hard Boss Mode",
                    fontSize = settings.dungeonOverlayModeSize.sp,
                    color = androidx.compose.ui.graphics.Color(settings.dungeonOverlayModeColor)
                )
                
                Text(
                    text = "Floor: 5/10 (50%)",
                    fontSize = settings.dungeonOverlayFloorSize.sp,
                    color = androidx.compose.ui.graphics.Color(settings.dungeonOverlayFloorColor)
                )
                
                Text(
                    text = "Orns: 25.5K | Gold: 125K | XP: 50K",
                    fontSize = settings.dungeonOverlayRewardsSize.sp,
                    color = androidx.compose.ui.graphics.Color(settings.dungeonOverlayRewardsColor)
                )
                
                Text(
                    text = "Cooldown: 11 hours",
                    fontSize = settings.dungeonOverlayCooldownSize.sp,
                    color = androidx.compose.ui.graphics.Color(settings.dungeonOverlayCooldownColor)
                )
                
                Text(
                    text = "Draconian & Dragon families, +Orns reward",
                    fontSize = settings.dungeonOverlaySpecialInfoSize.sp,
                    color = androidx.compose.ui.graphics.Color(settings.dungeonOverlaySpecialInfoColor)
                )
            }
        }
    }
}