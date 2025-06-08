package com.lloir.ornaassistant.services

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.lloir.ornaassistant.MainState
import com.lloir.ornaassistant.ScreenData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean

class ScreenReaderManager private constructor(
    private val context: Context,
    private val mainState: MainState
) {
    companion object {
        private const val TAG = "ScreenReaderManager"
        private const val PREF_SCREEN_READER_METHOD = "screen_reader_method"
        private const val METHOD_MEDIA_PROJECTION = "media_projection"
        private const val METHOD_ACCESSIBILITY = "accessibility"
        
        @Volatile
        private var INSTANCE: ScreenReaderManager? = null
        
        fun getInstance(context: Context, mainState: MainState): ScreenReaderManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScreenReaderManager(context, mainState).also { INSTANCE = it }
            }
        }
    }
    
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val isMediaProjectionActive = AtomicBoolean(false)
    private val isAccessibilityActive = AtomicBoolean(false)
    
    private var mediaProjectionService: MediaProjectionScreenReader? = null
    private var accessibilityService: MyAccessibilityService? = null
    
    fun startScreenReading() {
        val preferredMethod = preferences.getString(PREF_SCREEN_READER_METHOD, METHOD_MEDIA_PROJECTION)
        
        when (preferredMethod) {
            METHOD_MEDIA_PROJECTION -> {
                if (isMediaProjectionSupported()) {
                    startMediaProjection()
                } else {
                    Log.w(TAG, "MediaProjection not supported, falling back to Accessibility")
                    startAccessibilityFallback()
                }
            }
            METHOD_ACCESSIBILITY -> startAccessibilityFallback()
            else -> startMediaProjection() // Default to MediaProjection
        }
    }
    
    private fun isMediaProjectionSupported(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP
    }
    
    private fun startMediaProjection() {
        if (isMediaProjectionActive.compareAndSet(false, true)) {
            val intent = Intent(context, MediaProjectionScreenReader::class.java)
            context.startForegroundService(intent)
            Log.i(TAG, "MediaProjection screen reader started")
        }
    }
    
    private fun startAccessibilityFallback() {
        if (isAccessibilityActive.compareAndSet(false, true)) {
            // Accessibility service is started through system settings
            Log.i(TAG, "Accessibility fallback activated")
        }
    }
    
    fun stopScreenReading() {
        if (isMediaProjectionActive.compareAndSet(true, false)) {
            val intent = Intent(context, MediaProjectionScreenReader::class.java)
            context.stopService(intent)
        }
        isAccessibilityActive.set(false)
    }
    
    fun setScreenReaderMethod(method: String) {
        preferences.edit().putString(PREF_SCREEN_READER_METHOD, method).apply()
        stopScreenReading()
        startScreenReading()
    }
    
    fun processScreenData(packageName: String, data: ArrayList<ScreenData>) {
        mainState.processData(packageName, data)
    }
}
