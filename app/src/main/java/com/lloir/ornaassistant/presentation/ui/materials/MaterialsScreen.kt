package com.lloir.ornaassistant.presentation.ui.materials

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.domain.model.Material
import com.lloir.ornaassistant.presentation.viewmodel.MaterialsViewModel
import com.lloir.ornaassistant.presentation.ui.settings.SettingsViewModel
import com.lloir.ornaassistant.presentation.ui.components.FeatureTutorialCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsScreen(
    viewModel: MaterialsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState(initial = null)
    val tutorialSettings by settingsViewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Materials") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = padding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + 16.dp,
                    end = padding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                    top = padding.calculateTopPadding() + 16.dp
                )
        ) {
            // Tutorial card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Material Tracking",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This feature allows you to track materials in Orna. " +
                              "Open your inventory's Materials tab in the game to scan your materials. " +
                              "Set target quantities for materials you want to track."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Material Tracking",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings?.enableMaterialTracking ?: false,
                            onCheckedChange = { viewModel.updateEnableMaterialTracking(it) }
                        )
                    }
                }
            }

            // Feature tutorial card
            if (tutorialSettings.hasCompletedTutorial == false || tutorialSettings.showFeatureTutorials) {
                var showMaterialsTutorial by remember { mutableStateOf(true) }

                if (showMaterialsTutorial) {
                    FeatureTutorialCard(
                        title = "Material Tracking",
                        description = "Track materials you need for crafting and upgrades. Set target quantities and get notified when you reach your goals.",
                        icon = Icons.Default.List,
                        onDismiss = { showMaterialsTutorial = false }
                    )
                }
            }

            // Disabled state
            if (settings?.enableMaterialTracking != true) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Material tracking is disabled",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enable the switch above to start tracking materials",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                return@Scaffold
            }

            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                placeholder = { Text("Search materials...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Tracked materials section
            if (uiState.trackedMaterials.isNotEmpty()) {
                Text(
                    text = "Tracked Materials",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(uiState.trackedMaterials) { material ->
                        MaterialItem(
                            material = material,
                            onTrackClick = { viewModel.showTrackingDialog(material) },
                            onStopTrackingClick = { viewModel.stopTrackingMaterial(material.id) }
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // All materials or search results
            Text(
                text = if (uiState.searchQuery.isEmpty()) "All Materials" else "Search Results",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (uiState.filteredMaterials.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.searchQuery.isEmpty()) 
                            "No materials found. Open your inventory in Orna to scan materials." 
                        else 
                            "No materials found matching '${uiState.searchQuery}'",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(uiState.filteredMaterials) { material ->
                        MaterialItem(
                            material = material,
                            onTrackClick = { viewModel.showTrackingDialog(material) },
                            onStopTrackingClick = { viewModel.stopTrackingMaterial(material.id) }
                        )
                    }

                    // Load more indicator
                    if (uiState.hasMoreData || uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp)
                                    )
                                } else {
                                    Button(
                                        onClick = { viewModel.loadNextPage() },
                                        modifier = Modifier.fillMaxWidth(0.7f)
                                    ) {
                                        Text("Load More")
                                    }
                                }
                            }

                            // Trigger loading more data when this item becomes visible
                            LaunchedEffect(Unit) {
                                if (!uiState.isLoading && uiState.hasMoreData) {
                                    viewModel.loadNextPage()
                                }
                            }
                        }
                    }

                    // Show total count
                    if (uiState.totalItems > 0) {
                        item {
                            Text(
                                text = "Showing ${uiState.filteredMaterials.size} of ${uiState.totalItems} materials",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Track material dialog
    if (uiState.showTrackingDialog) {
        val material = uiState.selectedMaterial
        if (material != null) {
            var targetQuantity by remember { mutableStateOf((material.targetQuantity ?: material.currentQuantity).toString()) }

            AlertDialog(
                onDismissRequest = { viewModel.dismissTrackingDialog() },
                title = { Text("Track ${material.name}") },
                text = {
                    Column {
                        Text("Current quantity: ${material.currentQuantity}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Set target quantity:")
                        OutlinedTextField(
                            value = targetQuantity,
                            onValueChange = { targetQuantity = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val target = targetQuantity.toIntOrNull() ?: material.currentQuantity
                            viewModel.trackMaterial(material.id, target)
                            viewModel.dismissTrackingDialog()
                        }
                    ) {
                        Text("Track")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissTrackingDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun MaterialItem(
    material: Material,
    onTrackClick: () -> Unit,
    onStopTrackingClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge
                )

                if (material.isTracked && material.targetQuantity != null) {
                    val progress = (material.currentQuantity.toFloat() / material.targetQuantity).coerceIn(0f, 1f)

                    Text(
                        text = "${material.currentQuantity} / ${material.targetQuantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "Quantity: ${material.currentQuantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (material.isTracked) {
                IconButton(onClick = onStopTrackingClick) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Stop tracking",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                IconButton(onClick = onTrackClick) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Track material"
                    )
                }
            }
        }
    }
}
