package com.lloir.ornaassistant

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.WindowManager
import android.graphics.Rect
import android.os.Build
import android.view.LayoutInflater
import androidx.annotation.RequiresApi
import kotlin.collections.ArrayList
import kotlin.system.measureTimeMillis

class MyAccessibilityService : AccessibilityService() {

    private val TAG = "OrnaAssist"
    private var mDebugDepth = 0

    var lastEvent: Long = 0
    var getChildCalls = 0
    var state: MainState? = null
    private val handler = Handler(Looper.getMainLooper())

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.source == null) {
            return
        }

        mDebugDepth = 0

        val values = ArrayList<ScreenData>()
        val mNodeInfo: AccessibilityNodeInfo? = event.source

        if (event.source != null) {
            getChildCalls = 0
            val duration = measureTimeMillis {
                parseScreen(mNodeInfo, values, 0, 0)
            }
            Log.d(TAG, "parseScreen completed in $duration ms with ${values.size} items")
        }

        if (values.isEmpty()) {
            // If initial parsing doesn't yield any results, re-parse after a delay
            handler.postDelayed({
                val delayedValues = ArrayList<ScreenData>()
                if (mNodeInfo != null) {
                    getChildCalls = 0
                    val delayedDuration = measureTimeMillis {
                        parseScreen(mNodeInfo, delayedValues, 0, 0)
                    }
                    Log.d(TAG, "Delayed parseScreen completed in $delayedDuration ms with ${delayedValues.size} items")
                    state?.processData(event.packageName.toString(), delayedValues)
                }
            }, 500) // 500 ms delay, adjust as necessary
        } else {
            state?.processData(event.packageName.toString(), values)
        }

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
        if (depth > 100) { // Reduce max depth to limit recursion depth
            Log.d(TAG, "Max depth reached")
            return true
        }

        if (mNodeInfo.text != null) {
            when (mNodeInfo.text.toString()) {
                "DROP", "New", "SEND TO KEEP", "Map" -> done = true
            }
            val rect = Rect()
            mNodeInfo.getBoundsInScreen(rect)
            Log.d(TAG, "Text: ${mNodeInfo.text}, Bounds: $rect")
            data?.add(ScreenData(mNodeInfo.text.toString(), rect, time, mDebugDepth, mNodeInfo))
        }

        if (!done) {
            val count = mNodeInfo.childCount
            for (i in 0 until count) {
                getChildCalls++
                var child: AccessibilityNodeInfo? = null
                val thistime = measureTimeMillis { child = mNodeInfo.getChild(i) }
                if (child != null) {
                    mDebugDepth++
                    Log.d(TAG, "Parsing child at depth $depth, index $i")
                    done = parseScreen(child, data, depth + 1, thistime) // Increase depth by 1 instead of depth + i
                    if (done) break
                    mDebugDepth--
                }
            }
        }

        mNodeInfo.recycle()

        return done
    }

    override fun onInterrupt() {}

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = this.serviceInfo
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.notificationTimeout = 100 // Reduce notification timeout for faster updates
        info.packageNames = arrayOf(
            "playorna.com.orna",
            "com.discord",
            "com.avalon.rpg"
        )
        this.serviceInfo = info

        Log.i(TAG, "onServiceConnected called")
    }
}
