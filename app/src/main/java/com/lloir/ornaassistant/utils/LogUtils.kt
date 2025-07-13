package com.lloir.ornaassistant.utils

import android.util.Log
import com.lloir.ornaassistant.BuildConfig
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized logging utility to control log verbosity
 */
@Singleton
class LogUtils @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG_PREFIX = "OrnaAssistant"
        
        // Use the settings repository to check if debug mode is enabled
        private var debugModeEnabled = false
        
        // Add this field for build configuration control
        private val PRODUCTION_BUILD = !BuildConfig.DEBUG
        
        // Static methods for use before initialization
        fun isDebugEnabled(): Boolean = debugModeEnabled && !PRODUCTION_BUILD
        
        // Helper method to check if a tag should be logged
        private fun shouldLog(tag: String, level: Int): Boolean {
            // Always log errors regardless of debug mode
            if (level == Log.ERROR) return true
            
            // In production builds, don't log debug messages at all
            if (PRODUCTION_BUILD && level == Log.DEBUG) return false
            
            // Otherwise, check debug mode
            return debugModeEnabled
        }
    }
    
    /**
     * Initialize the logging utility with settings
     */
    fun initialize(settingsRepository: SettingsRepository) {
        // Set initial value
        runBlocking { 
            debugModeEnabled = settingsRepository.getSettings().debugMode
        }
        
        // Log initialization status
        if (debugModeEnabled) {
            Log.i("$TAG_PREFIX:LogUtils", "Debug logging enabled")
        }
    }
    
    /**
     * Debug log - only shown when debug mode is enabled
     */
    fun d(tag: String, message: String) {
        if (shouldLog(tag, Log.DEBUG)) {
            Log.d("$TAG_PREFIX:$tag", message)
        }
    }
    
    /**
     * Error log - always shown regardless of debug mode
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG_PREFIX:$tag", message, throwable)
    }
    
    /**
     * Warning log - controlled by debug mode in production
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(tag, Log.WARN)) {
            Log.w("$TAG_PREFIX:$tag", message, throwable)
        }
    }
    
    /**
     * Info log - controlled by debug mode
     */
    fun i(tag: String, message: String) {
        if (shouldLog(tag, Log.INFO)) {
            Log.i("$TAG_PREFIX:$tag", message)
        }
    }
}