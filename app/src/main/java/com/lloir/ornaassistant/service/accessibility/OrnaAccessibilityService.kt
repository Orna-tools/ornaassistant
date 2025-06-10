package com.lloir.ornaassistant.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenData
import com.lloir.ornaassistant.domain.model.ScreenType
import com.lloir.ornaassistant.service.overlay.OverlayManager
import com.lloir.ornaassistant.service.parser.ScreenParserManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.O)
class OrnaAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var overlayManager: OverlayManager

    @Inject
    lateinit var screenParserManager: ScreenParserManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _screenDataFlow = MutableSharedFlow<ParsedScreen>(replay = 0)
    val screenDataFlow = _screenDataFlow.asSharedFlow()

    private var lastProcessTime = 0L
    private val minProcessInterval = 500L // Minimum 500ms between processing events

    private var isServiceReady = false
    private var initializationJob: Job? = null

    companion object {
        private const val TAG = "OrnaAccessibilityService"
        private const val SERVICE_READY_DELAY = 1000L // 1 second

        // Supported packages
        private val SUPPORTED_PACKAGES = setOf(
            "playorna.com.orna",
            "com.discord"
        )
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Accessibility service created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")

        // Configure service info
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
            packageNames = SUPPORTED_PACKAGES.toTypedArray()
        }

        // Set the service reference IMMEDIATELY
        overlayManager.setAccessibilityService(this)

        // Initialize with a shorter delay since we're setting the reference immediately
        initializationJob = serviceScope.launch {
            try {
                Log.d(TAG, "Starting initialization with ${SERVICE_READY_DELAY}ms delay...")
                delay(SERVICE_READY_DELAY)

                // Double-check that the service is still connected
                if (serviceInfo != null) {
                    Log.d(TAG, "Service confirmed connected, initializing overlay manager...")
                    overlayManager.initialize()
                    isServiceReady = true
                    Log.i(TAG, "Accessibility service initialization completed successfully")
                } else {
                    Log.w(TAG, "Service is no longer connected during initialization")
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Initialization cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Error during service initialization", e)
                // Retry initialization once after a delay
                delay(500)
                try {
                    overlayManager.initialize()
                    isServiceReady = true
                    Log.i(TAG, "Accessibility service initialization retry succeeded")
                } catch (retryException: Exception) {
                    Log.e(TAG, "Retry initialization also failed", retryException)
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Don't process events until service is ready
        if (!isServiceReady || event?.source == null) {
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessTime < minProcessInterval) {
            return // Throttle events to prevent overwhelming the system
        }
        lastProcessTime = currentTime

        val packageName = event.packageName?.toString() ?: return

        if (!SUPPORTED_PACKAGES.contains(packageName)) {
            return
        }

        // Process the screen data in a background coroutine with error handling
        serviceScope.launch(Dispatchers.Default) {
            var sourceNode: AccessibilityNodeInfo? = null
            try {
                sourceNode = event.source
                if (sourceNode == null) {
                    Log.w(TAG, "Source node is null, skipping event")
                    return@launch
                }

                val screenData = parseAccessibilityTree(sourceNode)
                if (screenData.isEmpty()) {
                    Log.d(TAG, "No screen data extracted, skipping")
                    return@launch
                }

                val screenType = determineScreenType(screenData)
                val parsedScreen = ParsedScreen(
                    screenType = screenType,
                    data = screenData,
                    timestamp = LocalDateTime.now()
                )

                // Clear assessment data if we're not on an item detail screen
                if (screenType != ScreenType.ITEM_DETAIL) {
                    withContext(Dispatchers.Main) {
                        screenParserManager.clearItemAssessment()
                    }
                }

                // Emit the parsed screen data
                _screenDataFlow.emit(parsedScreen)

                // Update overlays on main thread with additional safety checks
                withContext(Dispatchers.Main) {
                    if (isServiceReady) {
                        overlayManager.handleScreenUpdate(parsedScreen)
                    }
                }

                // Process screen-specific logic
                screenParserManager.processScreen(parsedScreen)

            } catch (e: CancellationException) {
                Log.d(TAG, "Processing cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing accessibility event", e)
            } finally {
                // Always recycle the source node to prevent memory leaks
                try {
                    sourceNode?.recycle()
                } catch (e: Exception) {
                    Log.w(TAG, "Error recycling source node", e)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
        isServiceReady = false
    }

    override fun onDestroy() {
        Log.d(TAG, "Accessibility service destroying...")
        isServiceReady = false

        // Cancel initialization if it's still running
        initializationJob?.cancel()

        // Clean up overlays and clear service reference
        serviceScope.launch {
            try {
                overlayManager.cleanup()
            } catch (e: Exception) {
                Log.e(TAG, "Error during overlay cleanup", e)
            }
        }

        // Cancel the service scope
        serviceScope.cancel()

        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Accessibility service unbound")
        isServiceReady = false
        overlayManager.clearAccessibilityService()
        return super.onUnbind(intent)
    }

    private fun parseAccessibilityTree(rootNode: AccessibilityNodeInfo?): List<ScreenData> {
        if (rootNode == null) return emptyList()

        val screenData = mutableListOf<ScreenData>()
        val visitedNodes = mutableSetOf<AccessibilityNodeInfo>()

        try {
            parseNodeRecursively(rootNode, screenData, visitedNodes, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing accessibility tree", e)
        } finally {
            // Clean up visited nodes to prevent memory leaks
            visitedNodes.forEach { node ->
                try {
                    if (node != rootNode) {
                        node.recycle()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error recycling node", e)
                }
            }
        }

        return screenData
    }

    private fun parseNodeRecursively(
        node: AccessibilityNodeInfo,
        screenData: MutableList<ScreenData>,
        visitedNodes: MutableSet<AccessibilityNodeInfo>,
        depth: Int
    ) {
        if (depth > 50 || visitedNodes.contains(node)) {
            return // Prevent infinite recursion and circular references
        }

        visitedNodes.add(node)

        try {
            // Extract text content
            val text = node.text?.toString()
            if (!text.isNullOrBlank()) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)

                screenData.add(
                    ScreenData(
                        text = text,
                        bounds = bounds,
                        timestamp = System.currentTimeMillis(),
                        depth = depth
                    )
                )
            }

            // Process child nodes
            val childCount = node.childCount
            for (i in 0 until childCount) {
                try {
                    val child = node.getChild(i)
                    if (child != null) {
                        parseNodeRecursively(child, screenData, visitedNodes, depth + 1)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error processing child node at index $i", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error processing node at depth $depth", e)
        }
    }

    private fun determineScreenType(screenData: List<ScreenData>): ScreenType {
        val texts = screenData.map { it.text.lowercase() }

        return when {
            texts.any { it.contains("acquired") } -> ScreenType.ITEM_DETAIL
            texts.any { it.contains("new") && texts.any { it.contains("inventory") } } -> ScreenType.INVENTORY
            texts.any { it.contains("notifications") } -> ScreenType.NOTIFICATIONS
            texts.any { it.contains("this wayvessel is active") } -> ScreenType.WAYVESSEL
            texts.any { it.contains("special dungeon") || it.contains("world dungeon") } -> ScreenType.DUNGEON_ENTRY
            texts.any { it.contains("battle a series of opponents") } -> ScreenType.DUNGEON_ENTRY
            texts.any { it.contains("codex") && it.contains("skill") } -> ScreenType.BATTLE
            else -> ScreenType.UNKNOWN
        }
    }
}