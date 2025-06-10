package com.lloir.ornaassistant.utils

import android.content.Context
import android.provider.Settings
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
}