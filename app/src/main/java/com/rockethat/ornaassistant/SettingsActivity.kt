package com.rockethat.ornaassistant

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.rockethat.ornaassistant.ui.fragment.SettingsFragment
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Creating new fragment object
        val fragment = SettingsFragment()

        val transaction = supportFragmentManager.beginTransaction()

        // Replace the FrameLayout specified by the fragmentContainerId with fragment
        transaction.replace(R.id.main_content_frame, fragment)

        transaction.commit()
    }
}

@Serializable
data class GithubRelease(val tag_name: String)

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey)

        val updatePreference: Preference? = findPreference("check_updates")
        updatePreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            checkForUpdates()
            true
        }
    }

    private fun checkForUpdates() {
        Thread {
            try {
                val url = URL("https://api.github.com/repos/Orna-tools/ornaassistant/releases/latest")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val reader = conn.inputStream.bufferedReader()

                var release: GithubRelease? = null
                reader.use {
                    val response = it.readText()
                    release = Json { ignoreUnknownKeys = true }.decodeFromString<GithubRelease>(response)
                }

                // Now release.tag_name contains the version of the latest release

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}