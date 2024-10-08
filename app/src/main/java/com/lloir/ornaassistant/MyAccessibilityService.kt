package com.lloir.ornaassistant

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class MyAccessibilityService : AccessibilityService() {

    private var state: MainState? = null

    override fun onCreate() {
        super.onCreate()
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        state = MainState(
            getSystemService(WINDOW_SERVICE) as WindowManager, applicationContext,
            inflater.inflate(R.layout.notification_layout, null),
            inflater.inflate(R.layout.wayvessel_overlay, null),
            inflater.inflate(R.layout.assess_layout, null),
            inflater.inflate(R.layout.kg_layout, null),
            this
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                GlobalScope.launch {
                    var attempts = 0
                    while (attempts < 5) {
                        delay(500L * attempts)  // Increase delay with each attempt

                        if (event == null || event.source == null) return@launch

                        val values = ArrayList<ScreenData>()
                        val rootNode: AccessibilityNodeInfo? = event.source

                        if (parseScreen(rootNode, values, 0, 0)) {
                            state?.processData(event.packageName.toString(), values)
                            break  // Exit loop if parsing is successful
                        }
                        attempts++
                    }
                }
            }
        }
    }

    private fun parseScreen(
        rootNode: AccessibilityNodeInfo?,
        data: ArrayList<ScreenData>,
        depth: Int,
        time: Long
    ): Boolean {
        var done = false
        if (rootNode == null) return done
        if (depth > 250) return true

        if (rootNode.text != null) {
            when (rootNode.text.toString()) {
                "DROP", "New", "SEND TO KEEP", "Map" -> done = true
            }

            // Get screen bounds dynamically
            val rect = Rect()
            rootNode.getBoundsInScreen(rect)

            // Adjust the rectangle based on screen size (optional scaling)
            val scaledRect = scaleRect(rect)

            data.add(ScreenData(rootNode.text.toString(), scaledRect, time, 0, rootNode))
        }

        if (!done) {
            val count = rootNode.childCount
            for (i in 0 until count) {
                val child = rootNode.getChild(i)
                if (child != null) {
                    done = parseScreen(child, data, depth + 1, measureTimeMillis { })
                }
                if (done) break
            }
        }
        rootNode.recycle()
        return done
    }

    private fun scaleRect(rect: Rect): Rect {
        // Example of scaling based on screen width/height
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val left = rect.left * screenWidth / 1080 // assuming 1080 as a base width
        val top = rect.top * screenHeight / 1920  // assuming 1920 as a base height
        val right = rect.right * screenWidth / 1080
        val bottom = rect.bottom * screenHeight / 1920

        return Rect(left, top, right, bottom)
    }

    override fun onInterrupt() {
        state?.cleanup()
        Toast.makeText(this, "Orna Assistant Screen reader is off", Toast.LENGTH_LONG).show()
    }
}
