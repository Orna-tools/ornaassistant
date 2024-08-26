package com.lloir.ornaassistant.ui.fragment

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.UpdateChecker

class SettingsFragment : PreferenceFragmentCompat() {

    private val REQUEST_CODE_OVERLAY_PERMISSION = 123
    private val NOTIFICATION_ID = 1234
    private val CHANNEL_ID = "persistent_notification_channel"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey)

        findPreference<Preference>("enable_accessibility_service")?.setOnPreferenceClickListener {
            showAccessibilityExplanationDialog()
            true
        }

        findPreference<Preference>("check_updates")?.setOnPreferenceClickListener {
            UpdateChecker.checkForUpdates(requireContext())
            true
        }

        findPreference<Preference>("enable_notifications")?.setOnPreferenceClickListener {
            showNotificationPermissionDialog()
            true
        }

        val notificationSwitch: SwitchPreferenceCompat? = findPreference("persistent_notification_enabled")
        notificationSwitch?.setOnPreferenceChangeListener { _, newValue ->
            handlePersistentNotificationPreference(newValue as Boolean)
            true
        }

        findPreference<ListPreference>("theme_preference")?.setOnPreferenceChangeListener { _, newValue ->
            applyTheme(newValue as String)
            true
        }

        val overlaySwitch: SwitchPreferenceCompat? = findPreference("overlay_permission_enabled")
        overlaySwitch?.isChecked = Settings.canDrawOverlays(requireContext())

        overlaySwitch?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean
            if (isEnabled) {
                checkOverlayPermissionAndRequest()
            } else {
                // Optional: Handle disabling the overlay permission (if necessary)
            }
            true
        }

        createNotificationChannel()
    }

    private fun applyTheme(themeValue: String) {
        Log.d("ThemePreference", "Applying theme: $themeValue")
        val nightMode = when (themeValue) {
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "device" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
        }

        if (nightMode != AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
            AppCompatDelegate.setDefaultNightMode(nightMode)
        } else {
            Log.w("ThemePreference", "Unknown theme mode: $themeValue")
        }
    }

    private fun showAccessibilityExplanationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Accessibility Permission Needed")
            .setMessage("This permission is needed for the screen reader, which will only read Orna and nothing else.")
            .setPositiveButton("Go to Settings") { _, _ ->
                try {
                    val accessibilitySettingsIntent =
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    startActivity(accessibilitySettingsIntent)
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Unable to open Accessibility settings.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                showCancellationDialog()
            }
            .show()
    }

    private fun showCancellationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Functionality Limited")
            .setMessage("Without Accessibility Service, the overlays will not work.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showNotificationPermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Notification Permission Needed")
            .setMessage("This app requires notification permission to send you updates. Please enable notifications for this app in Settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent().apply {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                }

                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Unable to open notification settings.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkOverlayPermissionAndRequest() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(requireContext())) {
            // Overlay permission already granted or not required (below Android M)
            Toast.makeText(requireContext(), "Overlay permission already granted", Toast.LENGTH_SHORT).show()
        } else {
            // Overlay permission not granted, request permission
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${requireContext().packageName}")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            val overlaySwitch: SwitchPreferenceCompat? = findPreference("overlay_permission_enabled")
            overlaySwitch?.isChecked = Settings.canDrawOverlays(requireContext())
        }
    }

    private fun createNotificationChannel() {
        val channelName = "Persistent Notification"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
            description = "Channel for persistent notification"
        }
        val notificationManager = ContextCompat.getSystemService(requireContext(), NotificationManager::class.java)!!
        notificationManager.createNotificationChannel(channel)
    }

    private fun handlePersistentNotificationPreference(enabled: Boolean) {
        val notificationManager = ContextCompat.getSystemService(requireContext(), NotificationManager::class.java)!!

        if (enabled) {
            val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setContentTitle("App is Running")
                .setContentText("Tap to open.")
                .setSmallIcon(R.drawable.ric_notification)
                .setOngoing(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } else {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }
}
