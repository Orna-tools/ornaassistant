package com.lloir.ornaassistant.utils

import android.content.Context
import android.provider.Settings
import android.os.Build
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.view.View
import android.text.TextUtils
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.lloir.ornaassistant.domain.model.ColorBlindnessType
import java.util.UUID
import kotlin.math.pow

object AccessibilityUtils {

    private const val ACCESSIBILITY_SERVICE_NAME = "com.lloir.ornaassistant/.service.accessibility.OrnaAccessibilityService"

    // TTS functionality has been removed as per requirements

    // Motion animations have been removed as per requirements

    /**
     * Set view property without animation
     * @param view The view to modify
     * @param property The property to set (e.g., "alpha", "translationY")
     * @param value The final value to set
     * @param onEnd Callback when property is set
     */
    fun setViewProperty(
        view: View,
        property: String,
        value: Float,
        onEnd: (() -> Unit)? = null
    ) {
        // Set the final value immediately without animation
        when (property) {
            "alpha" -> view.alpha = value
            "translationX" -> view.translationX = value
            "translationY" -> view.translationY = value
            "scaleX" -> view.scaleX = value
            "scaleY" -> view.scaleY = value
            "rotation" -> view.rotation = value
            else -> {
                // For other properties, use reflection
                try {
                    val method = View::class.java.getMethod("set${property.capitalize()}", Float::class.java)
                    method.invoke(view, value)
                } catch (e: Exception) {
                    // Fallback for properties that can't be set directly
                    val animator = ObjectAnimator.ofFloat(view, property, value)
                    animator.duration = 0
                    animator.start()
                }
            }
        }
        onEnd?.invoke()
    }

    /**
     * Create a no-animation spec for Compose
     * @return An animation spec with zero duration
     */
    fun <T> getNoAnimationSpec(): AnimationSpec<T> {
        return TweenSpec(durationMillis = 0)
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            return false
        }

