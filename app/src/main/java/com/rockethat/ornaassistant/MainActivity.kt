package com.rockethat.ornaassistant

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.rockethat.ornaassistant.ui.fragment.FragmentAdapter

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var pager: ViewPager2
    private lateinit var adapter: FragmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        tabLayout = findViewById(R.id.tab_layout)
        pager = findViewById(R.id.pager)
        adapter = FragmentAdapter(supportFragmentManager, lifecycle)
        pager.adapter = adapter

        // Set up the Toolbar
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Set up tab layout and pager
        setUpTabLayout()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (!hasOverlayPermission()) {
            showOverlayExplanationDialog()
        } else if (!isAccessibilityEnabled() && !sharedPreferences.getBoolean("HasShownAccessibilityDialog", false)) {
            showAccessibilityExplanationDialog()
        }
    }

    private fun setUpTabLayout() {
        TabLayoutMediator(tabLayout, pager) { tab, position ->
            tab.text = when (position) {
                0 -> "Main"
                1 -> "OrnaHub"
                2 -> "Kingdom"
                else -> null
            }
        }.attach()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                pager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }
        })
    }

    private fun hasOverlayPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun showOverlayExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Request")
            .setMessage("This app requires overlay permission to function properly.")
            .setPositiveButton("Continue") { _, _ ->
                requestOverlayPermission()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED, 0
        )
        return accessibilityEnabled == 1
    }

    private fun showAccessibilityExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Accessibility Permission Needed")
            .setMessage("This permission is needed to read the screen for specific features.")
            .setPositiveButton("OK") { _, _ ->
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                with(sharedPreferences.edit()) {
                    putBoolean("HasShownAccessibilityDialog", true)
                    apply()
                }
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (!isAccessibilityEnabled() && sharedPreferences.getBoolean("HasShownAccessibilityDialog", false)) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.item_preference) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (hasOverlayPermission() && !isAccessibilityEnabled()) {
                showAccessibilityExplanationDialog()
            }
        }
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQ_CODE = 5469
    }
}
