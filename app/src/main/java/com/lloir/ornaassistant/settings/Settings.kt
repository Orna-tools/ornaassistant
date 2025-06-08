package com.lloir.ornaassistant.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object Settings {
    var isDebugEnabled: Boolean = false
    
    private lateinit var preferences: SharedPreferences
    
    fun initialize(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
        isDebugEnabled = preferences.getBoolean("enable_debug_logging", false)
    }
    
    fun getScreenReaderMethod(): String {
        return preferences.getString("screen_reader_method", "media_projection") ?: "media_projection"
    }
    
    fun setScreenReaderMethod(method: String) {
        preferences.edit().putString("screen_reader_method", method).apply()
    }
    
    fun isAutoDetectEnabled(): Boolean {
        return preferences.getBoolean("auto_detect_method", true)
    }
    
    fun isOverlayEnabled(overlayType: String): Boolean {
        return preferences.getBoolean("${overlayType}_overlay", true)
    }
    
    fun setOverlayEnabled(overlayType: String, enabled: Boolean) {
        preferences.edit().putBoolean("${overlayType}_overlay", enabled).apply()
    }
}
