package com.lloir.ornaassistant.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

object PermissionHelper {
    private const val TAG = "PermissionHelper"

    /**
     * Check if overlay permission is granted
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val canDraw = Settings.canDrawOverlays(context)
                Log.d(TAG, "Overlay permission check: $canDraw")
                canDraw
            } else {
                Log.d(TAG, "Pre-M device, overlay permission assumed granted")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking overlay permission", e)
            false
        }
    }

    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return AccessibilityUtils.isAccessibilityServiceEnabled(context)
    }

    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        val overlayGranted = hasOverlayPermission(context)
        val accessibilityGranted = isAccessibilityServiceEnabled(context)

        Log.d(TAG, "Permission status - Overlay: $overlayGranted, Accessibility: $accessibilityGranted")

        return overlayGranted && accessibilityGranted
    }

    /**
     * Request overlay permission
     */
    fun requestOverlayPermission(activity: ComponentActivity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(activity)) {
                    Log.d(TAG, "Requesting overlay permission")
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${activity.packageName}")
                    )
                    activity.startActivity(intent)
                } else {
                    Log.d(TAG, "Overlay permission already granted")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting overlay permission", e)
        }
    }

    /**
     * Request accessibility permission by opening accessibility settings
     */
    fun requestAccessibilityPermission(context: Context) {
        try {
            Log.d(TAG, "Opening accessibility settings")
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening accessibility settings", e)
        }
    }

    /**
     * Create overlay permission launcher for activity results
     */
    fun createOverlayPermissionLauncher(
        activity: ComponentActivity,
        onResult: (Boolean) -> Unit
    ): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            val hasPermission = hasOverlayPermission(activity)
            Log.d(TAG, "Overlay permission result: $hasPermission")
            onResult(hasPermission)
        }
    }

    /**
     * Get detailed permission status for debugging
     */
    fun getPermissionStatus(context: Context): String {
        val overlay = hasOverlayPermission(context)
        val accessibility = isAccessibilityServiceEnabled(context)

        return buildString {
            appendLine("Permission Status:")
            appendLine("- Overlay: $overlay")
            appendLine("- Accessibility: $accessibility")
            appendLine("- All granted: ${overlay && accessibility}")
            appendLine("- SDK Version: ${Build.VERSION.SDK_INT}")
            appendLine("- Package: ${context.packageName}")
        }
    }

    /**
     * Validate overlay permission with additional checks
     */
    fun validateOverlayPermission(context: Context): ValidationResult {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                ValidationResult.Success("Pre-M device, overlay permission not required")
            } else {
                val canDraw = Settings.canDrawOverlays(context)
                if (canDraw) {
                    ValidationResult.Success("Overlay permission granted")
                } else {
                    ValidationResult.Failure("Overlay permission not granted")
                }
            }
        } catch (e: SecurityException) {
            ValidationResult.Failure("Security exception: ${e.message}")
        } catch (e: Exception) {
            ValidationResult.Failure("Unexpected error: ${e.message}")
        }
    }

    sealed class ValidationResult {
        data class Success(val message: String) : ValidationResult()
        data class Failure(val message: String) : ValidationResult()
    }
}

/**
 * Extension functions for easier permission checking
 */
fun Context.hasOverlayPermission(): Boolean = PermissionHelper.hasOverlayPermission(this)
fun Context.hasAccessibilityPermission(): Boolean = PermissionHelper.isAccessibilityServiceEnabled(this)
fun Context.hasAllRequiredPermissions(): Boolean = PermissionHelper.hasAllPermissions(this)