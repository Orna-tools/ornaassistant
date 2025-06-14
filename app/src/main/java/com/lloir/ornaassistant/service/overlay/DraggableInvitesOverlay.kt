package com.lloir.ornaassistant.service.overlay

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.lloir.ornaassistant.domain.usecase.PartyInviteInfo

class DraggableInvitesOverlay(
    context: Context,
    windowManager: WindowManager
) : DraggableOverlayView(context, windowManager, "invites") {

    private var headerLayout: LinearLayout? = null
    private var invitesLayout: LinearLayout? = null

    override fun setupContent() {
        // Header row
        headerLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(Color.parseColor("#80333333"))
            setPadding(4, 2, 4, 2)
            
            addView(createHeaderText("Inviter", 2f))
            addView(createHeaderText("N", 1f))
            addView(createHeaderText("VoG", 1f))
            addView(createHeaderText("D", 1f))
            addView(createHeaderText("BG", 1f))
            addView(createHeaderText("UW", 1f))
            addView(createHeaderText("CG", 1f))
            addView(createHeaderText("CD", 1f))
        }
        addView(headerLayout)
        
        // Invites container
        invitesLayout = LinearLayout(context).apply {
            orientation = VERTICAL
        }
        addView(invitesLayout)
    }
    
    override fun updateContent(data: Any?) {
        if (data !is List<*>) return
        
        invitesLayout?.removeAllViews()
        
        @Suppress("UNCHECKED_CAST")
        val invites = data as List<PartyInviteInfo>
        
        invites.forEach { invite ->
            val row = createInviteRow(invite)
            invitesLayout?.addView(row)
        }
    }
    
    private fun createHeaderText(text: String, weight: Float): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
            gravity = Gravity.CENTER
        }
    }
    
    private fun createInviteRow(invite: PartyInviteInfo): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(4, 1, 4, 1)
            
            // Inviter name
            addView(TextView(context).apply {
                text = invite.inviterName
                setTextColor(Color.WHITE)
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            })
            
            // Dungeon counts
            addView(createCountText(invite.dungeonCounts.normal))
            addView(createCountText(invite.dungeonCounts.vog))
            addView(createCountText(invite.dungeonCounts.dragon))
            addView(createCountText(invite.dungeonCounts.bg))
            addView(createCountText(invite.dungeonCounts.underworld))
            addView(createCountText(invite.dungeonCounts.chaos))
            
            // Cooldown status
            addView(TextView(context).apply {
                text = invite.cooldownStatus
                setTextColor(if (invite.isOnCooldown) Color.RED else Color.GREEN)
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
            })
        }
    }
    
    private fun createCountText(count: Int): TextView {
        return TextView(context).apply {
            text = count.toString()
            setTextColor(Color.WHITE)
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
        }
    }
    
    override fun onStartDragging() {
        super.onStartDragging()
        setBackgroundColor(Color.parseColor("#44FFFFFF"))
    }
    
    override fun onStopDragging() {
        super.onStopDragging()
        setBackgroundColor(Color.BLACK)
    }
}
