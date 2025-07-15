package com.lloir.ornaassistant.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.presentation.viewmodel.DataRetentionViewModel

@Composable
fun DataRetentionScreen(
    viewModel: DataRetentionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState(initial = com.lloir.ornaassistant.domain.model.AppSettings())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Retention") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Data Retention Settings",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Control how long your data is kept before being automatically deleted. Set to 0 to keep data indefinitely.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(16.dp))

                    SettingsSlider(
                        title = "Dungeon History Retention",
                        description = "Days to keep dungeon visit records",
                        value = settings.dungeonDataRetentionDays.toFloat(),
                        onValueChange = { viewModel.updateDungeonRetention(it.toInt()) },
                        valueRange = 0f..365f,
                        valueLabel = { 
                            if (it.toInt() == 0) "Forever" else "${it.toInt()} days" 
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    SettingsSlider(
                        title = "Item Assessment Retention",
                        description = "Days to keep item assessment records",
                        value = settings.assessmentDataRetentionDays.toFloat(),
                        onValueChange = { viewModel.updateAssessmentRetention(it.toInt()) },
                        valueRange = 0f..365f,
                        valueLabel = { 
                            if (it.toInt() == 0) "Forever" else "${it.toInt()} days" 
                        }
                    )

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { viewModel.showPruneDataDialog() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            enabled = !viewModel.isPruning
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Prune Data Now")
                        }
                    }

                    if (viewModel.isPruning) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        )
                    }

                    viewModel.pruneResult?.let { result ->
                        val message = when (result) {
                            is DataRetentionViewModel.PruneResult.Success -> result.message
                            is DataRetentionViewModel.PruneResult.Error -> result.message
                        }
                        val color = when (result) {
                            is DataRetentionViewModel.PruneResult.Success -> MaterialTheme.colorScheme.primary
                            is DataRetentionViewModel.PruneResult.Error -> MaterialTheme.colorScheme.error
                        }

                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = color,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }

            if (viewModel.showPruneDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissPruneDataDialog() },
                    title = { Text("Prune Old Data") },
                    text = { 
                        Text("This will permanently delete data older than your retention settings. This action cannot be undone.") 
                    },
                    confirmButton = {
                        Button(
                            onClick = { 
                                viewModel.pruneDataNow()
                                viewModel.dismissPruneDataDialog()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Prune Data")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { viewModel.dismissPruneDataDialog() }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
