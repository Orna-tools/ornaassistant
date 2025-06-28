package com.lloir.ornaassistant.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.domain.usecase.SendDebugLogsUseCase
import com.lloir.ornaassistant.presentation.viewmodel.SettingsViewModel
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    // Debug log submission state
    val debugLogResult by viewModel.debugLogResult.collectAsState()
    val isSubmittingLogs by viewModel.isSubmittingLogs.collectAsState()
    var showDebugDialog by remember { mutableStateOf(false) }

    // Debug log submission state
    val debugLogResult by viewModel.debugLogResult.collectAsState()
    val isSubmittingLogs by viewModel.isSubmittingLogs.collectAsState()
    var showDebugDialog by remember { mutableStateOf(false) }

    // Debug log submission state
    val debugLogResult by viewModel.debugLogResult.collectAsState()
    val isSubmittingLogs by viewModel.isSubmittingLogs.collectAsState()
    var showDebugDialog by remember { mutableStateOf(false) }

    // Debug log submission state
    val debugLogResult by viewModel.debugLogResult.collectAsState()
    val isSubmittingLogs by viewModel.isSubmittingLogs.collectAsState()
    var showDebugDialog by remember { mutableStateOf(false) }

    // Debug log submission state
    val debugLogResult by viewModel.debugLogResult.collectAsState()
    val isSubmittingLogs by viewModel.isSubmittingLogs.collectAsState()
    var showDebugDialog by remember { mutableStateOf(false) }

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

            // Debug Section
            SettingsSection(title = "Debug") {
                SettingsSwitch(
                    title = "Debug Mode",
                    description = "Enable debug features and detailed logging",
                    checked = settings.debugMode,
                    onCheckedChange = viewModel::updateDebugMode
                )

                if (settings.debugMode) {
                    SettingsButton(
                        title = "Send Debug Logs",
                        description = "Submit logs to help developers debug issues",
                        icon = Icons.Default.BugReport,
                        enabled = !isSubmittingLogs,
                        onClick = { showDebugDialog = true }
                    )
                }
            }

            // Debug Section
            SettingsSection(title = "Debug") {
                SettingsSwitch(
                    title = "Debug Mode",
                    description = "Enable debug features and detailed logging",
                    checked = settings.debugMode,
                    onCheckedChange = viewModel::updateDebugMode
                )

                if (settings.debugMode) {
                    SettingsButton(
                        title = "Send Debug Logs",
                        description = "Submit logs to help developers debug issues",
                        icon = Icons.Default.BugReport,
                        enabled = !isSubmittingLogs,
                        onClick = { showDebugDialog = true }
                    )
                }
            }

            // Debug Section
            SettingsSection(title = "Debug") {
                SettingsSwitch(
                    title = "Debug Mode",
                    description = "Enable debug features and detailed logging",
                    checked = settings.debugMode,
                    onCheckedChange = viewModel::updateDebugMode
                )

                if (settings.debugMode) {
                    SettingsButton(
                        title = "Send Debug Logs",
                        description = "Submit logs to help developers debug issues",
                        icon = Icons.Default.BugReport,
                        enabled = !isSubmittingLogs,
                        onClick = { showDebugDialog = true }
                    )
                }
            }

            // Debug Section
            SettingsSection(title = "Debug") {
                SettingsSwitch(
                    title = "Debug Mode",
                    description = "Enable debug features and detailed logging",
                    checked = settings.debugMode,
                    onCheckedChange = viewModel::updateDebugMode
                )

                if (settings.debugMode) {
                    SettingsButton(
                        title = "Send Debug Logs",
                        description = "Submit logs to help developers debug issues",
                        icon = Icons.Default.BugReport,
                        enabled = !isSubmittingLogs,
                        onClick = { showDebugDialog = true }
                    )
                }
            }

            // Debug Section
            SettingsSection(title = "Debug") {
                SettingsSwitch(
                    title = "Debug Mode",
                    description = "Enable debug features and detailed logging",
                    checked = settings.debugMode,
                    onCheckedChange = viewModel::updateDebugMode
                )

                if (settings.debugMode) {
                    SettingsButton(
                        title = "Send Debug Logs",
                        description = "Submit logs to help developers debug issues",
                        icon = Icons.Default.BugReport,
                        enabled = !isSubmittingLogs,
                        onClick = { showDebugDialog = true }
                    )
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

        // Debug log submission dialog
        if (showDebugDialog) {
            DebugLogSubmissionDialog(
                onDismiss = {
                    showDebugDialog = false
                    viewModel.resetSubmissionState()
                },
                onSubmit = { description, email ->
                    viewModel.submitDebugLogs(description, email)
                },
                isSubmitting = isSubmittingLogs
            )
        }

        // Handle debug log submission results
        LaunchedEffect(debugLogResult) {
            debugLogResult?.let { result ->
                when (result) {
                    is SendDebugLogsUseCase.Result.Success -> {
                        // Success - close dialog and show feedback
                        showDebugDialog = false
                        // You could show a snackbar here
                        viewModel.clearDebugLogResult()
                    }
                    is SendDebugLogsUseCase.Result.Error -> {
                        // Error - keep dialog open but show error
                        // The error will be shown in the dialog
                        if (!showDebugDialog) {
                            viewModel.clearDebugLogResult()
                        }
                    }
                }
            }
        }

        // Debug log submission dialog
        if (showDebugDialog) {
            DebugLogSubmissionDialog(
                onDismiss = { showDebugDialog = false },
                onSubmit = { description, email ->
                    viewModel.submitDebugLogs(description, email)
                    showDebugDialog = false
                },
                isSubmitting = isSubmittingLogs
            )
        }

        // Show result of debug log submission
        LaunchedEffect(debugLogResult) {
            debugLogResult?.let { result ->
                when (result) {
                    is SendDebugLogsUseCase.Result.Success -> {
                        // Could show a success snackbar here
                    }
                    is SendDebugLogsUseCase.Result.Error -> {
                        // Could show an error snackbar here
                    }
                }
            }
        }

        // Debug log submission dialog
        if (showDebugDialog) {
            DebugLogSubmissionDialog(
                onDismiss = { showDebugDialog = false },
                onSubmit = { description, email ->
                    viewModel.submitDebugLogs(description, email)
                    showDebugDialog = false
                },
                isSubmitting = isSubmittingLogs
            )
        }

        // Show result of debug log submission
        LaunchedEffect(debugLogResult) {
            debugLogResult?.let { result ->
                when (result) {
                    is SendDebugLogsUseCase.Result.Success -> {
                        // Could show a success snackbar here
                    }
                    is SendDebugLogsUseCase.Result.Error -> {
                        // Could show an error snackbar here
                    }
                }
            }
        }

        // Debug log submission dialog
        if (showDebugDialog) {
            DebugLogSubmissionDialog(
                onDismiss = { showDebugDialog = false },
                onSubmit = { description, email ->
                    viewModel.submitDebugLogs(description, email)
                    showDebugDialog = false
                },
                isSubmitting = isSubmittingLogs
            )
        }

        // Show result of debug log submission
        LaunchedEffect(debugLogResult) {
            debugLogResult?.let { result ->
                when (result) {
                    is SendDebugLogsUseCase.Result.Success -> {
                        // Could show a success snackbar here
                    }
                    is SendDebugLogsUseCase.Result.Error -> {
                        // Could show an error snackbar here
                    }
                }
            }
        }

        // Debug log submission dialog
        if (showDebugDialog) {
            DebugLogSubmissionDialog(
                onDismiss = { showDebugDialog = false },
                onSubmit = { description, email ->
                    viewModel.submitDebugLogs(description, email)
                    showDebugDialog = false
                },
                isSubmitting = isSubmittingLogs
            )
        }

        // Show result of debug log submission
        LaunchedEffect(debugLogResult) {
            debugLogResult?.let { result ->
                when (result) {
                    is SendDebugLogsUseCase.Result.Success -> {
                        // Could show a success snackbar here
                    }
                    is SendDebugLogsUseCase.Result.Error -> {
                        // Could show an error snackbar here
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
private fun SettingsButton(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun DebugLogSubmissionDialog(
    onDismiss: () -> Unit,
    onSubmit: (description: String, email: String?) -> Unit,
    isSubmitting: Boolean
) {
    var description by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Send Debug Logs",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Please describe the issue you're experiencing. This will help developers understand and fix the problem.",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Describe the issue *") },
                    placeholder = { Text("e.g., App crashes when I enter a dungeon...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    placeholder = { Text("your.email@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Note: Logs will be posted as a public GitHub issue. Don't include sensitive information.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { 
                            if (description.isNotBlank()) {
                                onSubmit(description, email.takeIf { it.isNotBlank() })
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting && description.isNotBlank()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send Logs")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsButton(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun DebugLogSubmissionDialog(
    onDismiss: () -> Unit,
    onSubmit: (description: String, email: String?) -> Unit,
    isSubmitting: Boolean
) {
    var description by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Send Debug Logs",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Please describe the issue you're experiencing. This will help developers understand and fix the problem.",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Describe the issue *") },
                    placeholder = { Text("e.g., App crashes when I enter a dungeon...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    placeholder = { Text("your.email@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Note: Logs will be posted as a public GitHub issue. Don't include sensitive information.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { 
                            if (description.isNotBlank()) {
                                onSubmit(description, email.takeIf { it.isNotBlank() })
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting && description.isNotBlank()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send Logs")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsButton(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun DebugLogSubmissionDialog(
    onDismiss: () -> Unit,
    onSubmit: (description: String, email: String?) -> Unit,
    isSubmitting: Boolean
) {
    var description by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Send Debug Logs",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Please describe the issue you're experiencing. This will help developers understand and fix the problem.",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Describe the issue *") },
                    placeholder = { Text("e.g., App crashes when I enter a dungeon...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    placeholder = { Text("your.email@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Note: Logs will be posted as a public GitHub issue. Don't include sensitive information.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { 
                            if (description.isNotBlank()) {
                                onSubmit(description, email.takeIf { it.isNotBlank() })
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting && description.isNotBlank()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send Logs")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsButton(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun DebugLogSubmissionDialog(
    onDismiss: () -> Unit,
    onSubmit: (description: String, email: String?) -> Unit,
    isSubmitting: Boolean
) {
    var description by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Send Debug Logs",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Please describe the issue you're experiencing. This will help developers understand and fix the problem.",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Describe the issue *") },
                    placeholder = { Text("e.g., App crashes when I enter a dungeon...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    placeholder = { Text("your.email@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Note: Logs will be posted as a public GitHub issue. Don't include sensitive information.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (description.isNotBlank()) {
                                onSubmit(description, email.takeIf { it.isNotBlank() })
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting && description.isNotBlank()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send Logs")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsButton(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick diff --git a/app/src/main/AndroidManifest.xml b/app/src/main/AndroidManifest.xml

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