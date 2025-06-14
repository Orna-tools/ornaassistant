package com.lloir.ornaassistant.service.overlay

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import com.lloir.ornaassistant.utils.OverlayCompat
import androidx.core.view.isVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class DraggableOverlayView(
    passedContext: Context,
    protected val windowManager: WindowManager,
    protected val overlayType: String
) : LinearLayout(passedContext) {

    protected var layoutParams: WindowManager.LayoutParams? = null
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var touchStartTime = 0L
    private var hasMoved = false

    companion object {
        private const val TAG = "DraggableOverlay"
        private const val TAP_DURATION_MS = 200L
        private const val DRAG_THRESHOLD = 10f
        private const val LONG_PRESS_DURATION_MS = 300L
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.BLACK)
        alpha = 0.8f
        setPadding(12, 8, 12, 8)
        elevation = 10f
    }

    abstract fun setupContent()
    abstract fun updateContent(data: Any?)

    fun create() {
        setupContent()
        setupTouchHandling()
        addToWindow()
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
                    hasMoved = false
                    touchStartTime = System.currentTimeMillis()

                    // Start checking for long press
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(LONG_PRESS_DURATION_MS)
                        if (!hasMoved && !isDragging) {
                            isDragging = true
                            onStartDragging()
                        }
                    }
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY

                    if (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD) {
                        hasMoved = true

                        if (isDragging) {
                            layoutParams?.let { params ->
                                params.x = (initialX + deltaX).toInt()
                                params.y = (initialY + deltaY).toInt()
                                windowManager.updateViewLayout(this, params)
                            }
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - touchStartTime

                    when {
                        // Quick tap - dismiss
                        !hasMoved && duration < TAP_DURATION_MS -> {
                            Log.d(TAG, "Tap detected - dismissing overlay")
                            dismiss()
                        }
                        // Was dragging - save position
                        isDragging -> {
                            savePosition()
                            onStopDragging()
                        }
                    }

                    isDragging = false
                    hasMoved = false
                    true
                }

                else -> false
            }
        }
    }

    private fun addToWindow() {
        val savedPosition = loadPosition()
        
        layoutParams = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = OverlayCompat.getOverlayType()
            gravity = Gravity.TOP or Gravity.LEFT
            format = PixelFormat.TRANSPARENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            x = savedPosition.first
            y = savedPosition.second
        }

        windowManager.addView(this, layoutParams)
        isVisible = true
    }

    protected open fun onStartDragging() {
        // Override to add visual feedback
        alpha = 0.95f
    }

    protected open fun onStopDragging() {
        // Override to remove visual feedback
        alpha = 0.8f
    }

    open fun dismiss() {
        try {
            windowManager.removeView(this)
            Log.d(TAG, "$overlayType overlay dismissed")
        } catch (e: Exception) {
            Log.w(TAG, "Error dismissing $overlayType overlay", e)
        }
    }

    private fun savePosition() {
        layoutParams?.let { params ->
            val prefs = getPreferences()
            prefs.edit().apply {
                putInt("${overlayType}_x", params.x)
                putInt("${overlayType}_y", params.y)
                apply()
            }
            Log.d(TAG, "Saved $overlayType position: (${params.x}, ${params.y})")
        }
    }

    private fun loadPosition(): Pair<Int, Int> {
        val prefs = getPreferences()
        val defaultX = 20
        val defaultY = 200
        
        val x = prefs.getInt("${overlayType}_x", defaultX)
        val y = prefs.getInt("${overlayType}_y", defaultY)
        
        Log.d(TAG, "Loaded $overlayType position: ($x, $y)")
        return Pair(x, y)
    }

    private fun getPreferences(): SharedPreferences {
        return context.getSharedPreferences("overlay_positions", Context.MODE_PRIVATE)
    }

    fun updateTransparency(transparency: Float) {
        alpha = transparency
    }

    // Note: isVisible is already available from View class (LinearLayout extends View)
    // No need to declare it separately
}
