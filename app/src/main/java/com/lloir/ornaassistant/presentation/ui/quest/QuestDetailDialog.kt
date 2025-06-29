package com.lloir.ornaassistant.presentation.ui.quest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lloir.ornaassistant.domain.model.Quest
import com.lloir.ornaassistant.domain.model.QuestObjective
import com.lloir.ornaassistant.domain.model.QuestRewards
import java.time.format.DateTimeFormatter

/**
 * Dialog for displaying detailed information about a quest.
 * 
 * This dialog shows:
 * - Quest name, description, and type
 * - Detailed objective information with progress tracking
 * - Reward information
 * - Actions for tracking and completing the quest
 * 
 * It allows users to update objective progress and complete the quest.
 */
@Composable
fun QuestDetailDialog(
    quest: Quest,
    onDismiss: () -> Unit,
    onTrackingToggle: (Boolean) -> Unit,
    onCompleteQuest: () -> Unit,
    onUpdateObjective: (Int, Int) -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with quest name and type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuestTypeIcon(quest.questType)

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = quest.name,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Description
                Text(
                    text = quest.description,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Status information
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Status: ${if (quest.isCompleted) "Completed" else "Active"}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        quest.startTime?.let {
                            Text(
                                text = "Started: ${dateFormatter.format(it)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        quest.completionTime?.let {
                            Text(
                                text = "Completed: ${dateFormatter.format(it)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        quest.expiryTime?.let {
                            Text(
                                text = "Expires: ${dateFormatter.format(it)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Tracking toggle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Switch(
                            checked = quest.isTracked,
                            onCheckedChange = onTrackingToggle
                        )
                        Text(
                            text = if (quest.isTracked) "Tracking" else "Not Tracking",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Objectives section
                Text(
                    text = "Objectives",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                quest.objectives.forEachIndexed { index, objective ->
                    ObjectiveItem(
                        objective = objective,
                        index = index,
                        onUpdateProgress = { amount -> onUpdateObjective(index, amount) },
                        isQuestCompleted = quest.isCompleted
                    )

                    if (index < quest.objectives.size - 1) {
                        Divider(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .padding(start = 32.dp)
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Rewards section
                Text(
                    text = "Rewards",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                RewardsSection(quest.rewards)

                Spacer(modifier = Modifier.height(16.dp))

                // Additional information
                if (quest.locationHint.isNotEmpty() || quest.questGiver.isNotEmpty() || quest.requiredLevel > 1) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Additional Information",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (quest.locationHint.isNotEmpty()) {
                                Text(
                                    text = "Location: ${quest.locationHint}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (quest.questGiver.isNotEmpty()) {
                                Text(
                                    text = "Quest Giver: ${quest.questGiver}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (quest.requiredLevel > 1) {
                                Text(
                                    text = "Required Level: ${quest.requiredLevel}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Close")
                    }

                    if (!quest.isCompleted) {
                        Button(
                            onClick = onCompleteQuest,
                            enabled = !quest.isCompleted
                        ) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Complete Quest")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Component for displaying and updating a quest objective.
 */
@Composable
fun ObjectiveItem(
    objective: QuestObjective,
    index: Int,
    onUpdateProgress: (Int) -> Unit,
    isQuestCompleted: Boolean
) {
    var showProgressDialog by remember { mutableStateOf(false) }
    val progress = objective.progressPercentage()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Objective icon
        Icon(
            imageVector = when (objective.type) {
                com.lloir.ornaassistant.domain.model.ObjectiveType.KILL_MONSTERS -> Icons.Default.Security
                com.lloir.ornaassistant.domain.model.ObjectiveType.COLLECT_ITEMS -> Icons.Default.Inventory
                com.lloir.ornaassistant.domain.model.ObjectiveType.VISIT_LOCATION -> Icons.Default.Place
                com.lloir.ornaassistant.domain.model.ObjectiveType.COMPLETE_DUNGEON -> Icons.Default.Castle
                com.lloir.ornaassistant.domain.model.ObjectiveType.DEFEAT_BOSS -> Icons.Default.Dangerous
                com.lloir.ornaassistant.domain.model.ObjectiveType.REACH_LEVEL -> Icons.Default.BarChart
                com.lloir.ornaassistant.domain.model.ObjectiveType.CRAFT_ITEM -> Icons.Default.Build
                com.lloir.ornaassistant.domain.model.ObjectiveType.TALK_TO_NPC -> Icons.Default.Person
                com.lloir.ornaassistant.domain.model.ObjectiveType.USE_SKILL -> Icons.Default.AutoFixHigh
                else -> Icons.Default.CheckCircle
            },
            contentDescription = "Objective Type: ${objective.type}",
            tint = if (objective.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp)
        )

        // Objective details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = objective.description,
                style = MaterialTheme.typography.bodyMedium
            )

            if (objective.targetAmount > 1) {
                Text(
                    text = "${objective.currentAmount}/${objective.targetAmount} (${progress}%)",
                    style = MaterialTheme.typography.bodySmall
                )

                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }

        // Update progress button (only for non-completed quests)
        if (!isQuestCompleted && !objective.isCompleted && objective.targetAmount > 1) {
            IconButton(onClick = { showProgressDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Update Progress"
                )
            }
        }
    }

    // Progress update dialog
    if (showProgressDialog) {
        var currentAmount by remember { mutableStateOf(objective.currentAmount.toString()) }

        AlertDialog(
            onDismissRequest = { showProgressDialog = false },
            title = { Text("Update Progress") },
            text = {
                Column {
                    Text("Current progress: ${objective.currentAmount}/${objective.targetAmount}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentAmount,
                        onValueChange = { value ->
                            // Only allow numeric input
                            if (value.isEmpty() || value.all { it.isDigit() }) {
                                currentAmount = value
                            }
                        },
                        label = { Text("New progress") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newAmount = currentAmount.toIntOrNull() ?: 0
                        // Ensure the amount is within valid range
                        val validAmount = newAmount.coerceIn(0, objective.targetAmount)
                        onUpdateProgress(validAmount)
                        showProgressDialog = false
                    }
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProgressDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Component for displaying quest rewards.
 */
@Composable
fun RewardsSection(rewards: QuestRewards) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Currency rewards
        if (rewards.orns > 0 || rewards.gold > 0 || rewards.experience > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (rewards.orns > 0) {
                    Text(
                        text = "Orns: ${rewards.orns}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }

                if (rewards.gold > 0) {
                    Text(
                        text = "Gold: ${rewards.gold}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }

                if (rewards.experience > 0) {
                    Text(
                        text = "XP: ${rewards.experience}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Item rewards
        if (rewards.items.isNotEmpty()) {
            Text(
                text = "Items:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            rewards.items.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                )
            }
        }

        // Skill rewards
        if (rewards.skills.isNotEmpty()) {
            Text(
                text = "Skills:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            rewards.skills.forEach { skill ->
                Text(
                    text = "• $skill",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                )
            }
        }

        // Special reward
        if (rewards.specialReward.isNotEmpty()) {
            Text(
                text = "Special Reward:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = rewards.specialReward,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
        }
    }
}
