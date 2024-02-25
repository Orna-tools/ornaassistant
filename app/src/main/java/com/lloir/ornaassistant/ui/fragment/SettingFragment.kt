package com.lloir.ornaassistant.ui.fragment

import android.os.Bundle
import com.lloir.ornaassistant.R

import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import android.content.SharedPreferences




class SettingFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey)

        val prefs = context?.let {
            PreferenceManager.getDefaultSharedPreferences(it)
        }
        val editor: SharedPreferences.Editor? = prefs?.edit()
        if (editor != null) {
            editor.putBoolean("PREF_NAME", false)
            editor.apply()
        }
    }
}