        val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')

        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )

            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessabilityService = mStringColonSplitter.next()
                    if (accessabilityService.contains(context.packageName, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * Android 16 compatible accessibility announcement replacement
     * Using setAccessibilityPaneTitle instead of deprecated announceForAccessibility
     */
    fun announceForAccessibilityCompat(view: View, announcement: CharSequence) {
        if (Build.VERSION.SDK_INT >= 35) {
            // Android 16+: Use setAccessibilityPaneTitle for important announcements
            view.setAccessibilityPaneTitle(announcement)
        } else {
            // Pre-Android 16: Use the traditional method
            @Suppress("DEPRECATION")
            view.announceForAccessibility(announcement)
        }
    }

    /**
     * Android 16 compatible error announcement
     */
    fun announceErrorCompat(view: View, error: CharSequence) {
        if (Build.VERSION.SDK_INT >= 35) {
            // Android 16+: Use proper error handling
            if (view is android.widget.TextView) {
                view.error = error
            }
        } else {
            // Pre-Android 16: Use accessibility announcement
            @Suppress("DEPRECATION")
            view.announceForAccessibility(error)
        }
    }

    // Color Blindness Transformation Utilities

    /**
     * Transform a color based on the selected color blindness type
     * @param color The original color
     * @param colorBlindnessType The type of color blindness to simulate
     * @return The transformed color
     */
    fun transformColorForColorBlindness(color: Color, colorBlindnessType: ColorBlindnessType): Color {
        return when (colorBlindnessType) {
            ColorBlindnessType.NONE -> color
            ColorBlindnessType.PROTANOPIA -> simulateProtanopia(color)
            ColorBlindnessType.DEUTERANOPIA -> simulateDeuteranopia(color)
            ColorBlindnessType.TRITANOPIA -> simulateTritanopia(color)
            ColorBlindnessType.ACHROMATOPSIA -> simulateAchromatopsia(color)
        }
    }

    /**
     * Simulate protanopia (red-blind) color blindness
     * @param color The original color
     * @return The transformed color
     */
    private fun simulateProtanopia(color: Color): Color {
        val r = color.red
        val g = color.green
        val b = color.blue

        // Protanopia simulation matrix
        val newR = 0.567f * r + 0.433f * g + 0.0f * b
        val newG = 0.558f * r + 0.442f * g + 0.0f * b
        val newB = 0.0f * r + 0.242f * g + 0.758f * b

        return Color(newR, newG, newB, color.alpha)
    }

    /**
     * Simulate deuteranopia (green-blind) color blindness
     * @param color The original color
     * @return The transformed color
     */
    private fun simulateDeuteranopia(color: Color): Color {
        val r = color.red
        val g = color.green
        val b = color.blue

        // Deuteranopia simulation matrix
        val newR = 0.625f * r + 0.375f * g + 0.0f * b
        val newG = 0.7f * r + 0.3f * g + 0.0f * b
        val newB = 0.0f * r + 0.3f * g + 0.7f * b

        return Color(newR, newG, newB, color.alpha)
    }

    /**
     * Simulate tritanopia (blue-blind) color blindness
     * @param color The original color
     * @return The transformed color
     */
    private fun simulateTritanopia(color: Color): Color {
        val r = color.red
        val g = color.green
        val b = color.blue

        // Tritanopia simulation matrix
        val newR = 0.95f * r + 0.05f * g + 0.0f * b
        val newG = 0.0f * r + 0.433f * g + 0.567f * b
        val newB = 0.0f * r + 0.475f * g + 0.525f * b

        return Color(newR, newG, newB, color.alpha)
    }

    /**
     * Simulate achromatopsia (no color) color blindness
     * @param color The original color
     * @return The transformed color (grayscale)
     */
    private fun simulateAchromatopsia(color: Color): Color {
        val r = color.red
        val g = color.green
        val b = color.blue

        // Convert to grayscale using luminance formula
        val gray = 0.299f * r + 0.587f * g + 0.114f * b

        return Color(gray, gray, gray, color.alpha)
    }

    /**
     * Add pattern to a color for better distinction
     * This is a placeholder - in a real implementation, you would return a pattern drawable
     * @param color The original color
     * @return The color with pattern information
     */
    fun addPatternToColor(color: Color): Int {
        // In a real implementation, this would create or return a pattern drawable
        // For now, we just return the color's ARGB value
        return color.toArgb()
    }

    // Keyboard Navigation and Focus Utilities

    /**
     * Enhanced focus indicator size for better visibility
     * @param defaultSize The default focus indicator size
     * @param enhanceFocusIndicators Whether to enhance focus indicators
     * @return The adjusted focus indicator size
     */
    fun getFocusIndicatorSize(defaultSize: Float, enhanceFocusIndicators: Boolean): Float {
        return if (enhanceFocusIndicators) {
            defaultSize * 1.5f
        } else {
            defaultSize
        }
    }

    /**
     * Enhanced focus indicator color for better visibility
     * @param defaultColor The default focus indicator color
     * @param enhanceFocusIndicators Whether to enhance focus indicators
     * @return The adjusted focus indicator color
     */
    fun getFocusIndicatorColor(defaultColor: Color, enhanceFocusIndicators: Boolean): Color {
        return if (enhanceFocusIndicators) {
            // Make the focus color more vibrant
            Color(
                red = (defaultColor.red + 0.2f).coerceAtMost(1.0f),
                green = (defaultColor.green + 0.2f).coerceAtMost(1.0f),
                blue = (defaultColor.blue + 0.2f).coerceAtMost(1.0f),
                alpha = 1.0f
            )
        } else {
            defaultColor
        }
    }

    /**
     * Get keyboard shortcut description for accessibility
     * @param shortcut The keyboard shortcut (e.g., "Ctrl+S")
     * @param action The action description (e.g., "Save")
     * @param enableKeyboardShortcuts Whether keyboard shortcuts are enabled
     * @return The formatted shortcut description or empty string if shortcuts are disabled
     */
    fun getKeyboardShortcutDescription(
        shortcut: String,
        action: String,
        enableKeyboardShortcuts: Boolean
    ): String {
        return if (enableKeyboardShortcuts) {
            "$action ($shortcut)"
        } else {
            action
        }
    }
}
