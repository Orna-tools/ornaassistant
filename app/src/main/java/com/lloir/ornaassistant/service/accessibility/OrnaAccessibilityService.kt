package com.lloir.ornaassistant.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.domain.model.DungeonState
import com.lloir.ornaassistant.domain.model.DungeonVisit
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenData
import com.lloir.ornaassistant.domain.model.ScreenType
import com.lloir.ornaassistant.domain.model.WayvesselSession
import com.lloir.ornaassistant.service.overlay.OverlayManager
import com.lloir.ornaassistant.service.parser.impl.DungeonScreenParser
import com.lloir.ornaassistant.service.parser.ScreenParserManager
import com.lloir.ornaassistant.domain.repository.DungeonRepository
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import com.lloir.ornaassistant.domain.repository.WayvesselRepository
import com.lloir.ornaassistant.OrnaAssistantApplication
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

enum class DeviceType {
    ASUS, SAMSUNG, XIAOMI, OPPO, VIVO, ONEPLUS, HUAWEI, OTHER
}

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.O)
class OrnaAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var overlayManager: OverlayManager

    @Inject
    lateinit var screenParserManager: ScreenParserManager

    @Inject
    lateinit var dungeonScreenParser: DungeonScreenParser

    @Inject
    lateinit var dungeonRepository: DungeonRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var wayvesselRepository: WayvesselRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _screenDataFlow = MutableSharedFlow<ParsedScreen>(replay = 0)
    val screenDataFlow = _screenDataFlow.asSharedFlow()

    private var lastProcessTime = 0L
    private val minProcessInterval: Long
        get() = when (getDeviceType()) {
            DeviceType.ASUS -> 1000L
            DeviceType.SAMSUNG -> 750L
            DeviceType.XIAOMI -> 750L
            DeviceType.OPPO -> 800L
            DeviceType.VIVO -> 800L
            else -> 500L
        }

    private var isServiceReady = false
    private var initializationJob: Job? = null
    private var initRetryCount = 0

    private var currentDungeonState: DungeonState? = null
    private var currentDungeonVisit: DungeonVisit? = null
    private var onHoldVisits = mutableMapOf<String, DungeonVisit>()
    private var currentWayvesselSession: WayvesselSession? = null

    // Device-specific diagnostic mode
    private val diagnosticMode: Boolean
        get() = (getDeviceType() == DeviceType.ASUS || getDeviceType() == DeviceType.SAMSUNG) &&
                getSharedPreferences("orna_debug", MODE_PRIVATE).getBoolean("asus_diagnostic", false)
    private val diagnosticData = mutableListOf<String>()

    companion object {
        private const val TAG = "OrnaAccessibilityService"
        private const val SERVICE_READY_DELAY = 1000L
        private const val DEVICE_INIT_DELAY = 3000L
        private const val DEVICE_RETRY_DELAY = 1000L
        private const val DEVICE_MAX_RETRIES = 5
        private const val NOTIFICATION_ID = 1001

        // Supported packages
        private val SUPPORTED_PACKAGES = setOf(
            "playorna.com.orna",
            "com.discord"
        )
    }

    private fun getDeviceType(): DeviceType {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("asus") -> DeviceType.ASUS
            manufacturer.contains("samsung") -> DeviceType.SAMSUNG
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> DeviceType.XIAOMI
            manufacturer.contains("oppo") -> DeviceType.OPPO
            manufacturer.contains("vivo") -> DeviceType.VIVO
            manufacturer.contains("oneplus") -> DeviceType.ONEPLUS
            manufacturer.contains("huawei") -> DeviceType.HUAWEI
            else -> DeviceType.OTHER
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Accessibility service created on ${Build.MANUFACTURER} ${Build.MODEL} (${getDeviceType()})")

        // Start foreground service for problematic manufacturers
        when (getDeviceType()) {
            DeviceType.ASUS, DeviceType.SAMSUNG, DeviceType.XIAOMI,
            DeviceType.OPPO, DeviceType.VIVO -> {
                try {
                    startForegroundServiceForDevice()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start foreground service", e)
                }
            }
            else -> {}
        }

        observeSettings()
    }

    private fun startForegroundServiceForDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val channel = NotificationChannel(
                OrnaAssistantApplication.SERVICE_CHANNEL_ID,
                "Orna Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the screen reading service active"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null) // Important for Samsung
            }
            notificationManager.createNotificationChannel(channel)
        }

        val deviceName = when (getDeviceType()) {
            DeviceType.SAMSUNG -> "Samsung"
            DeviceType.ASUS -> "ASUS"
            DeviceType.XIAOMI -> "Xiaomi"
            DeviceType.OPPO -> "OPPO"
            DeviceType.VIVO -> "Vivo"
            else -> "your"
        }

        val notification = NotificationCompat.Builder(this, OrnaAssistantApplication.SERVICE_CHANNEL_ID)
            .setContentTitle("Orna Assistant")
            .setContentText("Optimized for $deviceName device")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true) // Important for Samsung
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        try {
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Started foreground service for $deviceName device")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground", e)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")

        try {
            // Configure service info
            configureServiceInfo()

            // Set the service reference
            overlayManager.setAccessibilityService(this)

            // Initialize with device-specific handling
            when (getDeviceType()) {
                DeviceType.ASUS, DeviceType.SAMSUNG, DeviceType.XIAOMI -> {
                    serviceScope.launch {
                        retryServiceInitialization()
                    }
                }
                else -> {
                    initializationJob = serviceScope.launch {
                        try {
                            Log.d(TAG, "Starting initialization with ${SERVICE_READY_DELAY}ms delay...")
                            delay(SERVICE_READY_DELAY)

                            if (serviceInfo != null) {
                                Log.d(TAG, "Service confirmed connected, initializing overlay manager...")
                                overlayManager.initialize()
                                isServiceReady = true
                                Log.i(TAG, "Accessibility service initialization completed successfully")
                            } else {
                                Log.w(TAG, "Service is no longer connected during initialization")
                            }
                            
                            // Clear any bad cached assessments on service start
                            screenParserManager.clearItemAssessment()
                            (screenParserManager as? ScreenParserManager)?.let {
                                // Access the item parser and clear its cache
                                try {
                                    val itemParserField = it.javaClass.getDeclaredField("itemParser")
                                    itemParserField.isAccessible = true
                                    val itemParser = itemParserField.get(it) as? ItemScreenParser
                                    itemParser?.clearAssessmentCache()
                                    Log.d(TAG, "Cleared item assessment cache on startup")
                                } catch (e: Exception) {
                                    Log.w(TAG, "Could not clear assessment cache", e)
                                }
                            }
                        } catch (e: CancellationException) {
                            Log.d(TAG, "Initialization cancelled")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during service initialization", e)
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onServiceConnected", e)
            // Try to recover for problematic devices
            if (getDeviceType() in listOf(DeviceType.ASUS, DeviceType.SAMSUNG, DeviceType.XIAOMI) &&
                initRetryCount < DEVICE_MAX_RETRIES) {
                serviceScope.launch {
                    delay(DEVICE_RETRY_DELAY)
                    onServiceConnected()
                }
                initRetryCount++
            }
        }
    }

    private fun configureServiceInfo() {
        when (getDeviceType()) {
            DeviceType.ASUS -> configureAsusService()
            DeviceType.SAMSUNG -> configureSamsungService()
            DeviceType.XIAOMI -> configureXiaomiService()
            else -> configureDefaultService()
        }
    }

    private fun configureAsusService() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_ANNOUNCEMENT

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS

            notificationTimeout = 200
            packageNames = SUPPORTED_PACKAGES.toTypedArray()
        }
        serviceInfo = info
        Log.d(TAG, "Configured service for ASUS device")
    }

    private fun configureSamsungService() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS

            notificationTimeout = 100
            packageNames = SUPPORTED_PACKAGES.toTypedArray()
        }
        serviceInfo = info
        Log.d(TAG, "Configured service for Samsung device")
    }

    private fun configureXiaomiService() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS

            notificationTimeout = 150
            packageNames = SUPPORTED_PACKAGES.toTypedArray()
        }
        serviceInfo = info
        Log.d(TAG, "Configured service for Xiaomi device")
    }

    private fun configureDefaultService() {
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
            packageNames = SUPPORTED_PACKAGES.toTypedArray()
        }
    }

    private suspend fun retryServiceInitialization() {
        var retries = 0
        var initialized = false

        while (!initialized && retries < DEVICE_MAX_RETRIES) {
            try {
                Log.d(TAG, "${getDeviceType()} initialization attempt ${retries + 1}")

                delay(if (retries == 0) DEVICE_INIT_DELAY else DEVICE_RETRY_DELAY)

                val testNode = try {
                    rootInActiveWindow
                } catch (e: Exception) {
                    null
                }

                if (testNode != null) {
                    testNode.recycle()
                    Log.d(TAG, "Service connection verified")

                    overlayManager.initialize()
                    isServiceReady = true
                    initialized = true
                    Log.i(TAG, "${getDeviceType()} device initialization successful")
                } else {
                    Log.w(TAG, "Service not fully connected yet")
                }

            } catch (e: Exception) {
                Log.e(TAG, "${getDeviceType()} initialization attempt failed", e)
            }

            retries++
        }

        if (!initialized) {
            Log.e(TAG, "Failed to initialize after $retries attempts")
            showErrorNotification("Service initialization failed. Please restart the app.")
        }
    }

    private fun showErrorNotification(message: String) {
        val notification = NotificationCompat.Builder(this, OrnaAssistantApplication.SERVICE_CHANNEL_ID)
            .setContentTitle("Orna Assistant Error")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun observeSettings() {
        serviceScope.launch {
            settingsRepository.getSettingsFlow().collect { settings ->
                if (settings.showSessionOverlay) {
                    if (currentWayvesselSession != null || currentDungeonVisit != null) {
                        overlayManager.showSessionOverlay(currentWayvesselSession, currentDungeonVisit)
                    }
                } else {
                    overlayManager.hideSessionOverlay()
                }

                if (!settings.showInvitesOverlay) {
                    overlayManager.hideInvitesOverlay()
                }

                if (!settings.showAssessOverlay) {
                    overlayManager.hideAssessmentOverlay()
                }

                overlayManager.setOverlayTransparency(settings.overlayTransparency)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Don't process events until service is ready
        if (!isServiceReady) {
            if (getDeviceType() in listOf(DeviceType.ASUS, DeviceType.SAMSUNG) && event != null) {
                serviceScope.launch {
                    retryServiceInitialization()
                }
            }
            return
        }

        // Diagnostic logging for problematic devices
        if (diagnosticMode && event != null) {
            runScreenReadingDiagnostic(event)
        }

        // Throttle events
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessTime < minProcessInterval) {
            return
        }
        lastProcessTime = currentTime

        val packageName = event?.packageName?.toString() ?: return
        if (!SUPPORTED_PACKAGES.contains(packageName)) {
            return
        }

        // Device-specific validation
        if (getDeviceType() in listOf(DeviceType.ASUS, DeviceType.SAMSUNG) && event.source == null) {
            serviceScope.launch {
                delay(50)
                event.source?.let { delayedSource ->
                    processEventWithSource(event, delayedSource)
                }
            }
            return
        }

        // Process the screen data
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

                // Debug logging
                if (screenData.size > 0) {
                    Log.d(TAG, "Screen data sample (${screenData.size} items):")
                    screenData.take(10).forEach { data ->
                        Log.d(TAG, "  - '${data.text}'")
                    }
                }

                val screenType = determineScreenType(screenData)
                val parsedScreen = ParsedScreen(
                    screenType = screenType,
                    data = screenData,
                    timestamp = LocalDateTime.now()
                )

                // Clear assessment data if needed
                if (screenType != ScreenType.ITEM_DETAIL || screenType == ScreenType.INVENTORY) {
                    withContext(Dispatchers.Main) {
                        screenParserManager.clearItemAssessment()
                    }
                }

                _screenDataFlow.emit(parsedScreen)

                // Update overlays
                withContext(Dispatchers.Main) {
                    if (isServiceReady) {
                        overlayManager.handleScreenUpdate(parsedScreen)
                    }
                }

                // Check for dungeon screens
                val isDungeonScreen = dungeonScreenParser.canParse(screenData)
                Log.d(TAG, "Is dungeon screen: $isDungeonScreen, detected type: $screenType")
                
                // Log some sample data for debugging
                if (isDungeonScreen) {
                    Log.d(TAG, "Dungeon screen detected with ${screenData.size} elements")
                    screenData.filter { it.text.contains("Floor", ignoreCase = true) || 
                                       it.text.contains("Dungeon") }.forEach {
                        Log.d(TAG, "  Relevant text: '${it.text}'")
                    }
                }

                if (isDungeonScreen) {
                    try {
                        val newState = dungeonScreenParser.parseState(screenData, currentDungeonState)
                        Log.d(TAG, "Current dungeon state: $currentDungeonState")
                        Log.d(TAG, "New dungeon state: $newState")

                        handleDungeonStateChange(newState, screenData)

                        // Update currentDungeonState AFTER handling changes
                        // This ensures victoryScreenHandledForFloor is properly tracked
                        currentDungeonState = newState

                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing dungeon state", e)
                    }
                } else if (currentDungeonState?.hasEntered == true) {
                    // We were in a dungeon but now we're not - reset state
                    Log.d(TAG, "No longer in dungeon, resetting state")
                    currentDungeonVisit?.let { visit ->
                        Log.d(TAG, "Final dungeon stats: ${visit.name} - " +
                                "Orns: ${visit.orns}, Gold: ${visit.gold}, " +
                                "Exp: ${visit.experience}, Floor: ${visit.floor}")
                    }
                    currentDungeonState = DungeonState()
                    currentDungeonVisit = null
                    updateOverlay()
                }

                // Check for wayvessel activation
                if (screenData.any { it.text.contains("This wayvessel is active", ignoreCase = true) }) {
                    val wayvesselName = screenData.find { it.text.contains("'s Wayvessel") }
                        ?.text?.replace("'s Wayvessel", "")
                    if (wayvesselName != null && currentWayvesselSession?.name != wayvesselName) {
                        handleWayvesselStart(wayvesselName)
                    }
                }

                // Process general screen parsing
                screenParserManager.processScreen(parsedScreen)

            } catch (e: CancellationException) {
                Log.d(TAG, "Processing cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing accessibility event", e)
            } finally {
                try {
                    sourceNode?.recycle()
                } catch (e: Exception) {
                    Log.w(TAG, "Error recycling source node", e)
                }
            }
        }
    }

    private fun processEventWithSource(event: AccessibilityEvent, source: AccessibilityNodeInfo) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessTime < minProcessInterval) {
            source.recycle()
            return
        }
        lastProcessTime = currentTime

        serviceScope.launch(Dispatchers.Default) {
            try {
                val screenData = parseAccessibilityTree(source)
                if (screenData.isNotEmpty()) {
                    val screenType = determineScreenType(screenData)
                    val parsedScreen = ParsedScreen(
                        screenType = screenType,
                        data = screenData,
                        timestamp = LocalDateTime.now()
                    )

                    _screenDataFlow.emit(parsedScreen)

                    withContext(Dispatchers.Main) {
                        if (isServiceReady) {
                            overlayManager.handleScreenUpdate(parsedScreen)
                        }
                    }

                    screenParserManager.processScreen(parsedScreen)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing delayed event", e)
            } finally {
                source.recycle()
            }
        }
    }

    private fun parseAccessibilityTree(rootNode: AccessibilityNodeInfo?): List<ScreenData> {
        if (rootNode == null) return emptyList()

        val screenData = mutableListOf<ScreenData>()
        val visitedNodes = mutableSetOf<AccessibilityNodeInfo>()

        try {
            // Device-specific handling
            when (getDeviceType()) {
                DeviceType.ASUS, DeviceType.SAMSUNG, DeviceType.XIAOMI -> {
                    // Force refresh for problematic devices
                    try {
                        rootNode.refresh()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to refresh root node", e)
                    }

                    // Try alternative methods if needed
                    val windows = windows
                    windows?.forEach { window ->
                        window.root?.let { root ->
                            if (root.packageName == "playorna.com.orna") {
                                parseNodeRecursively(root, screenData, visitedNodes, 0)
                                root.recycle()
                            }
                        }
                    }

                    if (screenData.isEmpty()) {
                        parseNodeRecursively(rootNode, screenData, visitedNodes, 0)
                    }
                }
                else -> {
                    parseNodeRecursively(rootNode, screenData, visitedNodes, 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing accessibility tree", e)
        } finally {
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
            return
        }

        visitedNodes.add(node)

        try {
            // Extract text content
            val text = extractTextFromNode(node)
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
                        // Device-specific child handling
                        if (getDeviceType() in listOf(DeviceType.ASUS, DeviceType.SAMSUNG)) {
                            try {
                                child.refresh()
                            } catch (e: Exception) {
                                // Ignore refresh errors
                            }
                        }
                        parseNodeRecursively(child, screenData, visitedNodes, depth + 1)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error processing child node at index $i, depth $depth", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error processing node at depth $depth", e)
        }
    }

    private fun extractTextFromNode(node: AccessibilityNodeInfo): String? {
        return when (getDeviceType()) {
            DeviceType.SAMSUNG -> extractTextFromNodeSamsung(node)
            DeviceType.ASUS -> extractTextFromNodeAsus(node)
            else -> extractTextFromNodeDefault(node)
        }
    }

    private fun extractTextFromNodeDefault(node: AccessibilityNodeInfo): String? {
        try {
            node.text?.toString()?.let { text ->
                if (text.isNotBlank()) return text
            }

            node.contentDescription?.toString()?.let { desc ->
                if (desc.isNotBlank()) return desc
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting text from node", e)
        }
        return null
    }

    private fun extractTextFromNodeAsus(node: AccessibilityNodeInfo): String? {
        try {
            // Standard text extraction
            node.text?.toString()?.let { text ->
                if (text.isNotBlank()) return text
            }

            // ASUS often uses contentDescription
            node.contentDescription?.toString()?.let { desc ->
                if (desc.isNotBlank()) return desc
            }

            // ASUS-specific extras check
            try {
                val extras = node.extras
                extras?.keySet()?.forEach { key ->
                    val value = extras.get(key)?.toString()
                    if (!value.isNullOrBlank() && value.length > 2) {
                        Log.d(TAG, "Found text in extras[$key]: $value")
                    }
                }
            } catch (e: Exception) {
                // Ignore extras errors
            }

            // Check ViewIdResourceName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                node.viewIdResourceName?.let { viewId ->
                    Log.v(TAG, "Node has viewId: $viewId")
                }
            }

            // Check if it's an EditText with hint text
            if (node.className == "android.widget.EditText") {
                node.hintText?.toString()?.let { hint ->
                    if (hint.isNotBlank()) {
                        Log.d(TAG, "Found hint text: $hint")
                    }
                }
            }

            // For TextView nodes, try getting text through child
            if (node.className?.contains("TextView") == true && node.childCount == 1) {
                val child = node.getChild(0)
                child?.text?.toString()?.let { text ->
                    child.recycle()
                    if (text.isNotBlank()) return text
                }
            }

        } catch (e: Exception) {
            Log.w(TAG, "Error extracting text from ASUS node", e)
        }

        return null
    }

    private fun extractTextFromNodeSamsung(node: AccessibilityNodeInfo): String? {
        try {
            // Standard text first
            node.text?.toString()?.let { text ->
                if (text.isNotBlank()) return text
            }

            // Samsung often uses contentDescription
            node.contentDescription?.toString()?.let { desc ->
                if (desc.isNotBlank()) return desc
            }

            // Samsung's One UI sometimes uses tooltipText
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                node.tooltipText?.toString()?.let { tooltip ->
                    if (tooltip.isNotBlank()) {
                        Log.d(TAG, "Found text in tooltip: $tooltip")
                        return tooltip
                    }
                }
            }

            // Check labelFor/labeledBy relationships (Samsung uses these)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                node.labelFor?.let { labelNode ->
                    val text = extractTextFromNode(labelNode)
                    labelNode.recycle()
                    if (!text.isNullOrBlank()) return text
                }
            }

        } catch (e: Exception) {
            Log.w(TAG, "Error extracting text from Samsung node", e)
        }

        return null
    }

    // Diagnostic methods
    private fun runScreenReadingDiagnostic(event: AccessibilityEvent) {
        val diagnostic = StringBuilder()

        diagnostic.appendLine("=== ${getDeviceType()} Screen Reading Diagnostic ===")
        diagnostic.appendLine("Time: ${LocalDateTime.now()}")
        diagnostic.appendLine("Event: ${getEventTypeString(event.eventType)} from ${event.packageName}")

        // Test root node access
        val rootTest = try {
            val root = rootInActiveWindow
            if (root != null) {
                diagnostic.appendLine("✓ Root node accessible")
                diagnostic.appendLine("  Package: ${root.packageName}")
                diagnostic.appendLine("  Window ID: ${root.windowId}")
                root.recycle()
                true
            } else {
                diagnostic.appendLine("✗ Root node is NULL")
                false
            }
        } catch (e: Exception) {
            diagnostic.appendLine("✗ Error getting root: ${e.message}")
            false
        }

        // Check windows
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val windows = windows
            diagnostic.appendLine("Windows available: ${windows?.size ?: 0}")
            windows?.forEach { window ->
                diagnostic.appendLine("  Window: ${window.title}, Type: ${window.type}")
            }
        }

        // Try event source
        if (!rootTest && event.source != null) {
            diagnostic.appendLine("Trying event source node...")
            try {
                val source = event.source
                diagnostic.appendLine("✓ Event source accessible")
                diagnostic.appendLine("  Class: ${source?.className}")
                diagnostic.appendLine("  Text: ${source?.text}")
                source?.recycle()
            } catch (e: Exception) {
                diagnostic.appendLine("✗ Error with event source: ${e.message}")
            }
        }

        Log.d(TAG, diagnostic.toString())
        diagnosticData.add(diagnostic.toString())

        // Save periodically
        if (diagnosticData.size > 100) {
            saveDiagnosticToFile()
        }
    }

    private fun saveDiagnosticToFile() {
        try {
            val file = File(
                getExternalFilesDir(null),
                "${getDeviceType().name.lowercase()}_diagnostic_${System.currentTimeMillis()}.txt"
            )
            file.writeText(diagnosticData.joinToString("\n\n"))
            diagnosticData.clear()

            // Notify user
            val notification = NotificationCompat.Builder(this, OrnaAssistantApplication.SERVICE_CHANNEL_ID)
                .setContentTitle("${getDeviceType()} Diagnostic Saved")
                .setContentText("Tap to share diagnostic file")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
                .build()

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(9999, notification)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save diagnostic", e)
        }
    }

    private fun getEventTypeString(eventType: Int): String {
        return when (eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> "VIEW_CLICKED"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> "VIEW_FOCUSED"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "VIEW_TEXT_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> "VIEW_SCROLLED"
            else -> "OTHER ($eventType)"
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
        isServiceReady = false

        // Try to recover for problematic devices
        if (getDeviceType() in listOf(DeviceType.ASUS, DeviceType.SAMSUNG, DeviceType.XIAOMI)) {
            serviceScope.launch {
                delay(1000)
                retryServiceInitialization()
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Accessibility service destroying...")
        isServiceReady = false

        initializationJob?.cancel()

        serviceScope.launch {
            try {
                overlayManager.cleanup()
            } catch (e: Exception) {
                Log.e(TAG, "Error during overlay cleanup", e)
            }
        }

        if (getDeviceType() in listOf(DeviceType.ASUS, DeviceType.SAMSUNG, DeviceType.XIAOMI,
                DeviceType.OPPO, DeviceType.VIVO)) {
            try {
                stopForeground(true)
            } catch (e: Exception) {
                // Ignore
            }
        }

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

    private fun updateOverlay() {
        serviceScope.launch {
            try {
                val settings = settingsRepository.getSettings()
                if (settings.showSessionOverlay) {
                    if (currentDungeonVisit != null || currentWayvesselSession != null) {
                        Log.d(TAG, "Showing session overlay - session: ${currentWayvesselSession?.name}, dungeon: ${currentDungeonVisit?.name}")
                        overlayManager.showSessionOverlay(currentWayvesselSession, currentDungeonVisit)
                    } else {
                        Log.d(TAG, "Hiding session overlay - no active session or dungeon")
                        overlayManager.hideSessionOverlay()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update overlay", e)
            }
        }
    }

    private fun updateDungeonInDatabase() {
        currentDungeonVisit?.let { visit ->
            if (visit.id > 0) {
                serviceScope.launch {
                    try {
                        dungeonRepository.updateVisit(visit)
                        Log.d(TAG, "Updated dungeon visit in database: $visit")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update dungeon in database", e)
                    }
                }
            }
        }
    }

    private suspend fun handleDungeonStateChange(newState: DungeonState, data: List<ScreenData>) {
        Log.d(TAG, "handleDungeonStateChange: newState=$newState, hasEntered=${newState.hasEntered}, currentState=$currentDungeonState")

        // Handle entering new dungeon
        if (newState.dungeonName != currentDungeonState?.dungeonName && newState.dungeonName.isNotEmpty()) {
            Log.d(TAG, "New dungeon detected: ${newState.dungeonName}")
            if (currentDungeonState?.dungeonName?.isNotEmpty() == true) {
                currentDungeonVisit?.let { visit ->
                    Log.d(TAG, "Putting ${currentDungeonState!!.dungeonName} on hold")
                    onHoldVisits[currentDungeonState!!.dungeonName] = visit
                }
            }
            currentDungeonVisit = null
        }

        // Handle dungeon entry - ONLY create new visit if we don't have one for this dungeon
        if (newState.hasEntered && currentDungeonVisit == null && newState.dungeonName.isNotEmpty()) {
            Log.d(TAG, "Dungeon entered for first time: ${newState.dungeonName}")

            // Check if we have this dungeon on hold
            currentDungeonVisit = onHoldVisits.remove(newState.dungeonName)?.also {
                Log.d(TAG, "Restored dungeon visit from hold: ${it.name}")
            }

            if (currentDungeonVisit == null) {
                currentDungeonVisit = DungeonVisit(
                    name = newState.dungeonName,
                    mode = newState.mode,
                    sessionId = currentWayvesselSession?.id,
                    startTime = LocalDateTime.now(),
                    orns = 0L,
                    gold = 0L,
                    experience = 0L,
                    floor = 0L,
                    godforges = 0L,
                    completed = false
                )
                Log.d(TAG, "Created new dungeon visit: ${newState.dungeonName}, mode: ${newState.mode}")

                currentDungeonVisit?.let { visit ->
                    serviceScope.launch {
                        try {
                            val id = dungeonRepository.insertVisit(visit)
                            currentDungeonVisit = visit.copy(id = id)
                            Log.d(TAG, "Saved initial dungeon visit with id: $id")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to save dungeon visit", e)
                        }
                    }
                }
            }

            updateOverlay()
        }

        // Handle floor change - ONLY update floor, don't create new visits
        if (newState.hasEntered && newState.floorNumber != currentDungeonState?.floorNumber && currentDungeonVisit != null) {
            val oldFloor = currentDungeonVisit?.floor ?: 0
            currentDungeonVisit = currentDungeonVisit?.copy(floor = newState.floorNumber.toLong())
            Log.d(TAG, "Floor changed from $oldFloor to ${newState.floorNumber} in same dungeon: ${currentDungeonVisit?.name}")
            updateDungeonInDatabase()
            updateOverlay()
        }

        // Handle godforge
        if (data.any { it.text.lowercase().contains("godforged") } && newState.hasEntered) {
            currentDungeonVisit = currentDungeonVisit?.copy(
                godforges = (currentDungeonVisit?.godforges ?: 0) + 1
            )
            Log.d(TAG, "Godforge detected!")
            updateDungeonInDatabase()
        }

        // Handle loot parsing - check if we're on a loot screen
        val hasVictory = data.any { it.text.lowercase().contains("victory") }
        val hasComplete = data.any { it.text.lowercase().contains("complete") }
        val hasLoot = data.any {
            val lower = it.text.lowercase()
            lower.contains("orns") || lower.contains("gold") || lower.contains("experience")
        }

        if ((hasVictory || hasComplete) && hasLoot && newState.hasEntered &&
            !newState.victoryScreenHandledForFloor
        ) {
            Log.d(TAG, "Victory/complete screen detected, parsing loot...")
            val loot = dungeonScreenParser.parseLoot(data)
            Log.d(TAG, "Parsed loot: $loot")

            if (loot.isNotEmpty()) {
                val ornsToAdd = loot["orns"] ?: 0
                val goldToAdd = loot["gold"] ?: 0
                val expToAdd = loot["experience"] ?: 0

                currentDungeonVisit = currentDungeonVisit?.copy(
                    orns = (currentDungeonVisit?.orns ?: 0) + ornsToAdd,
                    gold = (currentDungeonVisit?.gold ?: 0) + goldToAdd,
                    experience = (currentDungeonVisit?.experience ?: 0) + expToAdd
                )

                Log.d(TAG, "Updated dungeon loot - orns: +$ornsToAdd (total: ${currentDungeonVisit?.orns}), " +
                        "gold: +$goldToAdd (total: ${currentDungeonVisit?.gold}), " +
                        "exp: +$expToAdd (total: ${currentDungeonVisit?.experience})")

                updateDungeonInDatabase()

                currentWayvesselSession?.let { session ->
                    val updatedSession = session.copy(
                        orns = session.orns + ornsToAdd,
                        gold = session.gold + goldToAdd,
                        experience = session.experience + expToAdd
                    )
                    currentWayvesselSession = updatedSession
                    serviceScope.launch {
                        wayvesselRepository.updateSession(updatedSession)
                    }
                }

                updateOverlay()
            }
        }

        // Handle dungeon completion
        val isDefeat = data.any { it.text.lowercase().contains("defeat") }
        if ((hasComplete || isDefeat) &&
            newState.hasEntered && currentDungeonVisit != null
        ) {
            currentDungeonVisit?.let { visit ->
                val isCompleted = !isDefeat
                val completedVisit = visit.copy(
                    completed = isCompleted,
                    durationSeconds = java.time.temporal.ChronoUnit.SECONDS.between(
                        visit.startTime,
                        LocalDateTime.now()
                    )
                )

                Log.d(TAG, "Dungeon completed: $completedVisit")

                serviceScope.launch {
                    try {
                        dungeonRepository.updateVisit(completedVisit)
                        Log.d(TAG, "Saved completed dungeon visit to database")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update completed dungeon", e)
                    }
                }

                currentWayvesselSession?.let { session ->
                    val updatedSession = session.copy(dungeonsVisited = session.dungeonsVisited + 1)
                    currentWayvesselSession = updatedSession
                    serviceScope.launch {
                        wayvesselRepository.updateSession(updatedSession)
                    }
                }

                updateOverlay()
            }

            // Clear current visit after completion
            currentDungeonVisit = null
            Log.d(TAG, "Cleared current dungeon visit after completion")
        }
    }

    private suspend fun handleWayvesselStart(wayvesselName: String) {
        Log.d(TAG, "Starting wayvessel session: $wayvesselName")

        currentWayvesselSession?.let { session ->
            val endTime = LocalDateTime.now()
            val duration = java.time.temporal.ChronoUnit.SECONDS.between(session.startTime, endTime)
            val completedSession = session.copy(durationSeconds = duration)
            wayvesselRepository.updateSession(completedSession)
        }

        val session = WayvesselSession(
            name = wayvesselName,
            startTime = LocalDateTime.now()
        )
        val id = wayvesselRepository.insertSession(session)
        currentWayvesselSession = session.copy(id = id)

        updateOverlay()
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