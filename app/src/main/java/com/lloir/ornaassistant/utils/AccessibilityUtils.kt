package com.lloir.ornaassistant.utils

import android.content.Context
import android.provider.Settings
import android.os.Build
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.text.TextUtils
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import java.util.Locale
import java.util.UUID

object AccessibilityUtils {

    private const val ACCESSIBILITY_SERVICE_NAME = "com.lloir.ornaassistant/.service.accessibility.OrnaAccessibilityService"

    // Text-to-speech instance
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    /**
     * Initialize the TextToSpeech engine
     * Call this method in your Application class or main activity
     */
    fun initTextToSpeech(context: Context, onInitListener: ((status: Int) -> Unit)? = null) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context) { status ->
                isTtsInitialized = status == TextToSpeech.SUCCESS
                if (isTtsInitialized) {
                    textToSpeech?.language = Locale.getDefault()
                }
                onInitListener?.invoke(status)
            }
        }
    }

    /**
     * Speak the given text using TextToSpeech
     * @param text The text to speak
     * @param queueMode Whether to queue the speech or interrupt current speech
     * @param onDone Callback when speech is complete
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH, onDone: (() -> Unit)? = null) {
        if (!isTtsInitialized || textToSpeech == null) return

        val utteranceId = UUID.randomUUID().toString()

        if (onDone != null) {
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    onDone.invoke()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {}
            })
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech?.speak(text, queueMode, null, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            textToSpeech?.speak(text, queueMode, null)
        }
    }

    /**
     * Release TextToSpeech resources
     * Call this method in onDestroy() of your Application class or main activity
     */
    fun shutdownTextToSpeech() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isTtsInitialized = false
    }

    /**
     * Convenience method to speak text if text-to-speech is enabled in settings
     * @param text The text to speak
     * @param useTextToSpeech Whether text-to-speech is enabled in settings
     * @param queueMode Whether to queue the speech or interrupt current speech
     */
    fun speakIfEnabled(text: String, useTextToSpeech: Boolean, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (useTextToSpeech && isTtsInitialized) {
            speak(text, queueMode)
        }
    }

    /**
     * Convenience method to speak text for a UI element with content description
     * @param view The view to get content description from
     * @param useTextToSpeech Whether text-to-speech is enabled in settings
     */
    fun speakContentDescription(view: View, useTextToSpeech: Boolean) {
        if (useTextToSpeech && isTtsInitialized) {
            val contentDescription = view.contentDescription
            if (!contentDescription.isNullOrEmpty()) {
                speak(contentDescription.toString())
            }
        }
    }

    // Reduced Motion Utilities

    /**
     * Default animation duration in milliseconds
     */
    private const val DEFAULT_ANIMATION_DURATION = 300L

    /**
     * Reduced animation duration in milliseconds (for users who prefer reduced motion)
     */
    private const val REDUCED_ANIMATION_DURATION = 100L

    /**
     * Get the appropriate animation duration based on reduced motion preference
     * @param useReducedMotion Whether reduced motion is enabled in settings
     * @param defaultDuration The default animation duration
     * @return The adjusted animation duration
     */
    fun getAnimationDuration(useReducedMotion: Boolean, defaultDuration: Long = DEFAULT_ANIMATION_DURATION): Long {
        return if (useReducedMotion) {
            REDUCED_ANIMATION_DURATION
        } else {
            defaultDuration
        }
    }

    /**
     * Create an animation spec that respects reduced motion settings
     * @param useReducedMotion Whether reduced motion is enabled in settings
     * @param durationMillis The default animation duration
     * @return An animation spec with appropriate duration
     */
    fun <T> getAccessibleAnimationSpec(
        useReducedMotion: Boolean,
        durationMillis: Int = DEFAULT_ANIMATION_DURATION.toInt()
    ): AnimationSpec<T> {
        val duration = getAnimationDuration(useReducedMotion, durationMillis.toLong()).toInt()
        return TweenSpec(durationMillis = duration)
    }

    /**
     * Animate a view with accessibility considerations
     * @param view The view to animate
     * @param property The property to animate (e.g., "alpha", "translationY")
     * @param values The values to animate between
     * @param useReducedMotion Whether reduced motion is enabled in settings
     * @param duration The default animation duration
     * @param interpolator The animation interpolator
     * @param onEnd Callback when animation ends
     */
    fun animateViewProperty(
        view: View,
        property: String,
        values: FloatArray,
        useReducedMotion: Boolean,
        duration: Long = DEFAULT_ANIMATION_DURATION,
        interpolator: TimeInterpolator = AccelerateDecelerateInterpolator(),
        onEnd: (() -> Unit)? = null
    ) {
        // If reduced motion is enabled and this is not an essential animation,
        // skip the animation and just set the final value
        if (useReducedMotion) {
            when (property) {
                "alpha" -> view.alpha = values.last()
                "translationX" -> view.translationX = values.last()
                "translationY" -> view.translationY = values.last()
                "scaleX" -> view.scaleX = values.last()
                "scaleY" -> view.scaleY = values.last()
                "rotation" -> view.rotation = values.last()
                else -> {
                    // For other properties, use a very short animation
                    val animator = ObjectAnimator.ofFloat(view, property, *values)
                    animator.duration = REDUCED_ANIMATION_DURATION
                    animator.interpolator = interpolator
                    if (onEnd != null) {
                        animator.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                onEnd.invoke()
                            }
                        })
                    }
                    animator.start()
                }
            }
            onEnd?.invoke()
        } else {
            // Normal animation
            val animator = ObjectAnimator.ofFloat(view, property, *values)
            animator.duration = duration
            animator.interpolator = interpolator
            if (onEnd != null) {
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onEnd.invoke()
                    }
                })
            }
            animator.start()
        }
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
}
