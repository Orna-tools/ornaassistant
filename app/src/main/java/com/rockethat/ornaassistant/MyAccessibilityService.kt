package com.rockethat.ornaassistant

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
        when(p0?.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                GlobalScope.launch {
                    delay(500) // delay before parsing the screen

                    if (p0 == null || p0.source == null) return@launch

                    val values = ArrayList<ScreenData>()
                    val mNodeInfo: AccessibilityNodeInfo? = p0.source

                    mDebugDepth = 0
                    getChildCalls = 0
                    parseScreen(mNodeInfo, values, 0, 0)
                    state?.processData(p0.packageName.toString(), values)
                    lastEvent = System.currentTimeMillis()
                }
            }
        }
    }

    // Parse the screen
    private fun parseScreen(
        mNodeInfo: AccessibilityNodeInfo?,
        data: ArrayList<ScreenData>,
        depth: Int,
        time: Long
    ): Boolean {
        var done = false
        if (mNodeInfo == null) return done
        if (depth > 250) return true

        if (mNodeInfo.text != null) {
            when (mNodeInfo.text.toString()) {
                "DROP" -> done = true
                "New" -> done = true // Inventory
                "SEND TO KEEP" -> done = true // Inventory
                "Map" -> done = true
            }
            val rect = Rect()
            mNodeInfo.getBoundsInScreen(rect)
            data.add(ScreenData(mNodeInfo.text.toString(), rect, time, mDebugDepth, mNodeInfo))
        }

        if (!done) {
            val count = mNodeInfo.childCount
            for (i in 0 until count) {
                getChildCalls++
                var child: AccessibilityNodeInfo?
                val thistime = measureTimeMillis { child = mNodeInfo.getChild(i) }
                if (child != null) {
                    mDebugDepth++
                    done = parseScreen(child, data, depth + 1, thistime)
                    mDebugDepth--
                }
                if (done) {
                    break
                }
            }
        }
        mNodeInfo.recycle()
        return done
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onInterrupt() {
        // Clean any held resources or stop ongoing tasks
        state?.cleanup()

        // Optionally notify the user. Remember that your service might not have UI!
        Toast.makeText(this, "Orna Assistant Screen reader is off", Toast.LENGTH_LONG).show()
    }
}