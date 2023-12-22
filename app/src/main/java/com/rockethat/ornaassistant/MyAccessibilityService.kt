package com.rockethat.ornaassistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import kotlin.system.measureTimeMillis


class MyAccessibilityService() : AccessibilityService() {

    private val TAG = "OrnaAssist"
    private var mDebugDepth = 0

    var lastEvent: Long = 0
    var getChildCalls = 0
    var state: MainState? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        state = MainState(
            getSystemService(WINDOW_SERVICE) as WindowManager, applicationContext,
            inflater.inflate(R.layout.notification_layout, null),
            inflater.inflate(R.layout.wayvessel_overlay, null),
            inflater.inflate(R.layout.assess_layout, null),
            this
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
        if (p0 == null/* || p0.packageName == null || !p0.packageName.contains("orna")*/) {
            return
        }

        if (p0.source == null) {
            return
        }

        mDebugDepth = 0

        var values = ArrayList<ScreenData>()
        var mNodeInfo: AccessibilityNodeInfo? = p0.source

        if (p0?.source != null) {
            getChildCalls = 0
            parseScreen(mNodeInfo, values, 0, 0)
        }

        state?.processData(p0.packageName.toString(), values)

        lastEvent = System.currentTimeMillis()
    }

    private fun parseScreen(
        mNodeInfo: AccessibilityNodeInfo?,
        data: ArrayList<ScreenData>?,
        depth: Int,
        time: Long
    ): Boolean {
        var done = false
        if (mNodeInfo == null) return done
        if (depth > 250)
        {
            return true
        }

        //Log.d(TAG, "$text #${mNodeInfo.text}#")
        if (mNodeInfo.text != null) {
            when (mNodeInfo.text.toString()) {
                "DROP" -> done = true
                "New" -> done = true // Inventory
                "SEND TO KEEP" -> done = true // Inventory
                "Map" -> done = true
                //"Character" -> done = true
            }
            val rect = Rect()
            mNodeInfo.getBoundsInScreen(rect)
            data?.add(ScreenData(mNodeInfo.text.toString(), rect, time, mDebugDepth, mNodeInfo))
        }

        if (!done) {
            val count = mNodeInfo.childCount
            for (i in 0 until count) {
                getChildCalls++
                var child: AccessibilityNodeInfo?
                val thistime = measureTimeMillis { child = mNodeInfo.getChild(i) }
                if (child != null) {
                    mDebugDepth++;
                    done = parseScreen(child, data, depth + i, thistime)
                    if (done) {
                        break
                    }
                    mDebugDepth--;
                }
            }
        }

        mNodeInfo.recycle()

        return done
    }

    override fun onInterrupt() {
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL or AccessibilityServiceInfo.FEEDBACK_SPOKEN
            notificationTimeout = 100
            packageNames = arrayOf("playorna.com.orna", "com.discord")
            flags = AccessibilityServiceInfo.DEFAULT or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
        }
    }
}