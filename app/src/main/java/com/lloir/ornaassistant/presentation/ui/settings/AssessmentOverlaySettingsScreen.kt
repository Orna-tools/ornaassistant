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
fun AssessmentOverlaySettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    
    AssessmentOverlaySettingsScreen(
        settings = settings,
        onNavigateBack = onNavigateBack,
        onUpdateTitleColor = viewModel::updateAssessOverlayTitleColor,
        onUpdateQualityColor = viewModel::updateAssessOverlayQualityColor,
        onUpdateStatsColor = viewModel::updateAssessOverlayStatsColor,
        onUpdateMaterialsColor = viewModel::updateAssessOverlayMaterialsColor,
        onUpdateTitleSize = viewModel::updateAssessOverlayTitleSize,
        onUpdateQualitySize = viewModel::updateAssessOverlayQualitySize,
        onUpdateStatsSize = viewModel::updateAssessOverlayStatsSize,
        onUpdateMaterialsSize = viewModel::updateAssessOverlayMaterialsSize,
        onUpdateShowMaterials = { showMaterials ->
            viewModel.updateAssessOverlayContent(
                showMaterials = showMaterials,
                showStats = settings.assessOverlayShowStats
            )
        },
        onUpdateShowStats = { showStats ->
            viewModel.updateAssessOverlayContent(
                showMaterials = settings.assessOverlayShowMaterials,
                showStats = showStats
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentOverlaySettingsScreen(
    settings: AppSettings,
    onNavigateBack: () -> Unit,
    onUpdateTitleColor: (Int) -> Unit,
    onUpdateQualityColor: (Int) -> Unit,
    onUpdateStatsColor: (Int) -> Unit,
    onUpdateMaterialsColor: (Int) -> Unit,
    onUpdateTitleSize: (Float) -> Unit,
    onUpdateQualitySize: (Float) -> Unit,
    onUpdateStatsSize: (Float) -> Unit,
    onUpdateMaterialsSize: (Float) -> Unit,
    onUpdateShowMaterials: (Boolean) -> Unit,
    onUpdateShowStats: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assessment Overlay Settings") },
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
            // Preview of the assessment overlay
            AssessmentOverlayPreview(settings)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Content visibility settings
            SettingsSection(title = "Content Settings") {
                SettingsSwitch(
                    title = "Show Materials",
                    description = "Show materials required for upgrades",
                    checked = settings.assessOverlayShowMaterials,
                    onCheckedChange = onUpdateShowMaterials
                )
                
                SettingsSwitch(
                    title = "Show Stats",
                    description = "Show item stats in the overlay",
                    checked = settings.assessOverlayShowStats,
                    onCheckedChange = onUpdateShowStats
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Font size settings
            SettingsSection(title = "Font Size Settings") {
                FontSizeSlider(
                    title = "Title Size",
                    value = settings.assessOverlayTitleSize,
                    onValueChange = onUpdateTitleSize
                )
                
                FontSizeSlider(
                    title = "Quality Size",
                    value = settings.assessOverlayQualitySize,
                    onValueChange = onUpdateQualitySize
                )
                
                FontSizeSlider(
                    title = "Stats Size",
                    value = settings.assessOverlayStatsSize,
                    onValueChange = onUpdateStatsSize
                )
                
                FontSizeSlider(
                    title = "Materials Size",
                    value = settings.assessOverlayMaterialsSize,
                    onValueChange = onUpdateMaterialsSize
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Color settings
            SettingsSection(title = "Color Settings") {
                ColorPicker(
                    title = "Title Color",
                    currentColor = settings.assessOverlayTitleColor,
                    onColorSelected = onUpdateTitleColor
                )
                
                ColorPicker(
                    title = "Quality Color",
                    currentColor = settings.assessOverlayQualityColor,
                    onColorSelected = onUpdateQualityColor
                )
                
                ColorPicker(
                    title = "Stats Color",
                    currentColor = settings.assessOverlayStatsColor,
                    onColorSelected = onUpdateStatsColor
                )
                
                ColorPicker(
                    title = "Materials Color",
                    currentColor = settings.assessOverlayMaterialsColor,
                    onColorSelected = onUpdateMaterialsColor
                )
            }
        }
    }
}

@Composable
fun AssessmentOverlayPreview(settings: AppSettings) {
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
                    text = "Ornate Godforged Arisen Staff",
                    fontSize = settings.assessOverlayTitleSize.sp,
                    color = androidx.compose.ui.graphics.Color(settings.assessOverlayTitleColor),
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Quality: 198.45%",
                    fontSize = settings.assessOverlayQualitySize.sp,
                    color = androidx.compose.ui.graphics.Color(settings.assessOverlayQualityColor)
                )
                
                if (settings.assessOverlayShowStats) {
                    Text(
                        text = "ATK: 1250  MAG: 850  DEF: 450",
                        fontSize = settings.assessOverlayStatsSize.sp,
                        color = androidx.compose.ui.graphics.Color(settings.assessOverlayStatsColor)
                    )
                }
                
                if (settings.assessOverlayShowMaterials) {
                    Text(
                        text = "MF: 125 | DF: 250",
                        fontSize = settings.assessOverlayMaterialsSize.sp,
                        color = androidx.compose.ui.graphics.Color(settings.assessOverlayMaterialsColor)
                    )
                }
            }
        }
    }
}

@Composable
fun FontSizeSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "$title: ${value.toInt()}sp",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 8f..24f,
            steps = 16,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun ColorPicker(
    title: String,
    currentColor: Int,
    onColorSelected: (Int) -> Unit
) {
    val colorOptions = listOf(
        "White" to Color.WHITE,
        "Cyan" to Color.CYAN,
        "Yellow" to Color.YELLOW,
        "Green" to Color.GREEN,
        "Red" to Color.RED,
        "Magenta" to Color.MAGENTA,
        "Blue" to Color.BLUE,
        "Light Gray" to Color.LTGRAY
    )
    
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            // Color preview
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(androidx.compose.ui.graphics.Color(currentColor))
                    .border(1.dp, MaterialTheme.colorScheme.outline)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Dropdown for color selection
            Box {
                Button(onClick = { expanded = true }) {
                    Text("Select Color")
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    colorOptions.forEach { (name, color) ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(androidx.compose.ui.graphics.Color(color))
                                            .border(1.dp, MaterialTheme.colorScheme.outline)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name)
                                }
                            },
                            onClick = {
                                onColorSelected(color)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}