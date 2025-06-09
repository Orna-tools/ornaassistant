package com.lloir.ornaassistant.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.lloir.ornaassistant.MainState
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.ScreenData
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class MyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "OrnaAccessibilityService"
        private const val MAX_DEPTH = 50 // Reduced from 250 to prevent deep recursion
        private const val MIN_PROCESSING_INTERVAL = 500L // Minimum time between processing
        private const val IDLE_FALLBACK_INTERVAL = 1000L
        private const val MAX_NODES_PER_SCAN = 200 // Limit total nodes processed
    }

    private var mainStateRef: WeakReference<MainState>? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val lastEventTime = AtomicLong(System.currentTimeMillis())
    private val isProcessing = AtomicBoolean(false)
    private val lastProcessTime = AtomicLong(0L)

    private val debugEnabled: Boolean
        get() = getSharedPreferences("orna_settings", Context.MODE_PRIVATE)
            .getBoolean("debug_logs", false)

    override fun onCreate() {
        super.onCreate()
        initializeMainState()
    }

    private fun initializeMainState() {
        try {
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            val mainState = MainState(
                windowManager,
                applicationContext,
                inflater.inflate(R.layout.notification_layout, null),
                inflater.inflate(R.layout.wayvessel_overlay, null),
                inflater.inflate(R.layout.assess_layout, null),
                inflater.inflate(R.layout.kg_layout, null),
                this
            )

            mainStateRef = WeakReference(mainState)
            Log.d(TAG, "MainState initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MainState", e)
            Toast.makeText(this, "Orna Assistant failed to start", Toast.LENGTH_LONG).show()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        startIdleFallbackMonitoring()
        Log.d(TAG, "Accessibility service connected")
    }

    private fun startIdleFallbackMonitoring() {
        serviceScope.launch {
            while (isActive) {
                delay(IDLE_FALLBACK_INTERVAL)

                val now = System.currentTimeMillis()
                if (now - lastEventTime.get() > 750 && !isProcessing.get()) {
                    performIdleFallbackScan()
                }
            }
        }
    }

    private suspend fun performIdleFallbackScan() {
        try {
            val root = rootInActiveWindow ?: return
            val screenDataList = arrayListOf<ScreenData>()

            withContext(Dispatchers.Default) {
                parseScreenOptimized(root, screenDataList, 0)
            }

            // Only process if we found item-related content
            if (screenDataList.any { it.name.contains("ACQUIRED", ignoreCase = true) }) {
                if (debugEnabled) {
                    Log.d(TAG, "Idle fallback triggered item screen parse with ${screenDataList.size} elements")
                }

                mainStateRef?.get()?.processData("com.orna", screenDataList)
                lastEventTime.set(System.currentTimeMillis())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in idle fallback scan", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let { accessibilityEvent ->
            val currentTime = System.currentTimeMillis()
            lastEventTime.set(currentTime)

            // Rate limiting
            if (currentTime - lastProcessTime.get() < MIN_PROCESSING_INTERVAL) {
                return
            }

            when (accessibilityEvent.eventType) {
                AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_VIEW_FOCUSED,
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    processAccessibilityEvent(accessibilityEvent)
                }
            }
        }
    }

    private fun processAccessibilityEvent(event: AccessibilityEvent) {
        if (!isProcessing.compareAndSet(false, true)) {
            return // Already processing
        }

        serviceScope.launch {
            try {
                delay(500) // Allow UI to settle

                val sourceNode = event.source
                if (sourceNode != null) {
                    val screenDataList = arrayListOf<ScreenData>()

                    withContext(Dispatchers.Default) {
                        parseScreenOptimized(sourceNode, screenDataList, 0)
                    }

                    if (debugEnabled && screenDataList.isNotEmpty()) {
                        Log.d(TAG, "Event type: ${event.eventType}, found ${screenDataList.size} elements")
                        if (screenDataList.size <= 10) { // Only log details for small lists
                            screenDataList.forEach { data ->
                                Log.d(TAG, "Text: ${data.name} | Rect: ${data.rect}")
                            }
                        }
                    }

                    val packageName = event.packageName?.toString() ?: "unknown"
                    mainStateRef?.get()?.processData(packageName, screenDataList)

                    // Handle item screen refresh
                    if (screenDataList.any { it.name.contains("ACQUIRED", ignoreCase = true) }) {
                        handleItemScreenRefresh(packageName)
                    }

                    lastProcessTime.set(System.currentTimeMillis())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing accessibility event", e)
            } finally {
                isProcessing.set(false)
            }
        }
    }

    private suspend fun handleItemScreenRefresh(packageName: String) {
        try {
            delay(1000) // Wait for potential screen updates

            val refreshedRoot = rootInActiveWindow
            if (refreshedRoot != null) {
                val retryList = arrayListOf<ScreenData>()

                withContext(Dispatchers.Default) {
                    parseScreenOptimized(refreshedRoot, retryList, 0)
                }

                if (debugEnabled) {
                    Log.d(TAG, "Refreshed root scan found ${retryList.size} elements")
                    if (retryList.size <= 10) {
                        retryList.forEach { data ->
                            Log.d(TAG, "Retry Text: ${data.name} | Rect: ${data.rect}")
                        }
                    }
                }

                mainStateRef?.get()?.processData(packageName, retryList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in item screen refresh", e)
        }
    }

    private fun parseScreenOptimized(
        node: AccessibilityNodeInfo?,
        dataList: ArrayList<ScreenData>,
        depth: Int
    ): Boolean {
        // Safety checks
        if (node == null ||
            depth > MAX_DEPTH ||
            dataList.size >= MAX_NODES_PER_SCAN ||
            !serviceScope.isActive) {
            return false
        }

        var isProcessed = false
        val processingTime = System.currentTimeMillis()

        try {
            // Process current node text
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let { text ->
                // Check for stop conditions
                if (text in setOf("DROP", "New", "SEND TO KEEP")) {
                    isProcessed = true
                }

                // Create screen data with proper bounds checking
                val rect = Rect()
                try {
                    node.getBoundsInScreen(rect)
                    // Validate rect bounds
                    if (rect.width() > 0 && rect.height() > 0) {
                        dataList.add(ScreenData(text, rect, processingTime, depth, node))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error getting bounds for node", e)
                }
            }

            // Process children if not already processed and within limits
            if (!isProcessed && dataList.size < MAX_NODES_PER_SCAN) {
                val childCount = minOf(node.childCount, 20) // Limit children processed

                for (i in 0 until childCount) {
                    if (dataList.size >= MAX_NODES_PER_SCAN) break

                    try {
                        val child = node.getChild(i)
                        if (child != null) {
                            val childProcessed = parseScreenOptimized(child, dataList, depth + 1)
                            if (childProcessed) {
                                isProcessed = true
                                break
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error processing child node at index $i", e)
                        continue
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing node at depth $depth", e)
        } finally {
            // Properly recycle node to prevent memory leaks
            try {
                if (depth > 0) { // Don't recycle root node
                    node.recycle()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error recycling node", e)
            }
        }

        return isProcessed
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
        cleanup()
        Toast.makeText(this, "Orna Assistant Screen Reader is off", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
        cleanup()
    }

    private fun cleanup() {
        try {
            // Cancel all coroutines
            serviceScope.cancel()

            // Clean up main state
            mainStateRef?.get()?.cleanup()
            mainStateRef = null

            Log.d(TAG, "Cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}