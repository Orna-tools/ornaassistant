package com.lloir.ornaassistant.presentation.ui.quest

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lloir.ornaassistant.domain.model.Quest
import com.lloir.ornaassistant.domain.model.QuestType
import java.time.format.DateTimeFormatter

/**
 * Card component for displaying a quest in a list.
 * 
 * This component shows:
 * - Quest name and description
 * - Quest type and progress
 * - Actions for tracking and completing the quest
 * 
 * It uses Material 3 design components for a modern look and feel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestCard(
    quest: Quest,
    onQuestClick: () -> Unit,
    onTrackingToggle: (Boolean) -> Unit,
    onCompleteQuest: () -> Unit
) {
    val progressPercent = quest.progressPercentage()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onQuestClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with quest name and tracking toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quest type icon
                QuestTypeIcon(quest.questType)
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Quest name
                Text(
                    text = quest.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Tracking toggle
                IconToggleButton(
                    checked = quest.isTracked,
                    onCheckedChange = onTrackingToggle
                ) {
                    Icon(
                        imageVector = if (quest.isTracked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (quest.isTracked) "Untrack Quest" else "Track Quest",
                        tint = if (quest.isTracked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Description
            Text(
                text = quest.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Progress bar
            LinearProgressIndicator(
                progress = { progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            
            // Footer with metadata and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quest type and progress
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quest.questType.name.lowercase().capitalize(),
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    Text(
                        text = if (quest.isCompleted) 
                            "Completed" + (quest.completionTime?.let { " on ${dateFormatter.format(it)}" } ?: "") 
                        else 
                            "Progress: $progressPercent%",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                // Complete button (only for active quests)
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
                        Text("Complete")
                    }
                }
            }
        }
    }
}

/**
 * Icon representing the quest type.
 */
@Composable
fun QuestTypeIcon(questType: QuestType) {
    val icon = when (questType) {
        QuestType.MAIN -> Icons.Default.Star
        QuestType.SIDE -> Icons.Default.Bookmark
        QuestType.DAILY -> Icons.Default.Today
        QuestType.WEEKLY -> Icons.Default.DateRange
        QuestType.EVENT -> Icons.Default.Event
        QuestType.KINGDOM -> Icons.Default.Group
        QuestType.ACHIEVEMENT -> Icons.Default.EmojiEvents
    }
    
    Icon(
        imageVector = icon,
        contentDescription = "Quest Type: ${questType.name}",
        tint = MaterialTheme.colorScheme.primary
    )
}

// Extension function to capitalize first letter
private fun String.capitalize(): String {
    return this.replaceFirstChar { it.uppercase() }
}