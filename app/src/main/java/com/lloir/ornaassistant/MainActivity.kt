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
import com.lloir.ornaassistant.settings.Settings
import com.lloir.ornaassistant.ui.fragment.FragmentAdapter
import com.lloir.ornaassistant.ui.fragment.MainFragment
import androidx.core.content.edit

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

        // Initialize settings first
        Settings.initialize(this)

        setupViewPager()
        setupComposeView()
        createNotificationChannel()
        registerNotificationReceiver()

        // Show screen reader setup dialog on first launch
        showScreenReaderSetupDialog()
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
            NotificationChannel(CHANNEL_ID, "Persistent Notification", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Channel for persistent notification"
                setShowBadge(false) // Don't show badge for persistent notifications
            }
        )
    }

    private fun registerNotificationReceiver() {
        val filter = IntentFilter(ACTION_UPDATE_NOTIFICATION)
        registerReceiver(notificationReceiver, filter)
    }

    fun handlePersistentNotificationPreference(enabled: Boolean) {
        val notificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return

        if (enabled) {
            // Create main app intent
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, mainIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Orna Assistant")
                .setContentText("Tap to open app")
                .setSmallIcon(R.drawable.ric_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)

            // Store preference
            getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                .edit {
                    putBoolean("persistent_notification_enabled", true)
                }

            android.util.Log.d("MainActivity", "Persistent notification enabled")
        } else {
            notificationManager.cancel(NOTIFICATION_ID)

            // Store preference
            getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                .edit {
                    putBoolean("persistent_notification_enabled", false)
                }

            android.util.Log.d("MainActivity", "Persistent notification disabled")
        }
    }

    private fun showScreenReaderSetupDialog() {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val hasShownSetup = sharedPreferences.getBoolean("has_shown_screen_reader_setup", false)

        if (!hasShownSetup) {
            AlertDialog.Builder(this)
                .setTitle("Choose Screen Reader Method")
                .setMessage("""
                    Orna Assistant offers two screen reading methods:
                    
                    🚀 Modern (Recommended): Uses advanced screen capture
                    📱 Classic: Uses accessibility service (fallback)
                    
                    You can change this later in Settings.
                """.trimIndent())
                .setPositiveButton("Modern") { _, _ ->
                    Settings.setScreenReaderMethod("media_projection")
                    startActivity(Intent(this, com.lloir.ornaassistant.activities.PermissionActivity::class.java))
                }
                .setNegativeButton("Classic") { _, _ ->
                    Settings.setScreenReaderMethod("accessibility")
                    showAccessibilitySetupInfo()
                }
                .setNeutralButton("Setup Later", null)
                .show()

            sharedPreferences.edit { putBoolean("has_shown_screen_reader_setup", true) }
        }
    }

    private fun showAccessibilitySetupInfo() {
        AlertDialog.Builder(this)
            .setTitle("Accessibility Setup")
            .setMessage("Please enable the Orna Assistant service in Android Accessibility Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to open accessibility settings", e)
                }
            }
            .setNegativeButton("Later", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error unregistering receiver", e)
        }
    }

    private inner class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val enabled = intent?.getBooleanExtra(EXTRA_NOTIFICATION_ENABLED, false) ?: false
            android.util.Log.d("MainActivity", "Notification receiver: enabled=$enabled")
            handlePersistentNotificationPreference(enabled)
        }
    }
}