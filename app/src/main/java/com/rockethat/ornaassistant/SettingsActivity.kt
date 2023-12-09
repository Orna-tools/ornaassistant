package com.rockethat.ornaassistant

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.rockethat.ornaassistant.ui.fragment.SettingsFragment

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content_preference, SettingsFragment())
            .commit()
        setContentView(R.layout.activity_settings)
    }
}