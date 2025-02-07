package com.lloir.ornaassistant

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.lloir.ornaassistant.overlays.Overlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.main_content_frame, SettingsFragment())
            commit()
        }
    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey)

        // ✅ FIX: Ensure correct type is used for each preference
        val inviterOverlayCheckBox: CheckBoxPreference? = findPreference("inviter_overlay")
        val sessionOverlayCheckBox: CheckBoxPreference? = findPreference("session_overlay")
        val kgOverlayCheckBox: CheckBoxPreference? = findPreference("kg")
        val assessOverlayCheckBox: CheckBoxPreference? = findPreference("assess_overlay")

        inviterOverlayCheckBox?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                Overlay.startOverlay(requireContext(), "inviter")
            } else {
                Overlay.stopOverlay()
            }
            true
        }

        sessionOverlayCheckBox?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                Overlay.startOverlay(requireContext(), "session")
            } else {
                Overlay.stopOverlay()
            }
            true
        }

        kgOverlayCheckBox?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                Overlay.startOverlay(requireContext(), "kg")
            } else {
                Overlay.stopOverlay()
            }
            true
        }

        assessOverlayCheckBox?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                Overlay.startOverlay(requireContext(), "assess")
            } else {
                Overlay.stopOverlay()
            }
            true
        }

        // ✅ Overlay Permission
        val overlayPermission: Preference? = findPreference("overlay_permission_enabled")
        overlayPermission?.setOnPreferenceClickListener {
            checkOverlayPermissionAndRequest()
            true
        }

        // ✅ Accessibility Permission
        val accessibilityService: Preference? = findPreference("enable_accessibility_service")
        accessibilityService?.setOnPreferenceClickListener {
            showAccessibilityExplanationDialog()
            true
        }
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
                    Uri.parse("package:${requireContext().packageName}")
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
