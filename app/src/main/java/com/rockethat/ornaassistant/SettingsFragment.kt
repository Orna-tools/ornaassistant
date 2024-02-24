package com.rockethat.ornaassistant

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class AppSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey)
    }
}