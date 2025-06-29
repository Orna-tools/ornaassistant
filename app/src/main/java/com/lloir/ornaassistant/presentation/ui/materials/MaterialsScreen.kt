package com.lloir.ornaassistant.presentation.ui.materials

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.domain.model.Material
import com.lloir.ornaassistant.presentation.ui.components.AdaptiveContainer
import com.lloir.ornaassistant.presentation.viewmodel.MaterialsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MaterialsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allMaterials by viewModel.allMaterials.collectAsState()
    val trackedMaterials by viewModel.trackedMaterials.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Materials Tracker") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        AdaptiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Tutorial card
                if (settings.enableMaterialTracking) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Material Tracking Tutorial",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "1. Open Orna and navigate to Inventory → Materials",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Text(
                                text = "2. The app will automatically detect materials and their quantities",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Text(
                                text = "3. Search for a material below and tap 'Track' to set a target quantity",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Text(
                                text = "4. You'll receive a notification when you reach your target",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Material Tracking is Disabled",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Please enable Material Tracking in Settings to use this feature.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Search bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.searchMaterials(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search Materials") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchMaterials("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tracked materials section
                Text(
                    text = "Tracked Materials",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (trackedMaterials.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No materials being tracked yet. Search for materials and set tracking targets below.",
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        items(trackedMaterials) { material ->
                            TrackedMaterialItem(
                                material = material,
                                onStopTracking = { viewModel.stopTrackingMaterial(material.id) },
                                onUpdateTarget = { target -> viewModel.trackMaterial(material.id, target) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // All materials or search results
                Text(
                    text = if (uiState.searchQuery.isBlank()) "All Materials" else "Search Results",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val materialsToShow = if (uiState.searchQuery.isBlank()) allMaterials else uiState.searchResults

                if (uiState.isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (materialsToShow.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isBlank()) 
                                "No materials found. Materials will appear here when detected in the game." 
                            else 
                                "No materials found matching '${uiState.searchQuery}'",
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(materialsToShow) { material ->
                            MaterialItem(
                                material = material,
                                onTrack = { target -> viewModel.trackMaterial(material.id, target) }
                            )
                        }
                    }
                }

                // Error handling
                uiState.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
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
                            IconButton(onClick = viewModel::clearError) {
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
        }
    }
}

@Composable
fun MaterialItem(
    material: Material,
    onTrack: (Int) -> Unit
) {
    var showTrackDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Quantity: ${material.currentQuantity}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!material.isTracked) {
                Button(
                    onClick = { showTrackDialog = true },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Track")
                }
            }
        }
    }

    if (showTrackDialog) {
        TrackMaterialDialog(
            materialName = material.name,
            currentQuantity = material.currentQuantity,
            onDismiss = { showTrackDialog = false },
            onConfirm = { target ->
                onTrack(target)
                showTrackDialog = false
            }
        )
    }
}

@Composable
fun TrackedMaterialItem(
    material: Material,
    onStopTracking: () -> Unit,
    onUpdateTarget: (Int) -> Unit
) {
    var showUpdateDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = material.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium
                )

                Row {
                    IconButton(onClick = { showUpdateDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Target")
                    }
                    IconButton(onClick = onStopTracking) {
                        Icon(Icons.Default.Close, contentDescription = "Stop Tracking")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Current: ${material.currentQuantity} / Target: ${material.targetQuantity ?: "None"}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = material.progressPercentage() / 100f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (material.hasReachedTarget()) "Target reached!" else "Need ${material.remainingQuantity()} more",
                style = MaterialTheme.typography.bodySmall,
                color = if (material.hasReachedTarget()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (showUpdateDialog) {
        TrackMaterialDialog(
            materialName = material.name,
            currentQuantity = material.currentQuantity,
            initialTarget = material.targetQuantity,
            onDismiss = { showUpdateDialog = false },
            onConfirm = { target ->
                onUpdateTarget(target)
                showUpdateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMaterialDialog(
    materialName: String,
    currentQuantity: Int,
    initialTarget: Int? = null,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var targetText by remember { mutableStateOf(initialTarget?.toString() ?: "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Track ${materialName.replaceFirstChar { it.uppercase() }}") },
        text = {
            Column {
                Text("Current quantity: $currentQuantity")
                Spacer(modifier = Modifier.height(16.dp))
                Text("Enter target quantity:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { 
                        targetText = it
                        isError = it.isNotBlank() && (it.toIntOrNull() == null || it.toIntOrNull()!! <= 0)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Please enter a valid positive number")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetText.toIntOrNull()
                    if (target != null && target > 0) {
                        onConfirm(target)
                    } else {
                        isError = true
                    }
                },
                enabled = targetText.isNotBlank() && !isError
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
