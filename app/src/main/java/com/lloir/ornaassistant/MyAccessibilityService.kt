package com.lloir.ornaassistant

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.WindowManager
import android.graphics.Rect
import android.os.Build
import android.view.LayoutInflater
import androidx.annotation.RequiresApi
import kotlinx.coroutines.delay
import kotlin.system.measureTimeMillis

class MyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "OrnaAssist"
        private val SUPPORTED_PACKAGES = listOf("playorna.com.orna", "com.discord", "com.avalon.rpg").toTypedArray()
        private const val TIMEOUT = 500
        private val IGNORED_STRINGS = listOf("DROP", "New", "SEND TO KEEP", "Map")
    }

    private lateinit var state: MainState
    private var mDebugDepth = 0
    private var getChildCalls = 0
    private var lastEvent = 0L


    override fun onCreate() {
        super.onCreate()
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        state = MainState(
            getSystemService(WINDOW_SERVICE) as WindowManager, applicationContext,
            inflater.inflate(R.layout.notification_layout, null),
            inflater.inflate(R.layout.wayvessel_overlay, null),
            inflater.inflate(R.layout.kg_layout, null),
            inflater.inflate(R.layout.assess_layout, null),
            this
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.source?.let { source ->
            resetDebugInfo()
            parseScreen(source, ArrayList(), 0, 0)
            state.processData(event.packageName.toString(), ArrayList())
            lastEvent = System.currentTimeMillis()
        }
    }

    private fun resetDebugInfo() {
        mDebugDepth = 0
        getChildCalls = 0
    }

    private fun parseScreen(nodeInfo: AccessibilityNodeInfo?, data: ArrayList<ScreenData>, depth: Int, time: Long): Boolean {
        if (nodeInfo == null || timeShouldBeDelayed(nodeInfo, depth)) {
            // Handle null or delay
            val delayStartTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - delayStartTime < 5000) {
                // Do nothing, just wait
            }
            return false
        }
        return processNodeInfo(nodeInfo, data, depth, time)
    }

    private fun timeShouldBeDelayed(nodeInfo: AccessibilityNodeInfo?, depth: Int) =
        nodeInfo == null || depth > 250

    private fun processNodeInfo(nodeInfo: AccessibilityNodeInfo, data: ArrayList<ScreenData>, depth: Int, time: Long): Boolean {
        var done = false
        if (!IGNORED_STRINGS.contains(nodeInfo.text.toString())) {
            data.add(createScreenData(nodeInfo, time))
        }
        if (!done) {
            done = processChildren(nodeInfo, data, depth)
        }
        nodeInfo.recycle()
        return done
    }

    private fun createScreenData(nodeInfo: AccessibilityNodeInfo, time: Long): ScreenData {
        val rect = Rect()
        nodeInfo.getBoundsInScreen(rect)
        return ScreenData(nodeInfo.text.toString(), rect, time, mDebugDepth, nodeInfo)
    }

    private fun processChildren(nodeInfo: AccessibilityNodeInfo, data: ArrayList<ScreenData>, depth: Int): Boolean {
        val count = nodeInfo.childCount
        for (i in 0 until count) {
            getChildCalls++
            var child: AccessibilityNodeInfo? = null
            val thisTime = measureTimeMillis { child = nodeInfo.getChild(i) }
            if (child != null) {
                mDebugDepth++
                val done = parseScreen(child, data, depth + i, thisTime)
                mDebugDepth--
                if (done) {
                    return true
                }
            }
        }
        return false
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = this.serviceInfo
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.notificationTimeout = TIMEOUT.toLong()
        info.packageNames = SUPPORTED_PACKAGES
        this.serviceInfo = info
        Log.i(TAG, "onServiceConnected called")
    }
}
