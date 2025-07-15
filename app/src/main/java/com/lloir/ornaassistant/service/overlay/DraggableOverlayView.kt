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
import android.widget.TextView
import androidx.core.view.isVisible
import com.lloir.ornaassistant.domain.model.AppSettings
import com.lloir.ornaassistant.utils.AccessibilityUtils
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
    private var closeButton: TextView? = null
    private var headerLayout: LinearLayout? = null

    // Accessibility settings
    protected var useHighContrastMode: Boolean = false
    protected var useLargerFontSize: Boolean = false
    protected var useTextToSpeech: Boolean = false
    protected var useReducedMotion: Boolean = false

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

        // Set content description for the overlay itself
        contentDescription = "$overlayType overlay"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES

        // Create a horizontal header layout for title and close button
        headerLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            contentDescription = "$overlayType overlay header"
        }

        // Add close button
        closeButton = TextView(context).apply {
            text = "✕"  // X symbol
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(8, 0, 0, 0)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                weight = 0f
                marginEnd = 0
            }

            // Set content description for screen readers
            contentDescription = "Close $overlayType overlay"
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES

            // Set click listener to dismiss the overlay
            setOnClickListener {
                // Announce before dismissing
                announceForAccessibility("Closing $overlayType overlay")
                dismiss()
            }
        }

        // Add the header layout as the first child
        headerLayout?.let {
            it.addView(closeButton)
            addView(it, 0)
        }
    }

    abstract fun setupContent()
    /**
     * Update the content of the overlay
     * Subclasses should implement this to update their specific content
     * @param data The data to update the content with
     */
    abstract fun updateContent(data: Any?)

    /**
     * Announce content updates for accessibility
     * This should be called by subclasses when significant content changes occur
     * @param message The message to announce
     * @param important Whether this is an important announcement that should be prioritized
     */
    protected fun announceContentUpdate(message: String, important: Boolean = false) {
        // TextToSpeech functionality has been removed

        // Use the view's accessibility announcement mechanism
        AccessibilityUtils.announceForAccessibilityCompat(this, message)
    }

    /**
     * Create the overlay and add it to the window
     * @param settings Optional accessibility settings to apply
     */
    fun create(settings: AppSettings? = null) {
        // Apply accessibility settings if provided
        settings?.let { applyAccessibilitySettings(it) }

        setupContent()
        setupTouchHandling()
        addToWindow()

        Log.d(TAG, "Created $overlayType overlay with accessibility settings: highContrast=$useHighContrastMode, largerFont=$useLargerFontSize, tts=$useTextToSpeech, reducedMotion=$useReducedMotion")
    }

    /**
     * Helper method to create a TextView with common styling
     * Applies accessibility settings like larger font size and high contrast if enabled
     */
    protected fun createTextView(
        textColor: Int = Color.WHITE,
        textSize: Float = 12f,
        bottomPadding: Int = 4
    ): TextView {
        return TextView(context).apply {
            // Apply high contrast mode if enabled
            val finalTextColor = if (useHighContrastMode) Color.WHITE else textColor
            setTextColor(finalTextColor)

            // Apply larger font size if enabled
            val finalTextSize = if (useLargerFontSize) textSize * 1.3f else textSize
            this.textSize = finalTextSize

            setPadding(0, 0, 0, bottomPadding)
        }
    }

    /**
     * Helper method to create a title TextView
     */
    protected fun createTitleTextView(): TextView {
        return createTextView(Color.WHITE, 14f, 4)
    }

    /**
     * Helper method to create a subtitle TextView
     */
    protected fun createSubtitleTextView(textColor: Int = Color.YELLOW): TextView {
        return createTextView(textColor, 12f, 4)
    }

    /**
     * Helper method to create a detail TextView
     */
    protected fun createDetailTextView(textColor: Int = Color.CYAN): TextView {
        return createTextView(textColor, 11f, 2)
    }

    /**
     * Helper method to create a small info TextView
     */
    protected fun createSmallInfoTextView(textColor: Int = Color.LTGRAY): TextView {
        return createTextView(textColor, 10f, 2)
    }

    /**
     * Helper method to format numbers with K/M suffixes
     */
    protected fun formatNumber(number: Long): String {
        return when {
            number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
            number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
            else -> number.toString()
        }
    }

    private fun setupTouchHandling() {
        setOnTouchListener { v, event ->
            // Don't handle touch events if they're on the close button
            if (event.rawX >= closeButton?.left ?: 0 && 
                event.rawX <= closeButton?.right ?: 0 &&
                event.rawY >= closeButton?.top ?: 0 && 
                event.rawY <= closeButton?.bottom ?: 0) {
                return@setOnTouchListener false
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x?.toFloat() ?: 0f
                    initialY = layoutParams?.y?.toFloat() ?: 0f
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    hasMoved = false
                    touchStartTime = System.currentTimeMillis()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY

                    if (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD) {
                        hasMoved = true

                        // Start dragging as soon as the user moves beyond the threshold
                        if (!isDragging) {
                            isDragging = true
                            onStartDragging()
                        }

                        // Update position while dragging
                        layoutParams?.let { params ->
                            params.x = (initialX + deltaX).toInt()
                            params.y = (initialY + deltaY).toInt()
                            windowManager.updateViewLayout(this, params)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - touchStartTime

                    if (isDragging) {
                        // Save position after dragging
                        savePosition()
                        onStopDragging()
                    } else if (!hasMoved && duration < TAP_DURATION_MS) {
                        // Handle tap (if needed in the future)
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
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.TOP or Gravity.LEFT
            format = PixelFormat.TRANSPARENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            x = savedPosition.first
            y = savedPosition.second
        }

        windowManager.addView(this, layoutParams)
        isVisible = true

        // Announce that the overlay has been created
        announceOverlayCreated()
    }

    /**
     * Announce that the overlay has been created
     * This is called when the overlay is added to the window
     */
    protected fun announceOverlayCreated() {
        // Announce the overlay creation with a slight delay to ensure TTS is ready
        CoroutineScope(Dispatchers.Main).launch {
            delay(500) // Short delay to ensure the overlay is visible
            announceForAccessibility("$overlayType overlay opened")
        }
    }

    protected open fun onStartDragging() {
        // Announce that dragging has started
        announceForAccessibility("Moving $overlayType overlay")

        // Apply visual feedback with reduced motion consideration
        if (!useReducedMotion) {
            // Normal animation - using setViewProperty instead of animateViewProperty
            AccessibilityUtils.setViewProperty(
                view = this,
                property = "alpha",
                value = 0.95f
            )
        } else {
            // Skip animation for reduced motion
            alpha = 0.95f
        }
    }

    protected open fun onStopDragging() {
        // Announce that dragging has stopped
        announceForAccessibility("$overlayType overlay moved")

        // Apply visual feedback with reduced motion consideration
        if (!useReducedMotion) {
            // Normal animation - using setViewProperty instead of animateViewProperty
            AccessibilityUtils.setViewProperty(
                view = this,
                property = "alpha",
                value = 0.8f
            )
        } else {
            // Skip animation for reduced motion
            alpha = 0.8f
        }
    }

    open fun dismiss() {
        try {
            // Announce that the overlay is being dismissed
            announceForAccessibility("$overlayType overlay closed")

            // Clean up accessibility resources
            cleanupAccessibilityResources()

            windowManager.removeView(this)
            // Use Log directly here since LogUtils might not be available in this base class
            Log.d(TAG, "$overlayType overlay dismissed")
        } catch (e: Exception) {
            Log.w(TAG, "Error dismissing $overlayType overlay", e)
        }
    }

    /**
     * Clean up accessibility resources when the overlay is dismissed
     * This method previously handled TextToSpeech cleanup, which has been removed
     */
    protected fun cleanupAccessibilityResources() {
        // TextToSpeech cleanup has been removed
        Log.d(TAG, "Accessibility resources cleanup for $overlayType overlay")
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

    /**
     * Apply accessibility settings from AppSettings
     * @param settings The app settings containing accessibility preferences
     */
    fun applyAccessibilitySettings(settings: AppSettings) {
        useHighContrastMode = settings.useHighContrastMode
        useLargerFontSize = settings.useLargerFontSize
        useTextToSpeech = settings.useTextToSpeech
        useReducedMotion = settings.useReducedMotion

        // Apply high contrast mode if enabled
        if (useHighContrastMode) {
            applyHighContrastMode()
        }

        // TextToSpeech initialization has been removed

        // Set content description for the close button
        closeButton?.contentDescription = "Close $overlayType overlay"

        Log.d(TAG, "Applied accessibility settings: highContrast=$useHighContrastMode, largerFont=$useLargerFontSize, tts=$useTextToSpeech, reducedMotion=$useReducedMotion")
    }

    /**
     * Update accessibility settings for an existing TextView
     * This is useful for subclasses to apply accessibility settings to their TextViews
     * @param textView The TextView to update
     * @param baseTextSize The base text size to use if larger font size is enabled
     * @param baseTextColor The base text color to use if high contrast mode is not enabled
     */
    protected fun updateTextViewAccessibility(
        textView: TextView?,
        baseTextSize: Float,
        baseTextColor: Int
    ) {
        textView?.let {
            // Apply high contrast mode if enabled
            if (useHighContrastMode) {
                it.setTextColor(Color.WHITE)
            } else {
                it.setTextColor(baseTextColor)
            }

            // Apply larger font size if enabled
            if (useLargerFontSize) {
                it.textSize = baseTextSize * 1.3f
            } else {
                it.textSize = baseTextSize
            }
        }
    }

    /**
     * Apply high contrast mode to the overlay
     */
    protected fun applyHighContrastMode() {
        setBackgroundColor(Color.BLACK)
        closeButton?.setTextColor(Color.WHITE)
    }

    /**
     * Apply larger font size to a TextView
     * @param textView The TextView to apply larger font size to
     * @param baseSize The base font size
     */
    protected fun applyLargerFontSize(textView: TextView?, baseSize: Float) {
        if (useLargerFontSize && textView != null) {
            textView.textSize = baseSize * 1.3f
        }
    }

    /**
     * Announce a message for accessibility
     * TextToSpeech functionality has been removed
     * @param message The message to announce
     */
    protected fun announceForAccessibility(message: String) {
        // TextToSpeech functionality has been removed
        // Use standard accessibility announcement
        AccessibilityUtils.announceForAccessibilityCompat(this, message)
    }

    // Note: isVisible is already available from View class (LinearLayout extends View)
    // No need to declare it separately
}
