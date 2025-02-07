package com.lloir.ornaassistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.lloir.ornaassistant.ui.fragment.FragmentAdapter
import com.lloir.ornaassistant.ui.fragment.MainFragment

class MainActivity : AppCompatActivity() {

    companion object {
        private const val NOTIFICATION_ID = 1234
        private const val CHANNEL_ID = "persistent_notification_channel"
        private const val ACTION_UPDATE_NOTIFICATION = "com.lloir.ornaassistant.UPDATE_NOTIFICATION"
        private const val EXTRA_NOTIFICATION_ENABLED = "enabled"
    }

    private lateinit var pager: ViewPager2
    private lateinit var adapter: FragmentAdapter
    private val notificationReceiver = NotificationReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupViewPager()
        setupComposeView()
        createNotificationChannel()
        registerNotificationReceiver()

        // ✅ Show Changelog Popup on First Launch After Update
        showChangelogPopup()
    }

    private fun setupViewPager() {
        pager = findViewById(R.id.pager)
        adapter = FragmentAdapter(supportFragmentManager, lifecycle)
        pager.adapter = adapter
    }

    private fun setupComposeView() {
        findViewById<ComposeView>(R.id.compose_view).setContent {
            AppDrawer(this@MainActivity)
        }
    }

    private fun updateMainFragment() {
        val fragment = adapter.fragments.getOrNull(0) as? MainFragment ?: return
        fragment.view?.let { view ->
            fragment.drawChart(view, R.id.cWeeklyDungeons, 7)
            fragment.drawChart(view, R.id.cCustomDungeons, 14)
        }
    }

    private fun createNotificationChannel() {
        val notificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java)
        notificationManager?.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Persistent Notification", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Channel for persistent notification"
            }
        )
    }

    private fun registerNotificationReceiver() {
        registerReceiver(notificationReceiver, IntentFilter(ACTION_UPDATE_NOTIFICATION))
    }

    fun handlePersistentNotificationPreference(enabled: Boolean) {
        val notificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        if (enabled) {
            notificationManager.notify(NOTIFICATION_ID, NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("App is Running")
                .setContentText("Tap to open.")
                .setSmallIcon(R.drawable.ric_notification)
                .setOngoing(true)
                .build()
            )
        } else {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(notificationReceiver)
    }

    private inner class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handlePersistentNotificationPreference(intent?.getBooleanExtra(EXTRA_NOTIFICATION_ENABLED, false) ?: false)
        }
    }

    // ✅ Show Changelog Popup on First Launch After Update
    private fun showChangelogPopup() {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val lastVersion = sharedPreferences.getInt("last_version", 0)
        val currentVersion = BuildConfig.VERSION_CODE  // Ensure `VERSION_CODE` is set in `build.gradle`

        if (lastVersion < currentVersion) {
            AlertDialog.Builder(this)
                .setTitle("What's New in Orna Assistant")
                .setMessage(getChangelogText())  // ✅ Displays changelog
                .setPositiveButton("OK") { _, _ ->
                    sharedPreferences.edit().putInt("last_version", currentVersion).apply()
                }
                .show()
        }
    }

    private fun getChangelogText(): String {
        return """
        🔹 **Recent Changes:**
        - Fixed overlay toggles in Settings
        - Fixed crashes when starting overlays
        - Improved update checker
        - Optimized accessibility settings

        🔴 **Current Bugs:**
        - Some items won’t scan → Press rename or wiggle screen to fix
        - Turning overlays off causes a crash

        ⚡ More fixes coming soon!
        """.trimIndent()
    }
}
