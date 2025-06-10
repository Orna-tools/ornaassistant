package com.lloir.ornaassistant.presentation.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lloir.ornaassistant.domain.model.ParsedScreen

@Composable
fun DraggableOverlay(
    onMove: (deltaX: Float, deltaY: Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        // If drag distance is very small, treat as click to dismiss
                    }
                ) { change, dragAmount ->
                    if (isDragging) {
                        onMove(dragAmount.x, dragAmount.y)
                    }
                }
            }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            )
        ) {
            content()
        }
    }
}

@Composable
fun SessionOverlayContent(parsedScreen: ParsedScreen) {
    // This would connect to actual ViewModels in real implementation
    Column(
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = "Session Stats",
            color = Color.White,
            fontSize = 12.sp,
            style = MaterialTheme.typography.labelMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Orns", color = Color.White, fontSize = 10.sp)
                Text("0", color = Color.White, fontSize = 10.sp)
            }
            Column {
                Text("Gold", color = Color.White, fontSize = 10.sp)
                Text("0", color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun InvitesOverlayContent(parsedScreen: ParsedScreen) {
    // Parse invite data from screen
    val invites = remember(parsedScreen) {
        parseInvites(parsedScreen)
    }

    if (invites.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier.padding(4.dp)
        ) {
            item {
                // Header row
                InviteHeaderRow()
            }

            items(invites) { invite ->
                InviteRow(invite)
            }
        }
    }
}

@Composable
fun AssessOverlayContent(parsedScreen: ParsedScreen) {
    // This would connect to assessment ViewModel
    Column(
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = "Item Assessment",
            color = Color.White,
            fontSize = 12.sp
        )

        // Assessment data would be displayed here
        Text(
            text = "Quality: calculating...",
            color = Color.White,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun InviteHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray.copy(alpha = 0.8f))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Inviter", color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(2f))
        Text("N", color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text("VoG", color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text("D", color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text("BG", color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text("UW", color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text("CG", color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text("CD", color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun InviteRow(invite: InviteData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(invite.name, color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(2f))
        Text(invite.normal.toString(), color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(invite.vog.toString(), color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(invite.dragon.toString(), color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(invite.bg.toString(), color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(invite.uw.toString(), color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(invite.cg.toString(), color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(
            text = invite.cooldown,
            color = if (invite.isOnCooldown) Color.Red else Color.White,
            fontSize = 10.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// Helper functions and data classes
private fun parseInvites(parsedScreen: ParsedScreen): List<InviteData> {
    // Parse screen data to extract party invites
    val invites = mutableListOf<InviteData>()

    parsedScreen.data.forEach { screenData ->
        if (screenData.text.contains("invited you", ignoreCase = true)) {
            val inviterName = screenData.text.replace(" has invited you to their party.", "")
            invites.add(
                InviteData(
                    name = inviterName,
                    normal = 0,
                    vog = 0,
                    dragon = 0,
                    bg = 0,
                    uw = 0,
                    cg = 0,
                    cooldown = "Ready",
                    isOnCooldown = false
                )
            )
        }
    }

    return invites
}

data class InviteData(
    val name: String,
    val normal: Int,
    val vog: Int,
    val dragon: Int,
    val bg: Int,
    val uw: Int,
    val cg: Int,
    val cooldown: String,
    val isOnCooldown: Boolean
)