package com.rockethat.ornaassistant.ui.fragment

import UpdateChecker
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.rockethat.ornaassistant.R

class SettingsFragment : PreferenceFragmentCompat() {

    private val REQUEST_CODE_OVERLAY_PERMISSION = 123

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

        findPreference<ListPreference>("theme_preference")?.setOnPreferenceChangeListener { _, newValue ->
            applyTheme(newValue as String)
            true
        }

        findPreference<Preference>("enable_overlay_permission")?.setOnPreferenceClickListener {
            checkOverlayPermissionAndRequest()
            true
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(
                requireContext()
            )
        ) {
            // Overlay permission already granted
            Toast.makeText(
                requireContext(),
                "Overlay permission already granted",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // Overlay permission not granted, show dialog to request
            AlertDialog.Builder(requireContext())
                .setTitle("Overlay Permission Required")
                .setMessage("This app needs the overlay permission to display information on top of other apps. Please grant this permission in settings.")
                .setPositiveButton("Open Settings") { dialog, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${requireContext().packageName}")
                    )
                    startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    // You might want to handle the case where the user cancels (e.g., disable functionality)
                }
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(
                    requireContext()
                )
            ) {
                // Overlay permission granted
                Toast.makeText(requireContext(), "Overlay permission granted", Toast.LENGTH_SHORT)
                    .show()
                // You might want to enable functionality that relies on the overlay permission here
            } else {
                // Overlay permission denied
                Toast.makeText(requireContext(), "Overlay permission denied", Toast.LENGTH_SHORT)
                    .show()
                AlertDialog.Builder(requireContext())
                    .setTitle("Overlay Permission Denied")
                    .setMessage("The Overlay will not work, to enable it, please grant the permission in settings.")
                    .setPositiveButton("Open Settings") { dialog, _ ->
                    }
            }
        }
    }
}