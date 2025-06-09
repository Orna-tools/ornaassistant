package com.lloir.ornaassistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.lloir.ornaassistant.overlays.Overlay
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.settings.Settings as AppSettings
import androidx.core.net.toUri

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey)

        // Overlay preferences
        val inviterOverlayCheckBox: CheckBoxPreference? = findPreference("inviter_overlay")
        val sessionOverlayCheckBox: CheckBoxPreference? = findPreference("session_overlay")
        val kgOverlayCheckBox: CheckBoxPreference? = findPreference("kg")
        val assessOverlayCheckBox: CheckBoxPreference? = findPreference("assess_overlay")

        // Persistent notification preference
        val persistentNotificationCheckBox: CheckBoxPreference? = findPreference("persistent_notification")

        // Screen reader method preference
        val screenReaderMethod: ListPreference? = findPreference("screen_reader_method")

        // Privacy Policy preference
        val privacyPolicyPreference: Preference? = findPreference("privacy_policy")

        // Initialize persistent notification checkbox with current state
        persistentNotificationCheckBox?.let { checkbox ->
            val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("persistent_notification_enabled", false)
            checkbox.isChecked = isEnabled

            checkbox.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                Log.d("SettingsFragment", "Persistent notification preference changed: $enabled")

                // Send broadcast to MainActivity to handle notification
                val intent = Intent("com.lloir.ornaassistant.UPDATE_NOTIFICATION").apply {
                    putExtra("enabled", enabled)
                }
                requireContext().sendBroadcast(intent)

                // Also call MainActivity method directly if possible
                (requireActivity() as? MainActivity)?.handlePersistentNotificationPreference(enabled)

                showToast(if (enabled) "Persistent notification enabled" else "Persistent notification disabled")
                true
            }
        }

        screenReaderMethod?.setOnPreferenceChangeListener { _, newValue ->
            val method = newValue as String
            AppSettings.setScreenReaderMethod(method)

            when (method) {
                "media_projection" -> {
                    // Check if permission is already granted
                    val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                    val isGranted = prefs.getBoolean("media_projection_granted", false)

                    if (!isGranted) {
                        showMediaProjectionDialog()
                    } else {
                        Toast.makeText(requireContext(), "Switched to modern screen reader", Toast.LENGTH_SHORT).show()
                        // Start the MediaProjection service
                        startMediaProjectionService()
                    }
                }
                "accessibility" -> {
                    showAccessibilitySetupInfo()
                }
            }
            true
        }

        // Privacy Policy click listener
        privacyPolicyPreference?.setOnPreferenceClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ornaassistant.com/"))
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Failed to open privacy policy URL", e)
                showToast("Unable to open privacy policy. Please visit: https://www.ornaassistant.com/")
            }
            true
        }

        inviterOverlayCheckBox?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            if (enabled) {
                checkOverlayPermissionFirst {
                    Overlay.startOverlay(requireContext(), "inviter")
                }
            } else {
                Overlay.stopOverlay()
            }
            true
        }

        sessionOverlayCheckBox?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            if (enabled) {
                checkOverlayPermissionFirst {
                    Overlay.startOverlay(requireContext(), "session")
                }
            } else {
                Overlay.stopOverlay()
            }
            true
        }

        kgOverlayCheckBox?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            if (enabled) {
                checkOverlayPermissionFirst {
                    Overlay.startOverlay(requireContext(), "kg")
                }
            } else {
                Overlay.stopOverlay()
            }
            true
        }

        assessOverlayCheckBox?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            if (enabled) {
                checkOverlayPermissionFirst {
                    Overlay.startOverlay(requireContext(), "assess")
                }
            } else {
                Overlay.stopOverlay()
            }
            true
        }

        val overlayPermission: Preference? = findPreference("overlay_permission_enabled")
        overlayPermission?.setOnPreferenceClickListener {
            checkOverlayPermissionAndRequest()
            true
        }

        val accessibilityService: Preference? = findPreference("enable_accessibility_service")
        accessibilityService?.setOnPreferenceClickListener {
            showAccessibilityExplanationDialog()
            true
        }
    }

    private fun startMediaProjectionService() {
        try {
            val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val resultCode = prefs.getInt("media_projection_result_code", -1)
            val dataString = prefs.getString("media_projection_data", null)

            if (resultCode != -1 && dataString != null) {
                // We have saved permission data, start the service
                val serviceIntent = Intent(requireContext(), com.lloir.ornaassistant.services.MediaProjectionScreenReader::class.java).apply {
                    action = com.lloir.ornaassistant.services.MediaProjectionScreenReader.ACTION_START
                    putExtra(com.lloir.ornaassistant.services.MediaProjectionScreenReader.EXTRA_RESULT_CODE, resultCode)
                    // Note: We'd need to reconstruct the Intent from the string, which is complex
                    // For now, just request permission again
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireContext().startForegroundService(serviceIntent)
                } else {
                    requireContext().startService(serviceIntent)
                }
                showToast("Modern screen reader started")
            } else {
                // No saved permission, need to request again
                showMediaProjectionDialog()
            }
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Failed to start MediaProjection service", e)
            showToast("Failed to start screen reader")
        }
    }

    private fun checkOverlayPermissionFirst(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(requireContext())) {
            AlertDialog.Builder(requireContext())
                .setTitle("Overlay Permission Required")
                .setMessage("This overlay requires permission to draw over other apps.")
                .setPositiveButton("Grant Permission") { _, _ ->
                    checkOverlayPermissionAndRequest()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            onGranted()
        }
    }

    private fun showMediaProjectionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Modern Screen Reader")
            .setMessage("The modern screen reader uses screen capture to read the game. This is more reliable than accessibility service.")
            .setPositiveButton("Grant Permission") { _, _ ->
                startActivity(Intent(requireContext(), com.lloir.ornaassistant.activities.PermissionActivity::class.java))
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Revert to accessibility method
                AppSettings.setScreenReaderMethod("accessibility")
                val screenReaderMethod: ListPreference? = findPreference("screen_reader_method")
                screenReaderMethod?.value = "accessibility"
            }
            .show()
    }

    private fun showAccessibilitySetupInfo() {
        AlertDialog.Builder(requireContext())
            .setTitle("Classic Screen Reader")
            .setMessage("The classic screen reader uses Android's accessibility service. Please enable it in settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (e: Exception) {
                    showToast("Unable to open Accessibility settings.")
                }
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun showAccessibilityExplanationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Accessibility Permission Needed")
            .setMessage("This permission is required for the screen reader. It will only read Orna and nothing else.")
            .setPositiveButton("Go to Settings") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (e: Exception) {
                    showToast("Unable to open Accessibility settings.")
                }
            }
            .setNegativeButton("Cancel") { _, _ -> showCancellationDialog() }
            .show()
    }

    private fun showCancellationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Functionality Limited")
            .setMessage("Without Accessibility Service, the overlays will not function correctly.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun checkOverlayPermissionAndRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(requireContext())) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${requireContext().packageName}".toUri()
                )
            )
        } else {
            showToast("Overlay permission already granted.")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}