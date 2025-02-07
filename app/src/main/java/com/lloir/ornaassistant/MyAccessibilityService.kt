package com.lloir.ornaassistant

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MyAccessibilityService : AccessibilityService() {

    private var state: MainState? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        state = MainState(
            windowManager,
            applicationContext,
            inflater.inflate(R.layout.notification_layout, null),
            inflater.inflate(R.layout.wayvessel_overlay, null),
            inflater.inflate(R.layout.assess_layout, null),
            inflater.inflate(R.layout.kg_layout, null),
            this
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            when (it.eventType) {
                AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_VIEW_FOCUSED,
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    serviceScope.launch {
                        delay(500)
                        it.source?.let { rootNode ->
                            val screenDataList = arrayListOf<ScreenData>() // Explicitly use ArrayList
                            parseScreen(rootNode, screenDataList, 0)
                            state?.processData(it.packageName.toString(), screenDataList)
                        }
                    }
                }
                else -> {
                    // No action needed for other event types
                }
            }
        }
    }

    private fun parseScreen(
        node: AccessibilityNodeInfo?,
        dataList: ArrayList<ScreenData>,
        depth: Int
    ): Boolean {
        if (node == null || depth > 250) return false
        var isProcessed = false

        node.text?.toString()?.let { text ->
            if (text in setOf("DROP", "New", "SEND TO KEEP")) isProcessed = true

            val rect = Rect()
            node.getBoundsInScreen(rect)
            val processingTime = System.currentTimeMillis() // ✅ Fixed: Use system time instead
            dataList.add(ScreenData(text, rect, processingTime, 0, node))
        }

        if (!isProcessed) {
            val count = node.childCount
            for (i in 0 until count) {
                val child = node.getChild(i)
                if (child != null) {
                    val childProcessed = parseScreen(child, dataList, depth + 1)
                    if (childProcessed) {
                        isProcessed = true
                        break // ✅ No longer inside an inline lambda, so it's valid now!
                    }
                }
            }
        }
        node.recycle()
        return isProcessed
    }

    override fun onInterrupt() {
        state?.cleanup()
        Toast.makeText(this, "Orna Assistant Screen Reader is off", Toast.LENGTH_LONG).show()
    }
}
