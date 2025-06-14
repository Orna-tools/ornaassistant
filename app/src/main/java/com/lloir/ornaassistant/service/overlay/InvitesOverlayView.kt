package com.lloir.ornaassistant.service.overlay

import android.content.Context
import android.graphics.Color
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.lloir.ornaassistant.domain.usecase.PartyInviteInfo

class InvitesOverlayView(
    context: Context,
    windowManager: WindowManager
) : DraggableOverlayView(context, windowManager, "invites") {

    private var contentLayout: LinearLayout? = null

    override fun setupContent() {
        contentLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(4)
        }
        addView(contentLayout)
    }

    override fun updateContent(data: Any?) {
        contentLayout?.removeAllViews()

        when (data) {
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                val invites = data as? List<PartyInviteInfo> ?: return

                if (invites.isNotEmpty()) {
                    // Add header
                    addHeaderRow()

                    // Add invite rows
                    invites.forEach { invite ->
                        addInviteRow(invite)
                    }
                }
            }

            is String -> {
                // Simple text display
                val textView = TextView(context).apply {
                    text = data
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    setPadding(8)
                }
                contentLayout?.addView(textView)
            }

            else -> {
                // No data
                val textView = TextView(context).apply {
                    text = "No invites"
                    setTextColor(Color.GRAY)
                    textSize = 11f
                    setPadding(8)
                }
                contentLayout?.addView(textView)
            }
        }
    }

    private fun addHeaderRow() {
        val headerLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(Color.argb(128, 64, 64, 64))
            setPadding(4, 2, 4, 2)
        }

        val headers = listOf("Inviter", "N", "VoG", "D", "BG", "UW", "CG", "CD")
        val weights = floatArrayOf(2f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)

        headers.forEachIndexed { index, header ->
            val textView = TextView(context).apply {
                text = header
                setTextColor(Color.WHITE)
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    weights[index]
                )
            }
            headerLayout.addView(textView)
        }

        contentLayout?.addView(headerLayout)
    }

    private fun addInviteRow(invite: PartyInviteInfo) {
        val rowLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(4, 1, 4, 1)
        }

        // Inviter name
        TextView(context).apply {
            text = invite.inviterName
            setTextColor(Color.WHITE)
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                2f
            )
            rowLayout.addView(this)
        }

        // Dungeon counts
        val counts = listOf(
            invite.dungeonCounts.normal,
            invite.dungeonCounts.vog,
            invite.dungeonCounts.dragon,
            invite.dungeonCounts.bg,
            invite.dungeonCounts.underworld,
            invite.dungeonCounts.chaos
        )

        counts.forEach { count ->
            TextView(context).apply {
                text = count.toString()
                setTextColor(Color.WHITE)
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                rowLayout.addView(this)
            }
        }

        // Cooldown status
        TextView(context).apply {
            text = invite.cooldownStatus
            setTextColor(if (invite.isOnCooldown) Color.RED else Color.WHITE)
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            rowLayout.addView(this)
        }

        contentLayout?.addView(rowLayout)
    }
}