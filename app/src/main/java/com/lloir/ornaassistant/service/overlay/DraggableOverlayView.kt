package com.lloir.ornaassistant.service.overlay

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.lloir.ornaassistant.utils.OverlayDebugger
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
    private var tapDismissEnabled = false
    private var creationTime = 0L

    companion object {
        private const val TAG = "DraggableOverlay"
        private const val TAP_DURATION_MS = 200L
        private const val DRAG_THRESHOLD = 10f
        private const val LONG_PRESS_DURATION_MS = 300L
        private const val TAP_DISMISS_DELAY_MS = 1000L // Delay before tap-to-dismiss is enabled
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

        // Initialize creation time and set up delayed tap-to-dismiss
        creationTime = System.currentTimeMillis()
        tapDismissEnabled = false

        // Enable tap-to-dismiss after delay
        CoroutineScope(Dispatchers.Main).launch {
            delay(TAP_DISMISS_DELAY_MS)
            tapDismissEnabled = true
            Log.d(TAG, "Tap-to-dismiss enabled for $overlayType overlay")
        }
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
                        // Quick tap - dismiss only if tap-to-dismiss is enabled
                        !hasMoved && duration < TAP_DURATION_MS && tapDismissEnabled -> {
                            Log.d(TAG, "Tap detected - dismissing overlay")
                            dismiss()
                        }
                        // Quick tap but tap-to-dismiss not enabled yet
                        !hasMoved && duration < TAP_DURATION_MS && !tapDismissEnabled -> {
                            Log.d(TAG, "Tap detected but dismiss is not enabled yet - ignoring")
                            // Ignore the tap
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

        // Get device manufacturer for device-specific adjustments
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        Log.d(TAG, "Device manufacturer: $manufacturer")

        layoutParams = WindowManager.LayoutParams().apply {
            // Use MATCH_PARENT for width on Samsung devices to improve stability
            width = if (manufacturer.contains("samsung")) {
                Log.d(TAG, "Using Samsung-specific width parameter")
                WindowManager.LayoutParams.WRAP_CONTENT
            } else {
                WindowManager.LayoutParams.WRAP_CONTENT
            }

            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

            // Use different gravity for different manufacturers
            gravity = when {
                manufacturer.contains("samsung") -> {
                    Log.d(TAG, "Using Samsung-specific gravity")
                    Gravity.TOP or Gravity.RIGHT // Samsung devices often work better with RIGHT gravity
                }
                manufacturer.contains("oneplus") -> {
                    Log.d(TAG, "Using OnePlus-specific gravity")
                    Gravity.TOP or Gravity.CENTER // OnePlus devices often work better with CENTER gravity
                }
                else -> {
                    Gravity.TOP or Gravity.LEFT // Default for other devices
                }
            }

            format = PixelFormat.TRANSLUCENT // Use TRANSLUCENT instead of TRANSPARENT for better compatibility

            // Add additional flags for stability on problematic devices
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

            // Use saved position
            x = savedPosition.first
            y = savedPosition.second
        }

        try {
            // Create debug report before adding view
            val debugReport = OverlayDebugger().debugOverlayIssues(context)

            // Log detailed device and window parameters for debugging
            Log.d(TAG, "Adding $overlayType overlay with device-specific parameters")
            Log.d(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})")
            Log.d(TAG, "Window params: width=${layoutParams?.width}, height=${layoutParams?.height}, " +
                    "gravity=${layoutParams?.gravity}, format=${layoutParams?.format}, " +
                    "flags=${layoutParams?.flags}, position=(${layoutParams?.x}, ${layoutParams?.y})")

            windowManager.addView(this, layoutParams)
            isVisible = true

            Log.d(TAG, "Successfully added $overlayType overlay to window")

            // Log success in debug report
            debugReport.addSection("Overlay Creation Success")
            debugReport.addItem("Overlay Type", overlayType)
            debugReport.addItem("Parameters", "width=${layoutParams?.width}, height=${layoutParams?.height}, " +
                    "gravity=${layoutParams?.gravity}, position=(${layoutParams?.x}, ${layoutParams?.y})")

            // Log debug report for future reference
            Log.d(TAG, "Overlay debug report:\n$debugReport")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay to window", e)

            // Create debug report for failure
            val debugReport = OverlayDebugger().debugOverlayIssues(context)
            debugReport.addSection("Overlay Creation Failure")
            debugReport.addItem("Error", e.message ?: "Unknown error")
            debugReport.addItem("Overlay Type", overlayType)
            Log.e(TAG, "Overlay debug report for failure:\n$debugReport")

            // Try with simpler parameters as fallback
            try {
                Log.d(TAG, "Attempting fallback with simpler parameters")
                layoutParams = WindowManager.LayoutParams().apply {
                    width = WindowManager.LayoutParams.WRAP_CONTENT
                    height = WindowManager.LayoutParams.WRAP_CONTENT
                    type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                    gravity = Gravity.TOP or Gravity.LEFT
                    format = PixelFormat.TRANSPARENT
                    flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    x = savedPosition.first
                    y = savedPosition.second
                }
                windowManager.addView(this, layoutParams)
                isVisible = true
                Log.d(TAG, "Added overlay with fallback parameters")

                // Log success with fallback
                debugReport.addSection("Fallback Success")
                debugReport.addItem("Parameters", "width=${layoutParams?.width}, height=${layoutParams?.height}, " +
                        "gravity=${layoutParams?.gravity}, position=(${layoutParams?.x}, ${layoutParams?.y})")
                Log.d(TAG, "Updated overlay debug report:\n$debugReport")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to add overlay even with fallback parameters", e2)

                // Update debug report with fallback failure
                debugReport.addSection("Fallback Failure")
                debugReport.addItem("Error", e2.message ?: "Unknown error")
                Log.e(TAG, "Final overlay debug report:\n$debugReport")
            }
        }
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
