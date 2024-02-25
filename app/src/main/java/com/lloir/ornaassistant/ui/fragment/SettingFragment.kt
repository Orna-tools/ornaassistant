package com.lloir.ornaassistant.ui.fragment

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.lloir.ornaassistant.R

class SettingFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey)

        context?.let {
            val prefs = PreferenceManager.getDefaultSharedPreferences(it)
            val editor: SharedPreferences.Editor? = prefs?.edit()

            editor?.apply {
                putBoolean("PREF_NAME", false)
                apply()
            }
        }

        // Setup preference for enabling Notification Service
        findPreference<Preference>("enable_notifications")?.setOnPreferenceClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                showNotificationPermissionDialog()
            }
            true
        }
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