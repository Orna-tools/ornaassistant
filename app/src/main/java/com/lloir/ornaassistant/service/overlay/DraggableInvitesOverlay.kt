package com.lloir.ornaassistant.service.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.Log
import android.view.View // Import View for isVisible
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.lloir.ornaassistant.domain.usecase.PartyInviteInfo

class DraggableInvitesOverlay(
    private val service: AccessibilityService,
    private val windowManager: WindowManager
) : LinearLayout(service) {

    private lateinit var layoutParams: WindowManager.LayoutParams // Use lateinit
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var clickStartTime = 0L
    
    private var headerRow: LinearLayout? = null
    private var contentLayout: LinearLayout? = null // This will be toggled
    
    companion object {
        private const val CLICK_DURATION_MS = 200L
        private const val DRAG_THRESHOLD = 10f
        private const val TAG = "InvitesOverlay"
    }
    
    fun create() {
        setupLayout()
        setupTouchHandling()
        addToWindow()
    }

    private fun setupLayout() {
        orientation = VERTICAL
        setBackgroundColor(Color.BLACK)
        alpha = 0.8f
        setPadding(8, 8, 8, 8)
        
        // Header row
        headerRow = LinearLayout(context).apply { // Corrected variable name
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
        addView(headerRow)
        
        // Content layout for invite rows
        contentLayout = LinearLayout(service).apply {
            orientation = VERTICAL
            isVisible = true // Initially visible, can be toggled
        }
        addView(contentLayout)
    }
    
    private fun createHeaderText(text: String, weight: Float): TextView {
        return TextView(service).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 10f
            this.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight) // Corrected
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
            addView(TextView(service).apply {
                text = invite.cooldownStatus
                setTextColor(if (invite.isOnCooldown) Color.RED else Color.WHITE)
                textSize = 10f
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
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
    
    // Removed 'override' keyword
    private fun onStartDragging() {
        // super.onStartDragging() // Removed as there's no super method
        setBackgroundColor(Color.parseColor("#44FFFFFF"))
    }
    
    // Removed 'override' keyword
    private fun onStopDragging() {
        // super.onStopDragging() // Removed as there's no super method
        setBackgroundColor(Color.BLACK)
    }

    // Add the updateContent method
    fun updateContent(inviteInfoList: List<PartyInviteInfo>) {
        contentLayout?.let { layout ->
            layout.removeAllViews() // Clear previous invites

            if (inviteInfoList.isEmpty()) {
                // Optionally, add a TextView to indicate no invites
                val noInvitesView = TextView(service).apply {
                    text = "No active party invites."
                    setTextColor(Color.GRAY)
                    textSize = 10f
                    setPadding(0, 4, 0, 4)
                    gravity = Gravity.CENTER
                }
                layout.addView(noInvitesView)
                return@let
            }

            inviteInfoList.forEach { inviteInfo ->
                layout.addView(createInviteRow(inviteInfo))
            }
        }
    }

    private fun setupTouchHandling() {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x.toFloat()
                    initialY = layoutParams.y.toFloat()
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    clickStartTime = System.currentTimeMillis()
                    onStartDragging() // Call if you want visual feedback on press
                    true // Consume the event
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY

                    if (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD) {
                        if (!isDragging) { // Only call onStartDragging once
                            isDragging = true
                            // onStartDragging() // Already called on ACTION_DOWN if preferred
                        }
                        layoutParams.x = (initialX + deltaX).toInt()
                        layoutParams.y = (initialY + deltaY).toInt()
                        windowManager.updateViewLayout(this, layoutParams)
                    }
                    true // Consume the event
                }
                MotionEvent.ACTION_UP -> {
                    val clickDuration = System.currentTimeMillis() - clickStartTime
                    if (!isDragging && clickDuration < CLICK_DURATION_MS) {
                        // It's a click, toggle content visibility
                        contentLayout?.let {
                            it.isVisible = !it.isVisible
                        }
                        Log.d(TAG, "Click detected. Content visibility: ${contentLayout?.isVisible}")
                    }
                    if (isDragging) { // Check if it was dragging before resetting
                        onStopDragging() // Call if you want to remove visual feedback
                    }
                    isDragging = false
                    true // Consume the event
                }
                else -> false // Don't consume other events
            }
        }
    }

    private fun addToWindow() {
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100 // Initial X position
            y = 100 // Initial Y position
        }
        windowManager.addView(this, layoutParams)
    }

    // Add the dismiss function
    fun dismiss() {
        try {
            if (isAttachedToWindow) { // Check if the view is still attached
                windowManager.removeView(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing invites overlay", e)
        }
    }
}
