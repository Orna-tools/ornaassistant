package com.lloir.ornaassistant.service.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.lloir.ornaassistant.domain.model.DungeonMode
import com.lloir.ornaassistant.domain.model.DungeonVisit
import com.lloir.ornaassistant.domain.model.WayvesselSession

class DraggableSessionOverlay(
    private val service: AccessibilityService,
    private val windowManager: WindowManager
) : LinearLayout(service) {

    private var layoutParams: WindowManager.LayoutParams? = null
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var clickStartTime = 0L
    
    private var contentView: TextView? = null

    companion object {
        private const val CLICK_DURATION_MS = 200L
        private const val DRAG_THRESHOLD = 10f
        private const val TAG = "SessionOverlay"
    }
    
    fun create() {
        setupLayout()
        setupTouchHandling()
        addToWindow()
    }
    
    private fun setupLayout() {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.BLACK)
        alpha = 0.8f
        setPadding(16, 16, 16, 16)
        elevation = 10f

        contentView = TextView(service).apply {
            setTextColor(Color.WHITE)
            textSize = 11f
            setPadding(8, 8, 8, 8)
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }
        addView(contentView)
    }
    
    private fun setupTouchHandling() {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x?.toFloat() ?: 0f
                    initialY = layoutParams?.y?.toFloat() ?: 0f
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    clickStartTime = System.currentTimeMillis()
                    true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    
                    if (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD) {
                        isDragging = true
                        
                        layoutParams?.let { params ->
                            params.x = (initialX + deltaX).toInt()
                            params.y = (initialY + deltaY).toInt()
                            windowManager.updateViewLayout(this, params)
                        }
                    }
                    true
                }
                
                MotionEvent.ACTION_UP -> {
                    val clickDuration = System.currentTimeMillis() - clickStartTime
                    
                    if (!isDragging && clickDuration < CLICK_DURATION_MS) {
                        // This was a tap - close the overlay
                        Log.d(TAG, "Tap detected - closing overlay")
                        dismiss()
                    }
                    
                    isDragging = false
                    true
                }
                
                else -> false
            }
        }
    }
    
    private fun addToWindow() {
        layoutParams = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.TOP or Gravity.LEFT
            format = PixelFormat.TRANSPARENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            x = 0
            y = 470
        }
        
        windowManager.addView(this, layoutParams)
        isVisible = true
    }

    fun updateContent(data: Any?) {
        val (wayvesselSession, dungeonVisit) = data as? Pair<WayvesselSession?, DungeonVisit?> ?: return

        val displayText = buildString {
            if (wayvesselSession != null) {
                appendLine("@${wayvesselSession.name}")
                if (wayvesselSession.dungeonsVisited > 1) {
                    append("Session: ${formatNumber(wayvesselSession.orns)} orns")
                    if (dungeonVisit?.mode?.type != DungeonMode.Type.ENDLESS) {
                        append(", ${formatNumber(wayvesselSession.gold)} gold")
                    } else {
                        append(", ${formatNumber(wayvesselSession.experience)} exp")
                    }
                    appendLine()
                }
            }
            if (dungeonVisit != null) {
                append("${dungeonVisit.name} ${dungeonVisit.mode}")
                if (dungeonVisit.floor > 0) {
                    append(" Floor ${dungeonVisit.floor}")
                }
                appendLine()
                append("${formatNumber(dungeonVisit.orns)} orns")
                if (dungeonVisit.mode.type == DungeonMode.Type.ENDLESS) {
                    append(", ${formatNumber(dungeonVisit.experience)} exp")
                } else {
                    append(", ${formatNumber(dungeonVisit.gold)} gold")
                }
                if (dungeonVisit.godforges > 0) {
                    append(" [GF: ${dungeonVisit.godforges}]")
                }
            }
        }

        contentView?.text = displayText
    }

    private fun formatNumber(value: Long): String {
        return when {
            value >= 1_000_000 -> "%.1f m".format(value / 1_000_000.0)
            value >= 1_000 -> "%.1f k".format(value / 1_000.0)
            else -> value.toString()
        }
    }

    fun dismiss() {
        try {
            windowManager.removeView(this)
        } catch (e: Exception) {
            Log.w(TAG, "Error removing overlay", e)
        }
    }
}
