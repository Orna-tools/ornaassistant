package com.lloir.ornaassistant.utils

import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Android 16 Intent security helper
 * Handles new intent redirection protections
 */
object IntentSecurityHelper {
    private const val TAG = "IntentSecurityHelper"
    
    /**
     * Safely launch an intent with Android 16 security considerations
     */
    fun createSecureIntent(action: String? = null, uri: android.net.Uri? = null): Intent {
        val intent = Intent().apply {
            action?.let { this.action = it }
            uri?.let { data = it }
        }
        
        // Android 16: Apply launch security protections by default
        if (Build.VERSION.SDK_INT >= 35) {
            // Security is enabled by default, only opt-out if absolutely necessary
            Log.d(TAG, "Intent created with Android 16 security protections")
        }
        
        return intent
    }
    
    /**
     * Opt-out of launch security protection (use sparingly)
     */
    fun removeLaunchSecurityProtection(intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= 35) {
                // Use reflection for compatibility with pre-Android 16 compile targets
                val method = Intent::class.java.getDeclaredMethod("removeLaunchSecurityProtection")
                method.invoke(intent)
                Log.w(TAG, "Removed launch security protection - ensure this is necessary!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove launch security protection", e)
        }
    }
}
