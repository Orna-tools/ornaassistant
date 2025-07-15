package com.lloir.ornaassistant.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.presentation.viewmodel.PerformanceSettingsViewModel

@Composable
fun PerformanceSettingsScreen(
    viewModel: PerformanceSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState(initial = AppSettings())
    val memoryUsage by viewModel.memoryUsage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Settings") },
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
            // Battery Saver Mode
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Battery Optimization",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(8.dp))

                    SettingsSwitch(
                        title = "Battery Saver Mode",
                        description = "Reduce update frequency and background activity to save battery",
                        checked = settings.batterySaverMode,
                        onCheckedChange = { viewModel.updateBatterySaverMode(it) }
                    )

                    if (settings.batterySaverMode) {
                        Text(
                            "Battery saver mode is active. The app will update less frequently and some background features may be limited.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Offline Mode
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Network Settings",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(8.dp))

                    SettingsSwitch(
                        title = "Offline Mode",
                        description = "Disable network features and use cached data",
                        checked = settings.offlineMode,
                        onCheckedChange = { viewModel.updateOfflineMode(it) }
                    )

                    if (settings.offlineMode) {
                        Text(
                            "Offline mode is active. External item data and updates will not be available.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Memory Usage
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Memory Optimization",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(8.dp))

                    SettingsSwitch(
                        title = "Low Memory Mode",
                        description = "Reduce memory usage for older devices",
                        checked = settings.lowMemoryMode,
                        onCheckedChange = { viewModel.updateLowMemoryMode(it) }
                    )

                    if (settings.lowMemoryMode) {
                        Text(
                            "Low memory mode is active. Some features may have reduced functionality.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Current Memory Usage",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.weight(1f))

                        Text(
                            memoryUsage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.clearMemoryCache() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Clear Cache")
                    }
                }
            }
        }
    }
}
