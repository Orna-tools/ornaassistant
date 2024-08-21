package com.lloir.ornaassistant

//import AppDrawer
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
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
        private const val ACTION_UPDATE_NOTIFICATION = "com.rockethat.ornaassistant.UPDATE_NOTIFICATION"
        private const val EXTRA_NOTIFICATION_ENABLED = "enabled"
    }

    private lateinit var pager: ViewPager2
    private lateinit var adapter: FragmentAdapter

    private val notificationReceiver = NotificationReceiver()

    inner class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val enabled = intent?.getBooleanExtra(EXTRA_NOTIFICATION_ENABLED, false) ?: false
            handlePersistentNotificationPreference(enabled)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeViews()
        setupComposeView()
        createNotificationChannel()

        val filter = IntentFilter(ACTION_UPDATE_NOTIFICATION)
        registerReceiver(notificationReceiver, filter)
    }

    private fun initializeViews() {
        pager = findViewById(R.id.pager)
        adapter = FragmentAdapter(supportFragmentManager, lifecycle)
        pager.adapter = adapter
    }

    private fun setupComposeView() {
        val composeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {AppDrawer(this@MainActivity) }
    }

    private fun updateMainFragment() {
        if (pager.currentItem == 0 && adapter.fragments.size >= 1) {
            val fragment = adapter.fragments[0] as MainFragment
            fragment.view?.let { view ->
                // Update the 7-day chart
                fragment.drawChart(view, R.id.cWeeklyDungeons, 7)
                // Update the 14-day chart
                fragment.drawChart(view, R.id.cCustomDungeons, 14)
                // Add more drawChart calls if you have more charts or custom days
            }
        }
    }

    private fun createNotificationChannel() {
        val channelName = "Persistent Notification"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
            description = "Channel for persistent notification"
        }
        val notificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java)!!
        notificationManager.createNotificationChannel(channel)
    }

    fun handlePersistentNotificationPreference(enabled: Boolean) {
        val notificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java)!!

        if (enabled) {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("App is Running")
                .setContentText("Tap to open.")
                .setSmallIcon(R.drawable.ric_notification)
                .setOngoing(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } else {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(notificationReceiver)
    }
}