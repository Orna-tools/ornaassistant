package com.rockethat.ornaassistant

import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.rockethat.ornaassistant.ui.fragment.FragmentAdapter
import com.rockethat.ornaassistant.ui.fragment.MainFragment

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {

    private lateinit var tableLayout: TabLayout
    private lateinit var pager: ViewPager2
    private lateinit var adapter: FragmentAdapter
    private val TAG = "OrnaMainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tableLayout = findViewById(R.id.tab_layout)
        pager = findViewById(R.id.pager)
        adapter = FragmentAdapter(supportFragmentManager, lifecycle)
        pager.adapter = adapter

        tableLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.text) {
                    "Main" -> {
                        pager.currentItem = 0
                        if (adapter.frags.size >= 1) {
                            (adapter.frags[0] as MainFragment).drawWeeklyChart()
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tableLayout.selectTab(tableLayout.getTabAt(position))
            }
        })

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

        showPermissionExplanationDialog()
    }

    private fun showPermissionExplanationDialog() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Permission Request")
            .setMessage("This app requires certain permissions to function properly. " +
                    "Please grant Overlay Permission to proceed. This is needed to display the item assessor in game.")
            .setPositiveButton("Continue") { _, _ ->
                checkOverlayPermission()
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Handle the cancellation as needed
            }
            .create()

        alertDialog.show()
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        }
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQ_CODE = 5469 // Arbitrary number
    }

    override fun onResume() {
        super.onResume()
        if (!isAccessibilityEnabled()) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        if (tableLayout.selectedTabPosition == 0 && adapter.frags.size >= 1) {
            (adapter.frags[0] as MainFragment).drawWeeklyChart()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    var sharedPreferenceChangeListener = OnSharedPreferenceChangeListener { _, key ->
        if (key == "your_key") {
            // Respond to the preference change
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_preference -> {
                goToSettingsActivity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun goToSettingsActivity() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            // Handle the result of the overlay permission request
        }
    }

    fun isAccessibilityEnabled(): Boolean {
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                this.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: SettingNotFoundException) {
            Log.d(TAG, "Error finding setting, default accessibility to not found: ${e.message}")
        }

        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val splitter = SimpleStringSplitter(':')
            splitter.setString(settingValue)

            while (splitter.hasNext()) {
                val accessibilityService = splitter.next()
                if (accessibilityService.contains(packageName, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }
}
