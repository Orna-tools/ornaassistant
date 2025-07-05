package com.lloir.ornaassistant.ui.settings

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.Preference
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.domain.assessment.EnhancedItemDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        setupDatabaseInfoPreference()
    }
    
    private fun setupDatabaseInfoPreference() {
        val databaseInfoPref = findPreference<Preference>("database_info")
        databaseInfoPref?.let { pref ->
            lifecycleScope.launch {
                try {
                    // Ensure database is initialized
                    EnhancedItemDatabase.initialize(requireContext())
                    val stats = EnhancedItemDatabase.getStats()
                    
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
}