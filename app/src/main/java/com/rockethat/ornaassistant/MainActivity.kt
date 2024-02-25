import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.rockethat.ornaassistant.R
import com.rockethat.ornaassistant.ui.fragment.FragmentAdapter
import com.rockethat.ornaassistant.ui.fragment.KingdomFragment
import com.rockethat.ornaassistant.ui.fragment.MainFragment
import android.os.Build
import com.rockethat.ornaassistant.SettingsActivity

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {

    private lateinit var tableLayout: TabLayout
    private lateinit var pager: ViewPager2
    private lateinit var adapter: FragmentAdapter
    private val TAG = "OrnaMainActivity"
    private val ACCESSIBILITY_SERVICE_NAME = "OrnaAssistant service"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tableLayout = findViewById(R.id.tab_layout)
        pager = findViewById(R.id.pager)

        adapter = FragmentAdapter(supportFragmentManager, lifecycle)
        pager.adapter = adapter

        TabLayoutMediator(tableLayout, pager) { tab, position ->
            tab.text = when (position) {
                0 -> "Main"
                1 -> "Kingdom"
                2 -> "Orna Guide"
                3 -> " Orna Towers"
                else -> null
            }
        }.attach()



        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

        if (!isAccessibilityEnabled()) {
            requestAccessibilityPermission()
        }

        when (tableLayout.selectedTabPosition) {
            0 -> {
                // Handle tab 0 being selected
            }

            1 -> {
                // Handle tab 1 being selected
            }
            2 -> {
                // Handle tab 2 being selected
            }

            3 -> {
                // Handle tab 3 being selected
            }
        }
    }

    override fun onPause() {
    super.onPause()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
}



    fun isAccessibilityEnabled(): Boolean {
        var accessibilityEnabled = 0
        val accessibilityFound = false
        try {
            accessibilityEnabled =
                Settings.Secure.getInt(this.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
            Log.d(TAG, "ACCESSIBILITY: $accessibilityEnabled")
        } catch (e: SettingNotFoundException) {
            Log.d(TAG, "Error finding setting, default accessibility to not found: " + e.message)
        }
        val mStringColonSplitter = SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            Log.d(TAG, "***ACCESSIBILIY IS ENABLED***: ")
            val settingValue: String = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            Log.d(TAG, "Setting: $settingValue")
            mStringColonSplitter.setString(settingValue)
            while (mStringColonSplitter.hasNext()) {
                val accessabilityService = mStringColonSplitter.next()
                Log.d(TAG, "Setting: $accessabilityService")
                if (accessabilityService.toLowerCase().contains(
                        packageName.toLowerCase()
                    )
                ) {
                    Log.d(
                        TAG,
                        "We've found the correct setting - accessibility is switched on!"
                    )
                    return true
                }
            }
            Log.d(TAG, "***END***")
        } else {
            Log.d(TAG, "***ACCESSIBILIY IS DISABLED***")
        }
        return accessibilityFound
    }

    private fun requestAccessibilityPermission() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app requires Accessibility service to function properly. Please turn it on from the Settings?")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(this, "App may not function correctly without required permissions.", Toast.LENGTH_LONG)
                    .show()
            }
            .create()
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_preference -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private val sharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                "accessibility_enabled" -> {
                    val isEnabled = sharedPreferences.getBoolean(key, false)
                    Log.d("SharedPreferences", "Accessibility is now " + if (isEnabled) "Enabled" else "Disabled")
                }
            }
        }
}