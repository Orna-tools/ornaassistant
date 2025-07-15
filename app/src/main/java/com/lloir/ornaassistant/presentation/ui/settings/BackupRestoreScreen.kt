package com.lloir.ornaassistant.presentation.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.domain.model.BackupFrequency
import com.lloir.ornaassistant.presentation.ui.components.*
import com.lloir.ornaassistant.presentation.viewmodel.BackupRestoreViewModel
import java.time.format.DateTimeFormatter

@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val backupState by viewModel.backupState.collectAsState()
    val settings by viewModel.settings.collectAsState(initial = com.lloir.ornaassistant.domain.model.AppSettings())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Backup & Restore",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(adaptiveSpacing())
                .verticalScroll(rememberScrollState())
        ) {
            // Manual backup/restore section
            OrnaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OrnaSectionHeader(
                        title = "Manual Backup & Restore",
                        icon = Icons.Default.Save
                    )

                    Text(
                        "Create backups manually or restore from a previous backup file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OrnaButton(
                            onClick = { viewModel.createBackup() },
                            enabled = !backupState.isInProgress,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create Backup")
                        }

                        OrnaButton(
                            onClick = { viewModel.restoreBackup() },
                            enabled = !backupState.isInProgress,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Restore Backup")
                        }
                    }

                    if (backupState.isInProgress) {
                        Spacer(Modifier.height(16.dp))
                        OrnaProgressBar(
                            progress = 0.7f, // Indeterminate progress
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    backupState.lastBackupDate?.let {
                        Spacer(Modifier.height(12.dp))
                        OrnaInfoBox(
                            message = "Last backup: ${it.format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"))}",
                            type = InfoBoxType.INFO
                        )
                    }

                    backupState.error?.let {
                        Spacer(Modifier.height(12.dp))
                        OrnaInfoBox(
                            message = it,
                            type = InfoBoxType.WARNING
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Auto backup settings
            OrnaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OrnaSectionHeader(
                        title = "Automatic Backup Settings",
                        icon = Icons.Default.AutoMode
                    )

                    Text(
                        "Configure how and when automatic backups are created.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    SettingsSwitch(
                        title = "Auto Backup",
                        description = "Automatically backup your data",
                        checked = settings.autoBackupEnabled,
                        onCheckedChange = { viewModel.updateAutoBackupEnabled(it) }
                    )

                    FadeInContent(visible = settings.autoBackupEnabled) {
                        Column {
                            Divider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            SettingsDropdown(
                                title = "Backup Frequency",
                                description = "How often to create backups",
                                options = BackupFrequency.values().toList(),
                                selectedOption = settings.autoBackupFrequency,
                                onOptionSelected = { viewModel.updateAutoBackupFrequency(it) },
                                optionToString = {
                                    when (it) {
                                        BackupFrequency.DAILY -> "Daily"
                                        BackupFrequency.WEEKLY -> "Weekly"
                                        BackupFrequency.MONTHLY -> "Monthly"
                                        BackupFrequency.NEVER -> "Never"
                                    }
                                }
                            )

                            SettingsSlider(
                                title = "Number of Backups to Keep",
                                description = "Older backups will be deleted automatically",
                                value = settings.autoBackupRetention.toFloat(),
                                onValueChange = { viewModel.updateAutoBackupRetention(it.toInt()) },
                                valueRange = 1f..10f,
                                valueLabel = { "${it.toInt()} backups" }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Available backups section
            if (backupState.availableBackups.isNotEmpty()) {
                OrnaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        OrnaSectionHeader(
                            title = "Available Backups",
                            icon = Icons.Default.Backup
                        )

                        backupState.availableBackups.forEachIndexed { index, backup ->
                            BackupItem(
                                backup = backup,
                                isInProgress = backupState.isInProgress,
                                onRestore = { viewModel.restoreBackup(backup.uri) },
                                isFirst = index == 0
                            )

                            if (index < backupState.availableBackups.size - 1) {
                                Divider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
private fun BackupItem(
    backup: com.lloir.ornaassistant.data.backup.DataBackupService.BackupInfo,
    isInProgress: Boolean,
    onRestore: () -> Unit,
    isFirst: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(
                if (isFirst) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                backup.formattedDate, 
                style = MaterialTheme.typography.bodyMedium,
                color = if (isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                backup.formattedSize, 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OrnaButton(
            onClick = onRestore,
            enabled = !isInProgress
        ) {
            Icon(
                Icons.Default.Restore, 
                contentDescription = "Restore",
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Restore")
        }
    }
}
