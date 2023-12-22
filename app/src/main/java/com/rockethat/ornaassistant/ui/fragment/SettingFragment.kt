package com.rockethat.ornaassistant.ui.fragment

import UpdateChecker
import android.content.Intent
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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey)

        // Setup preference for enabling Accessibility Service
        findPreference<Preference>("enable_accessibility_service")?.setOnPreferenceClickListener {
            showAccessibilityExplanationDialog()
            true
        }

        // Setup preference for checking updates
        findPreference<Preference>("check_updates")?.setOnPreferenceClickListener {
            UpdateChecker.checkForUpdates(requireContext())
            true
        }

        // Setup preference for enabling Notification Service
        findPreference<Preference>("enable_notifications")?.setOnPreferenceClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                showNotificationPermissionDialog()
            } else {
                // For older versions, direct handling is not required as notification permission is granted by default
            }
            true
        }

        // Listen for changes in the theme preference
        findPreference<ListPreference>("theme_preference")?.setOnPreferenceChangeListener { _, newValue ->
            applyTheme(newValue as String)
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
            .setMessage("This permission is needed for the screen reader, which will only read Orna the RPG and nothing else.")
            .setPositiveButton("Go to Settings") { _, _ ->
                try {
                    val accessibilitySettingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(accessibilitySettingsIntent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Unable to open Accessibility settings.", Toast.LENGTH_SHORT).show()
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
                    // Notify the user if the settings cannot be opened
                    Toast.makeText(requireContext(), "Unable to open notification settings.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}