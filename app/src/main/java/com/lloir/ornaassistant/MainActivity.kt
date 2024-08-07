package com.lloir.ornaassistant

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.lloir.ornaassistant.ui.fragment.FragmentAdapter
import com.lloir.ornaassistant.ui.fragment.KingdomFragment
import com.lloir.ornaassistant.ui.fragment.MainFragment

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {

    private lateinit var tableLayout: TabLayout
    private lateinit var pager: ViewPager2
    private lateinit var adapter: FragmentAdapter
    private val TAG = "OrnaMainActivity"
    private val ACCESSIBILITY_SERVICE_NAME = "OrnaAssistant service"
    private val NOTIFICATION_ID = 1234
    private val CHANNEL_ID = "persistent_notification_channel"

    companion object {
        const val BOUNDING_BOX_LEFT = 55
        const val BOUNDING_BOX_TOP = 331
        const val BOUNDING_BOX_RIGHT = 765
        const val BOUNDING_BOX_BOTTOM = 1422
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tableLayout = findViewById(R.id.tab_layout)
        pager = findViewById(R.id.pager)
        adapter = FragmentAdapter(this)
        pager.adapter = adapter

        val tabTitles = arrayOf("Main", "Kingdom", "Orna Guide", "Orna Tower")

        TabLayoutMediator(tableLayout, pager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                handleTabSelection(position)
            }
        })
    }
    override fun onStart() {
        super.onStart()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    override fun onStop() {
        super.onStop()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    override fun onResume() {
        super.onResume()
        if (!isAccessibilityEnabled()) {
            requestAccessibilityPermission()
        }
    }

    class FragmentAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

        private val fragmentList = arrayOfNulls<Fragment>(itemCount)

        override fun getItemCount(): Int = 4


        override fun createFragment(position: Int): Fragment {
            val fragment = when (position) {
                0 -> MainFragment()
                1 -> KingdomFragment()
                2 -> OrnaGuideFragment()
                3 -> OrnaTowerFragment()
                else -> throw IllegalStateException("Invalid position for pager")
            }
            fragmentList[position] = fragment
            return fragment
        }

        fun getFragment(position: Int): Fragment? {
            return fragmentList[position]
        }
    }

    private fun handleTabSelection(tabPosition: Int) {
        pager.currentItem = tabPosition

        val frag = adapter.getFragment(tabPosition)

        when (frag) {
            is KingdomFragment -> {
                frag.updateSeenList()
            }

            is OrnaGuideFragment -> {
                frag.refreshWebpage()
            }

            is OrnaTowerFragment -> {
                frag.refreshWebpage()
            }

            else -> Unit
        }
    }


    private fun getFragmentTag(viewPagerId: Int, fragmentPosition: Int) =
        "android:switcher:$viewPagerId:$fragmentPosition"

    fun isAccessibilityEnabled(): Boolean {
        var accessibilityEnabled = getAccessibilitySetting()
        if (accessibilityEnabled == 1) {
            val settingValue: String = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return isOurServiceEnabled(settingValue)
        } else {
            Log.d(TAG, "***ACCESSIBILIY IS DISABLED***")
            return false
        }
    }

    private fun getAccessibilitySetting() : Int {
        return try {
            Settings.Secure.getInt(this.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: SettingNotFoundException) {
            Log.d(TAG, "Error finding setting, default accessibility to not found: " + e.message)
            0
        }
    }

    private fun isOurServiceEnabled(settingValue: String): Boolean {
        val mStringColonSplitter = SimpleStringSplitter(':')
        mStringColonSplitter.setString(settingValue)
        while (mStringColonSplitter.hasNext()) {
            val accessibilityService = mStringColonSplitter.next()
            Log.d(TAG, "Setting: $accessibilityService")
            if (accessibilityService.toLowerCase().contains(
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
        return false
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