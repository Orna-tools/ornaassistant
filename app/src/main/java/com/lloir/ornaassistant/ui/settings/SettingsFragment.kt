package com.lloir.ornaassistant.ui.settings

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.Preference
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.domain.language.LanguageManager
import com.lloir.ornaassistant.domain.repository.ItemDatabase
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var itemDatabase: ItemDatabase

    @Inject
    lateinit var languageManager: LanguageManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        setupDatabaseInfoPreference()
        setupLanguagePreferences()
        setupDatabaseDownloadPreference()
    }

    private fun setupDatabaseInfoPreference() {
        val databaseInfoPref = findPreference<Preference>("database_info")
        databaseInfoPref?.let { pref ->
            lifecycleScope.launch {
                try {
                    // Ensure database is initialized
                    itemDatabase.initialize(requireContext())
                    val stats = itemDatabase.getStats()

                    pref.summary = getString(
                        R.string.database_stats_format,
                        stats.totalItems,
                        stats.bossItems,
                        stats.tiersRepresented
                    )
                } catch (e: Exception) {
                    pref.summary = getString(R.string.database_not_loaded)
                }
            }
        }
    }

    private fun setupLanguagePreferences() {
        // App language preference
        val appLanguagePref = findPreference<ListPreference>("app_language")
        appLanguagePref?.setOnPreferenceChangeListener { _, newValue ->
            // Recreate activity to apply language change
            activity?.recreate()
            true
        }

        // Database language preference
        val dbLanguagePref = findPreference<ListPreference>("database_language")
        dbLanguagePref?.summary = "Current: ${getCurrentLanguageName(itemDatabase.getLanguage())}"
        dbLanguagePref?.setOnPreferenceChangeListener { _, newValue ->
            // Update the database language
            itemDatabase.setLanguage(newValue as String)
            dbLanguagePref.summary = "Current: ${getCurrentLanguageName(newValue)}"
            true
        }
    }

    private fun getCurrentLanguageName(languageCode: String): String {
        val dbLanguageNames = resources.getStringArray(R.array.database_language_names)
        val dbLanguageCodes = resources.getStringArray(R.array.database_language_codes)
        val index = dbLanguageCodes.indexOf(languageCode)
        return if (index >= 0) dbLanguageNames[index] else "Unknown"
    }

    private fun setupDatabaseDownloadPreference() {
        val downloadPref = findPreference<Preference>("download_database")
        downloadPref?.setOnPreferenceClickListener {
            lifecycleScope.launch {
                val language = languageManager.getDatabaseLanguage()

                // Show progress dialog
                val progressDialog = ProgressDialog(requireContext()).apply {
                    setMessage("Downloading database for ${getCurrentLanguageName(language)}...")
                    setCancelable(false)
                    show()
                }

                try {
                    val success = itemDatabase.downloadDatabase(requireContext(), language)
                    progressDialog.dismiss()

                    if (success) {
                        // Reload database info
                        setupDatabaseInfoPreference()
                        Toast.makeText(
                            requireContext(),
                            "Database downloaded successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to download database",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            true
        }
    }
}
