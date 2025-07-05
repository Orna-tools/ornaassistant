package com.lloir.ornaassistant.utils

import android.content.Context
import android.provider.Settings
import android.os.Build
import android.view.View
import android.text.TextUtils

object AccessibilityUtils {

    private const val ACCESSIBILITY_SERVICE_NAME = "com.lloir.ornaassistant/.service.accessibility.OrnaAccessibilityService"

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