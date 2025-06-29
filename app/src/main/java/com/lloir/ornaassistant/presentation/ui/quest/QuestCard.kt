package com.lloir.ornaassistant.presentation.ui.quest

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lloir.ornaassistant.domain.model.Quest
import com.lloir.ornaassistant.domain.model.QuestType
import java.time.format.DateTimeFormatter

/**
 * Enhanced card component for displaying a quest in a list.
 * 
 * Features:
 * - Animated progress indicator
 * - Visual cues for quest status (active, completed, expired)
 * - Improved layout and spacing
 * - Enhanced accessibility
 * - Subtle animations for better user experience
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

    // Animation for progress indicator
    val progressAnimation = remember(progressPercent) {
        Animatable(initialValue = 0f)
    }

    LaunchedEffect(progressPercent) {
        progressAnimation.animateTo(
            targetValue = progressPercent / 100f,
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            )
        )
    }

    // Card elevation animation on hover/press
    val cardElevation by animateDpAsState(
        targetValue = 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // Determine card color based on quest status
    val cardColors = when {
        quest.isCompleted -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
        quest.isTracked -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        )
        else -> CardDefaults.cardColors()
    }

    // Accessibility description
    val questDescription = buildString {
        append("Quest: ${quest.name}. ")
        append("Type: ${quest.questType.name}. ")
        append(if (quest.isCompleted) "Status: Completed. " else "Progress: $progressPercent%. ")
        if (quest.isTracked) append("Currently tracked. ")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics { contentDescription = questDescription },
        onClick = onQuestClick,
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = cardColors,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with quest name and tracking toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quest type icon with background
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    QuestTypeIcon(quest.questType)
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Quest name with status indicator
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quest.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Status chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        val statusColor = when {
                            quest.isCompleted -> MaterialTheme.colorScheme.tertiary
                            quest.isExpired() -> MaterialTheme.colorScheme.error
                            quest.isActive() -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        }

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = when {
                                quest.isCompleted -> "Completed"
                                quest.isExpired() -> "Expired"
                                quest.isActive() -> "Active"
                                else -> "Not Started"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }

                // Tracking toggle with animation
                IconToggleButton(
                    checked = quest.isTracked,
                    onCheckedChange = onTrackingToggle,
                    modifier = Modifier.semantics {
                        contentDescription = if (quest.isTracked) "Untrack Quest" else "Track Quest"
                    }
                ) {
                    val transition = updateTransition(quest.isTracked, label = "tracking")
                    val scale by transition.animateFloat(
                        label = "scale",
                        transitionSpec = { spring(stiffness = Spring.StiffnessLow) }
                    ) { isTracked -> if (isTracked) 1.2f else 1f }

                    Icon(
                        imageVector = if (quest.isTracked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = if (quest.isTracked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = quest.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Enhanced progress indicator
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background track
                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Animated progress
                LinearProgressIndicator(
                    progress = { progressAnimation.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    strokeCap = StrokeCap.Round,
                    color = when {
                        progressAnimation.value >= 1f -> MaterialTheme.colorScheme.tertiary
                        progressAnimation.value >= 0.7f -> MaterialTheme.colorScheme.primary
                        progressAnimation.value >= 0.3f -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )

                // Progress text
                Text(
                    text = "$progressPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(2.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer with metadata and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Quest type and completion info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = quest.questType.name.lowercase().capitalize(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (quest.isCompleted && quest.completionTime != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ${dateFormatter.format(quest.completionTime)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Next objective hint if not completed
                    if (!quest.isCompleted) {
                        quest.nextObjective()?.let { nextObjective ->
                            Text(
                                text = "Next: ${nextObjective.description}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Complete button with animation (only for active quests)
                AnimatedVisibility(
                    visible = !quest.isCompleted,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Button(
                        onClick = onCompleteQuest,
                        enabled = !quest.isCompleted,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.semantics {
                            contentDescription = "Complete Quest"
                        }
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
 * Enhanced icon representing the quest type.
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

    val iconColor = when (questType) {
        QuestType.MAIN -> MaterialTheme.colorScheme.primary
        QuestType.DAILY -> MaterialTheme.colorScheme.tertiary
        QuestType.EVENT -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    Icon(
        imageVector = icon,
        contentDescription = "Quest Type: ${questType.name}",
        tint = iconColor
    )
}

// Extension function to capitalize first letter
private fun String.capitalize(): String {
    return this.replaceFirstChar { it.uppercase() }
}
