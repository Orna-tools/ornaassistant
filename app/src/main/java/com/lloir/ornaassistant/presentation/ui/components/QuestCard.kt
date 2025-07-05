package com.lloir.ornaassistant.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Quest status enum
 */
enum class QuestStatus {
    ACTIVE,
    COMPLETED,
    EXPIRED
}

/**
 * Quest data model
 */
data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val progress: Float, // 0.0f to 1.0f
    val status: QuestStatus,
    val reward: String,
    val timeRemaining: String? = null // Only for active quests
)

/**
 * A modern, animated quest card component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestCard(
    quest: Quest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (quest.status) {
        QuestStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        QuestStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        QuestStatus.EXPIRED -> MaterialTheme.colorScheme.error
    }
    
    val statusIcon = when (quest.status) {
        QuestStatus.ACTIVE -> Icons.Default.Schedule
        QuestStatus.COMPLETED -> Icons.Default.CheckCircle
        QuestStatus.EXPIRED -> Icons.Default.HourglassEmpty
    }
    
    val statusText = when (quest.status) {
        QuestStatus.ACTIVE -> "Active"
        QuestStatus.COMPLETED -> "Completed"
        QuestStatus.EXPIRED -> "Expired"
    }
    
    // Animation for progress indicator
    val progressAnimation = remember(quest.id, quest.progress) {
        Animatable(initialValue = 0f)
    }
    
    LaunchedEffect(quest.id, quest.progress) {
        progressAnimation.animateTo(
            targetValue = quest.progress,
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    // Pulse animation for active quests
    val pulseAnimation = remember { Animatable(1f) }
    LaunchedEffect(quest.status) {
        if (quest.status == QuestStatus.ACTIVE) {
            while (true) {
                pulseAnimation.animateTo(
                    targetValue = 0.7f,
                    animationSpec = tween(700, easing = LinearEasing)
                )
                pulseAnimation.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(700, easing = LinearEasing)
                )
                delay(1000)
            }
        } else {
            pulseAnimation.snapTo(1f)
        }
    }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Quest: ${quest.title}, Status: $statusText, Progress: ${(quest.progress * 100).toInt()}%"
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Status indicator and title row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.1f))
                        .alpha(if (quest.status == QuestStatus.ACTIVE) pulseAnimation.value else 1f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = statusText,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Title and status
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
                
                // Time remaining for active quests
                if (quest.status == QuestStatus.ACTIVE && quest.timeRemaining != null) {
                    Text(
                        text = quest.timeRemaining,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            Text(
                text = quest.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress indicator
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Text(
                        text = "${(progressAnimation.value * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                LinearProgressIndicator(
                    progress = { progressAnimation.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Reward
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "Reward: ${quest.reward}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Completion animation
            if (quest.status == QuestStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(8.dp))
                
                val completionAnimation = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    completionAnimation.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 1000,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
                
                AnimatedVisibility(
                    visible = completionAnimation.value > 0.5f,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = "Quest completed!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuestCardPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuestCard(
                quest = Quest(
                    id = "1",
                    title = "Defeat 10 Monsters",
                    description = "Defeat 10 monsters in the wild to earn rewards.",
                    progress = 0.7f,
                    status = QuestStatus.ACTIVE,
                    reward = "500 Orns, 1000 Gold",
                    timeRemaining = "2h 30m"
                ),
                onClick = {}
            )
            
            QuestCard(
                quest = Quest(
                    id = "2",
                    title = "Collect 5 Materials",
                    description = "Gather 5 different materials from the world.",
                    progress = 1.0f,
                    status = QuestStatus.COMPLETED,
                    reward = "Rare Item, 2000 Gold"
                ),
                onClick = {}
            )
            
            QuestCard(
                quest = Quest(
                    id = "3",
                    title = "Explore New Areas",
                    description = "Discover 3 new areas on the map.",
                    progress = 0.33f,
                    status = QuestStatus.EXPIRED,
                    reward = "Map Fragment, 300 Orns"
                ),
                onClick = {}
            )
        }
    }
}