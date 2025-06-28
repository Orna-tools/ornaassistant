package com.lloir.ornaassistant.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.lloir.ornaassistant.BuildConfig
import com.lloir.ornaassistant.domain.model.DungeonState
import com.lloir.ornaassistant.domain.model.DungeonVisit
import com.lloir.ornaassistant.domain.model.FloorReward
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenData
import com.lloir.ornaassistant.domain.model.ScreenType
import com.lloir.ornaassistant.service.overlay.OverlayManager
import com.lloir.ornaassistant.service.parser.impl.DungeonScreenParser
import com.lloir.ornaassistant.service.parser.ScreenParserManager
import com.lloir.ornaassistant.service.parser.DungeonStateTracker
import com.lloir.ornaassistant.domain.repository.DungeonRepository
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDateTime
import javax.inject.Inject
/**
 * Core accessibility service that monitors and processes Orna game screens.
 * 
 * This service is responsible for:
 * - Parsing screen content using Android's accessibility framework
 * - Detecting game state changes (dungeon entry/exit, battles, etc.)
 * - Tracking dungeon visits and rewards
 * - Managing overlay displays
 * - Emitting screen data for other components to consume
 * 
 * Performance considerations:
 * - This service processes many events per second, so efficiency is critical
 * - Heavy processing is done on background threads
 * - Caching is used to avoid redundant processing
 * - Logging is minimized in production builds
 * 
 * The service maintains state about current dungeon visits and tracks rewards
 * across multiple screens to provide a comprehensive gameplay enhancement.
 */
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
    lateinit var dungeonStateTracker: DungeonStateTracker

    @Inject
    lateinit var dungeonRepository: DungeonRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _screenDataFlow = MutableSharedFlow<ParsedScreen>(replay = 0)
    val screenDataFlow = _screenDataFlow.asSharedFlow()

    private var lastProcessTime = 0L
    private val minProcessInterval = 500L // Minimum 500ms between processing events

    private var isServiceReady = false
    private var initializationJob: Job? = null

    // MediaProjection related variables
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: android.media.projection.MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var mediaProjectionHandler: Handler? = null
    private var mediaProjectionThread: HandlerThread? = null
    private var displayMetrics: android.util.DisplayMetrics? = null
    private var hasScreenCapturePermission = false

    private var currentDungeonState: DungeonState? = null
    private var currentDungeonVisit: DungeonVisit? = null
    private var onHoldVisits = mutableMapOf<String, DungeonVisit>()
    private var lastDungeonCreationTime = 0L // Track when we last created a dungeon
    private val recentlyCreatedDungeons = mutableMapOf<String, Long>() // Track recent dungeons by name

    // Track recent victory/completion for reward parsing
    private var recentVictoryTime = 0L
    private var awaitingRewards = false

    // Cache management
    private var lastCacheCleanup = 0L
    private val cacheCleanupInterval = 300000L // 5 minutes

    companion object {
        private const val TAG = "OrnaAccessibilityService"
        private const val SERVICE_READY_DELAY = 1000L // 1 second

        // Supported packages
        private val SUPPORTED_PACKAGES = setOf(
            "playorna.com.orna",
            "com.discord"
        )

        // Noise patterns to filter out
        private val NOISE_PATTERNS = listOf(
            Regex("^[0-9_]+$"), // Pure numbers with underscores
            Regex("^chat.*", RegexOption.IGNORE_CASE), // Chat-related
            Regex("^\\d+_[a-z]$"), // Patterns like "3_m"
            Regex("^[\\d\\s:]+$"), // Time patterns like "12:34"
            Regex("^[\\d\\s.]+$"), // Decimal numbers
            Regex("^[\\d\\s,]+$"), // Numbers with commas
            Regex("^[\\d\\s%]+$"), // Percentage values
            Regex("^[\\d\\s/]+$"), // Fraction-like patterns
            Regex("^[a-zA-Z]$"), // Single letters
            Regex("^\\s*$"), // Empty or whitespace-only strings
            Regex("^[^a-zA-Z0-9]+$") // Strings with no alphanumeric characters
        )

        // Pre-compiled regex for small numbers to avoid creating new Regex objects repeatedly
        private val SMALL_NUMBER_REGEX = Regex("^\\d{1,6}$")

        // Regex for identifying potential item names
        private val POTENTIAL_ITEM_REGEX = Regex("^[A-Z][a-zA-Z\\s'\\-]+$")
    }

    // Helper function to check if debug logging is enabled
    private suspend fun isDebugEnabled(): Boolean {
        return try {
            settingsRepository.getSettings().debugMode
        } catch (e: Exception) {
            false
        }
    }

    // Helper function to check if ML Kit is enabled
    private suspend fun isMlKitEnabled(): Boolean {
        return try {
            settingsRepository.getSettings().useMlKit
        } catch (e: Exception) {
            false
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Accessibility service created")
        observeSettings()

        // Initialize MediaProjection related components
        initializeMediaProjection()

        // Register BroadcastReceiver for MediaProjection setup
        val intentFilter = IntentFilter("com.lloir.ornaassistant.SETUP_MEDIA_PROJECTION")
        registerReceiver(mediaProjectionReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        Log.d(TAG, "Registered MediaProjection receiver")

        // Android 16: Check for 16KB page size compatibility
        if (Build.VERSION.SDK_INT >= 35) {
            val pageSize = try {
                Settings.Global.getString(contentResolver, "memory_page_size")
            } catch (e: Exception) { null }
            Log.d(TAG, "Device page size: $pageSize")
        }
    }

    /**
     * Initializes MediaProjection related components.
     * This sets up the necessary objects for screen capture.
     */
    private fun initializeMediaProjection() {
        try {
            // Get the MediaProjectionManager system service
            mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

            // Get display metrics
            displayMetrics = resources.displayMetrics

            // Create a handler thread for MediaProjection
            mediaProjectionThread = HandlerThread("MediaProjectionThread")
            mediaProjectionThread?.start()
            mediaProjectionHandler = Handler(mediaProjectionThread?.looper ?: Looper.getMainLooper())

            Log.d(TAG, "MediaProjection components initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaProjection components", e)
        }
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

    private fun observeSettings() {
        serviceScope.launch {
            settingsRepository.getSettingsFlow().collect { settings ->
                if (!settings.showAssessOverlay) {
                    overlayManager.hideAssessmentOverlay()
                }

                // Update overlay transparency
                overlayManager.setOverlayTransparency(settings.overlayTransparency)

                // Handle ML Kit setting changes
                handleMlKitSettingChange(settings.useMlKit)
            }
        }
    }

    /**
     * BroadcastReceiver for handling MediaProjection setup.
     */
    private val mediaProjectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.lloir.ornaassistant.SETUP_MEDIA_PROJECTION") {
                Log.d(TAG, "Received MediaProjection setup intent")

                val resultCode = intent.getIntExtra("resultCode", -1)
                val data = intent.getParcelableExtra<Intent>("data")

                if (resultCode != -1 && data != null) {
                    setupMediaProjection(resultCode, data)
                } else {
                    Log.e(TAG, "Invalid MediaProjection data received")
                }
            }
        }
    }

    /**
     * Handles changes to the ML Kit setting.
     * This method ensures that the necessary permissions are requested when ML Kit is enabled.
     *
     * @param enabled Whether ML Kit is enabled
     */
    private fun handleMlKitSettingChange(enabled: Boolean) {
        if (enabled && !hasScreenCapturePermission) {
            // ML Kit is enabled but we don't have screen capture permission
            // Request permission through a notification or broadcast
            requestScreenCapturePermission()
        } else if (!enabled) {
            // ML Kit is disabled, clean up MediaProjection resources
            cleanupMediaProjection()
        }
    }

    /**
     * Requests screen capture permission.
     * Since this is a service and not an activity, we need to use a different approach.
     * We'll send a broadcast to the MainActivity to request permission.
     */
    private fun requestScreenCapturePermission() {
        try {
            // Create an intent to request screen capture permission
            val intent = Intent("com.lloir.ornaassistant.REQUEST_SCREEN_CAPTURE")
            intent.setPackage(packageName)
            sendBroadcast(intent)

            Log.d(TAG, "Sent broadcast to request screen capture permission")
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting screen capture permission", e)
        }
    }


    /**
     * Processes accessibility events to extract and analyze screen content.
     * This is the main entry point for all accessibility event processing and is called
     * frequently, so it needs to be highly optimized.
     *
     * The method:
     * 1. Filters events based on package name and throttles processing
     * 2. Parses the accessibility tree to extract text content
     * 3. Determines the screen type and updates state accordingly
     * 4. Handles dungeon state changes and tracks rewards
     * 5. Updates overlays and emits screen data for observers
     *
     * @param event The accessibility event to process
     */
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

        // Only process events from supported packages
        if (!SUPPORTED_PACKAGES.contains(packageName)) {
            return
        }

        // Periodically clean up caches to prevent memory leaks
        if (currentTime - lastCacheCleanup > cacheCleanupInterval) {
            serviceScope.launch(Dispatchers.IO) {
                cleanupCaches()
                lastCacheCleanup = currentTime
            }
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

                // Check if ML Kit is enabled
                val useMlKit = runBlocking { isMlKitEnabled() }

                // Parse screen data using appropriate method
                val screenData = if (useMlKit) {
                    // Log that we're using ML Kit
                    Log.d(TAG, "ML Kit is enabled, attempting to use it for screen parsing")

                    // Capture screenshot and process with ML Kit
                    val bitmap = captureScreenshot()
                    if (bitmap != null) {
                        // Determine screen type before processing
                        val preliminaryScreenType = determineScreenTypeFromAccessibilityTree(sourceNode)

                        // Process with ML Kit
                        val mlKitProcessed = runBlocking { 
                            screenParserManager.processScreenWithMlKit(bitmap, preliminaryScreenType)
                        }

                        if (mlKitProcessed) {
                            // If ML Kit processing was successful, return an empty list here
                            // because the processing has already been done in processScreenWithMlKit
                            Log.d(TAG, "ML Kit processing successful, skipping accessibility tree parsing")
                            emptyList()
                        } else {
                            // Fallback to accessibility tree if ML Kit processing failed
                            Log.d(TAG, "ML Kit processing failed, falling back to accessibility tree")
                            parseAccessibilityTree(sourceNode)
                        }
                    } else {
                        // Fallback to accessibility tree if screenshot capture failed
                        Log.d(TAG, "Screenshot capture failed, falling back to accessibility tree")
                        parseAccessibilityTree(sourceNode)
                    }
                } else {
                    // Use regular accessibility tree parsing
                    parseAccessibilityTree(sourceNode)
                }

                if (screenData.isEmpty()) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "No screen data extracted, skipping")
                    }
                    return@launch
                }

                // Check if we should skip processing based on screen content
                if (shouldSkipProcessing(screenData)) {
                    if (BuildConfig.DEBUG) {
                        runBlocking {
                            if (isDebugEnabled()) {
                                Log.d(TAG, "Skipping processing - detected non-dungeon screen")
                            }
                        }
                    }
                    return@launch
                }

                // Only log screen data details in debug mode and only if debug logging is enabled
                if (BuildConfig.DEBUG && screenData.size > 0) {
                    runBlocking {
                        if (isDebugEnabled()) {
                            Log.d(TAG, "Screen data sample (${screenData.size} items):")
                            screenData.take(5).forEach { data ->
                                Log.d(TAG, "  - '${data.text}'")
                            }
                        }
                    }
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
                        try {
                            screenParserManager.clearItemAssessment()
                        } catch (e: SecurityException) {
                            // Android 16: Handle intent redirection security improvements
                            Log.w(TAG, "Security restriction on intent handling", e)
                        }
                    }
                }

                // Handle abandoned job detection for Android 16

                // Emit the parsed screen data
                _screenDataFlow.emit(parsedScreen)

                // Update overlays on main thread with additional safety checks
                withContext(Dispatchers.Main) {
                    if (isServiceReady) {
                        overlayManager.handleScreenUpdate(parsedScreen)
                    }
                }

                // Always check for dungeon screens, regardless of detected screen type
                val previousDungeonState = currentDungeonState
                val isDungeonScreen = dungeonScreenParser.canParse(screenData)
                Log.d(TAG, "Is dungeon screen: $isDungeonScreen, detected type: $screenType")

                if (isDungeonScreen) {
                    try {
                        val newState =
                            dungeonScreenParser.parseState(screenData, currentDungeonState)
                        Log.d(TAG, "Current dungeon state: $currentDungeonState")
                        Log.d(TAG, "New dungeon state: $newState")

                        // Always update state and handle changes
                        handleDungeonStateChange(newState, screenData)

                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing dungeon state", e)
                    }
                } else if (previousDungeonState?.hasEntered == true && !isDungeonScreen) {
                    // We've left the dungeon screen but haven't seen completion
                    // Don't clear the visit yet - they might be in inventory or something
                    Log.d(TAG, "Left dungeon screen but keeping visit active")
                }

                // Check for victory or completion screens using the optimized helper method
                val hasVictoryScreen = containsText(screenData, "VICTORY!")
                val hasDungeonComplete = containsText(screenData, "DUNGEON COMPLETE!")

                // Only log in debug mode to reduce overhead
                if (BuildConfig.DEBUG) {
                    if (hasVictoryScreen) {
                        Log.d(TAG, "=== VICTORY SCREEN DETECTED ===")
                        Log.d(TAG, "Looking for rewards in ${screenData.size} items")
                    }

                    if (hasDungeonComplete) {
                        Log.d(TAG, "=== DUNGEON COMPLETE SCREEN DETECTED ===")
                        Log.d(TAG, "Looking for rewards in ${screenData.size} items")
                    }
                }

                if (hasVictoryScreen || hasDungeonComplete) {
                    recentVictoryTime = System.currentTimeMillis()
                    awaitingRewards = true

                    // Only log detailed information in debug mode
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Victory/completion detected, awaiting rewards...")

                        // Find the victory/complete text index more efficiently
                        val victoryIndex = screenData.indexOfFirst { 
                            containsText(listOf(it), "VICTORY!") || 
                            containsText(listOf(it), "DUNGEON COMPLETE!") 
                        }

                        // Log items after the victory/complete text for debugging
                        if (victoryIndex >= 0 && victoryIndex < screenData.size - 5) {
                            Log.d(TAG, "Items after victory/complete:")
                            for (i in 1..5) {
                                if (victoryIndex + i < screenData.size) {
                                    Log.d(TAG, "  +$i: '${screenData[victoryIndex + i].text}'")
                                }
                            }
                        }
                    }
                }

                // Check if we're seeing potential reward numbers shortly after victory
                // Remove the incorrect code that treats all numbers as orns
                // The proper parsing is done in DungeonScreenParser

                if (hasVictoryScreen) {
                    try {
                        Log.d(TAG, "Victory screen detected!")
                        val battleLoot = dungeonScreenParser.parseBattleLoot(screenData)

                        Log.d(
                            TAG,
                            "Battle loot parsed: orns=${battleLoot["orns"]}, gold=${battleLoot["gold"]}, exp=${battleLoot["experience"]}"
                        )
                        Log.d(
                            TAG,
                            "Current dungeon visit: ${currentDungeonVisit?.name} (ID: ${currentDungeonVisit?.id})"
                        )

                        if (battleLoot.isNotEmpty()) {
                            // Add to current dungeon visit if one exists
                            currentDungeonVisit?.let { visit ->
                                Log.d(
                                    TAG,
                                    "Current visit before update: orns=${visit.orns}, gold=${visit.gold}, exp=${visit.experience}"
                                )
                                Log.d(
                                    TAG,
                                    "Battle loot to add: orns=${battleLoot["orns"]}, gold=${battleLoot["gold"]}, exp=${battleLoot["experience"]}"
                                )

                                // Create updated visit with new values

                                val updatedVisit = visit.copy(
                                    battleOrns = visit.battleOrns + (battleLoot["orns"] ?: 0),
                                    battleGold = visit.battleGold + (battleLoot["gold"] ?: 0),
                                    battleExperience = visit.battleExperience + (battleLoot["experience"]
                                        ?: 0),
                                    // Update totals
                                    orns = visit.orns + (battleLoot["orns"] ?: 0),
                                    gold = visit.gold + (battleLoot["gold"] ?: 0),
                                    experience = visit.experience + (battleLoot["experience"] ?: 0)
                                )

                                // Important: Update the currentDungeonVisit reference
                                currentDungeonVisit = updatedVisit

                                Log.d(
                                    TAG,
                                    "Updated visit: orns=${updatedVisit.orns}, gold=${updatedVisit.gold}, exp=${updatedVisit.experience}"
                                )


                                // Update database with the new values
                                serviceScope.launch {
                                    dungeonRepository.updateVisit(updatedVisit)
                                    Log.d(TAG, "Database updated with battle loot")
                                }
                                updateOverlay()

                                Log.d(
                                    TAG,
                                    "Added battle loot - orns: ${battleLoot["orns"]}, gold: ${battleLoot["gold"]}, exp: ${battleLoot["experience"]}"
                                )
                            }

                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing victory screen", e)
                    }
                }

                // Process general screen parsing
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

    private fun shouldSkipProcessing(screenData: List<ScreenData>): Boolean {
        val skipIndicators = setOf(
            "inventory", "runeshop", "shop", "arena", "codex", "settings",
            "profile", "friends", "guild", "kingdom", "chat", "inbox",
            "notifications", "archpaths", "status", "steps"
        )

        // If we see any of these indicators, skip processing unless we also see dungeon indicators
        val hasSkipIndicator = screenData.any { data ->
            skipIndicators.any { indicator ->
                data.text.lowercase().contains(indicator)
            }
        }

        if (hasSkipIndicator) {
            // Check if we also have dungeon indicators that override the skip
            val hasDungeonIndicator = screenData.any { data ->
                data.text.lowercase().contains("dungeon") ||
                data.text.lowercase().contains("floor") ||
                data.text.lowercase().contains("victory") ||
                data.text.lowercase().contains("complete")
            }
            return !hasDungeonIndicator
        }

        return false
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

        // Clean up MediaProjection resources
        cleanupMediaProjection()

        // Unregister BroadcastReceiver
        try {
            unregisterReceiver(mediaProjectionReceiver)
            Log.d(TAG, "Unregistered MediaProjection receiver")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering MediaProjection receiver", e)
        }

        // Cancel the service scope
        serviceScope.cancel()

        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
    }

    /**
     * Cleans up MediaProjection resources.
     * This should be called when the service is being destroyed.
     */
    private fun cleanupMediaProjection() {
        try {
            // Release MediaProjection resources
            mediaProjection?.stop()
            mediaProjection = null

            // Release ImageReader
            imageReader?.close()
            imageReader = null

            // Quit handler thread
            mediaProjectionThread?.quitSafely()
            mediaProjectionThread = null
            mediaProjectionHandler = null

            // Reset permission flag
            hasScreenCapturePermission = false

            Log.d(TAG, "MediaProjection resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up MediaProjection resources", e)
        }
    }

    /**
     * Sets up MediaProjection with the result from the permission request.
     * This method is called when the permission result is received from MainActivity.
     *
     * @param resultCode The result code from the permission request
     * @param data The intent data from the permission request
     */
    private fun setupMediaProjection(resultCode: Int, data: Intent) {
        try {
            // Clean up any existing MediaProjection resources
            cleanupMediaProjection()

            // Initialize MediaProjectionManager if not already initialized
            if (mediaProjectionManager == null) {
                mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            }

            // Create MediaProjection
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)

            if (mediaProjection != null) {
                // Set up handler thread if needed
                if (mediaProjectionThread == null || !mediaProjectionThread!!.isAlive) {
                    mediaProjectionThread = HandlerThread("MediaProjectionThread")
                    mediaProjectionThread?.start()
                    mediaProjectionHandler = Handler(mediaProjectionThread?.looper ?: Looper.getMainLooper())
                }

                hasScreenCapturePermission = true
                Log.d(TAG, "MediaProjection set up successfully")
            } else {
                Log.e(TAG, "Failed to set up MediaProjection")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up MediaProjection", e)
            hasScreenCapturePermission = false
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Accessibility service unbound")
        isServiceReady = false
        overlayManager.clearAccessibilityService()
        return super.onUnbind(intent)
    }

    /**
     * Parses the accessibility tree to extract text content from the screen.
     * This is a key method for screen content analysis and should be optimized for performance.
     *
     * @param rootNode The root node of the accessibility tree
     * @return List of ScreenData objects containing extracted text and metadata
     */
    private fun parseAccessibilityTree(rootNode: AccessibilityNodeInfo?): List<ScreenData> {
        if (rootNode == null) return emptyList()

        // Pre-allocate with a reasonable capacity to avoid resizing
        val screenData = ArrayList<ScreenData>(100)
        val visitedNodes = HashSet<AccessibilityNodeInfo>(100)
        val startTime = System.currentTimeMillis()

        try {
            parseNodeRecursively(rootNode, screenData, visitedNodes, 0)

            // Log performance metrics in debug mode
            if (BuildConfig.DEBUG) {
                val duration = System.currentTimeMillis() - startTime
                if (duration > 100) { // Only log if parsing took more than 100ms
                    Log.d(TAG, "Parsed accessibility tree in ${duration}ms, found ${screenData.size} items")
                }
            }
        } catch (e: IllegalStateException) {
            // Handle specific exceptions that might occur during parsing
            Log.e(TAG, "IllegalStateException parsing accessibility tree: ${e.message}")
        } catch (e: NullPointerException) {
            // Handle NPEs that might occur if nodes are recycled unexpectedly
            Log.e(TAG, "NullPointerException parsing accessibility tree: ${e.message}")
        } catch (e: Exception) {
            // Handle general exceptions
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error parsing accessibility tree", e)
            } else {
                Log.e(TAG, "Error parsing accessibility tree: ${e.message}")
            }
        } finally {
            // Clean up visited nodes to prevent memory leaks
            visitedNodes.forEach { node ->
                try {
                    if (node != rootNode) {
                        node.recycle()
                    }
                } catch (e: Exception) {
                    // Minimize logging for common recycling errors
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "Error recycling node: ${e.message}")
                    }
                }
            }
        }

        return screenData
    }

    /**
     * Recursively parses an accessibility node and its children to extract text content.
     * This method has been optimized for performance and robustness.
     *
     * @param node The accessibility node to parse
     * @param screenData The list to add extracted screen data to
     * @param visitedNodes Set of already visited nodes to prevent infinite recursion
     * @param depth Current recursion depth
     */
    private fun parseNodeRecursively(
        node: AccessibilityNodeInfo,
        screenData: MutableList<ScreenData>,
        visitedNodes: MutableSet<AccessibilityNodeInfo>,
        depth: Int
    ) {
        // Early return conditions:
        // 1. Prevent excessive recursion depth
        // 2. Avoid circular references
        // 3. Skip invisible nodes
        if (depth > 30 || visitedNodes.contains(node) || !node.isVisibleToUser) {
            return
        }

        visitedNodes.add(node)

        try {
            // Extract text content - only process if node is not a container
            // Many containers have no useful text but have children with text
            if (!isLikelyContainer(node)) {
                val nodeText = node.text?.toString()
                val contentDesc = node.contentDescription?.toString()

                // Only process if there's actual text content
                if (!nodeText.isNullOrBlank() || !contentDesc.isNullOrBlank()) {
                    val bounds = Rect()
                    node.getBoundsInScreen(bounds)

                    // Skip nodes with zero width or height (likely invisible)
                    if (bounds.width() <= 0 || bounds.height() <= 0) {
                        return
                    }

                    val currentTime = System.currentTimeMillis() // Get timestamp once for all items

                    // Process node text if present
                    if (!nodeText.isNullOrBlank()) {
                        processTextContent(nodeText, bounds, currentTime, depth, screenData)
                    }

                    // Process content description if present and different from text
                    if (!contentDesc.isNullOrBlank() && contentDesc != nodeText) {
                        processTextContent(contentDesc, bounds, currentTime, depth, screenData)
                    }
                }
            }

            // Process child nodes - use a more efficient approach for large node trees
            val childCount = node.childCount

            // Skip processing children if we already have a lot of data
            // This prevents excessive processing for very large screens
            if (screenData.size > 300 && depth > 10) {
                return
            }

            for (i in 0 until childCount) {
                try {
                    val child = node.getChild(i) ?: continue
                    parseNodeRecursively(child, screenData, visitedNodes, depth + 1)
                } catch (e: IllegalStateException) {
                    // Node might have been recycled, just continue
                    continue
                } catch (e: Exception) {
                    // Only log if debug is enabled to reduce log spam
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "Error processing child node at index $i: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            // Only log if debug is enabled to reduce log spam
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Error processing node at depth $depth: ${e.message}")
            }
        }
    }

    /**
     * Determines if a node is likely a container that doesn't have useful text itself.
     * This helps optimize parsing by focusing on nodes that are likely to contain actual content.
     */
    private fun isLikelyContainer(node: AccessibilityNodeInfo): Boolean {
        // Check if node has children but no text
        return node.childCount > 0 && 
               node.text == null && 
               node.contentDescription == null &&
               (node.className?.contains("Layout") == true || 
                node.className?.contains("Container") == true ||
                node.className?.contains("View") == true)
    }

    /**
     * Processes a single text item from an accessibility node.
     * 
     * @param text The text content to process
     * @param bounds The screen bounds of the text
     * @param timestamp Current timestamp to use for all items
     * @param depth Current recursion depth
     * @param screenData The list to add extracted screen data to
     */
    private fun processTextContent(
        text: String,
        bounds: Rect,
        timestamp: Long,
        depth: Int,
        screenData: MutableList<ScreenData>
    ) {
        // Skip empty or very short text
        if (text.length < 2) {
            return
        }

        // Clean the text - remove extra whitespace and normalize
        val cleanedText = text.trim().replace(Regex("\\s+"), " ")

        // Filter out noise
        val isNoise = NOISE_PATTERNS.any { pattern -> pattern.matches(cleanedText) }

        // Don't filter out small numbers that could be rewards
        val isSmallNumber = SMALL_NUMBER_REGEX.matches(cleanedText)
        val numberValue = cleanedText.toIntOrNull()
        val isPotentialReward = isSmallNumber && numberValue != null && numberValue in 1..999999

        // Check if this might be an item name
        val isPotentialItem = POTENTIAL_ITEM_REGEX.matches(cleanedText)

        // Keep text if it's a potential reward or item name, otherwise filter if it's noise
        if (isNoise && !isPotentialReward && !isPotentialItem) {
            // Skip noise but don't log in production to reduce overhead
            if (BuildConfig.DEBUG) {
                runBlocking {
                    if (isDebugEnabled()) {
                        Log.v(TAG, "Filtering noise: '$cleanedText'")
                    }
                }
            }
            return
        }

        // Only log potential rewards in debug mode
        if (isPotentialReward && BuildConfig.DEBUG) {
            Log.d(TAG, "Found potential reward number: $cleanedText")
        }

        // Log potential item names in debug mode
        if (isPotentialItem && BuildConfig.DEBUG) {
            Log.d(TAG, "Found potential item name: '$cleanedText'")
        }

        // Add the cleaned text to screen data
        screenData.add(
            ScreenData(
                text = cleanedText,
                bounds = bounds,
                timestamp = timestamp,
                depth = depth
            )
        )
    }

    private suspend fun cleanupCaches() {
        // Clean up recent dungeons older than 30 seconds
        val currentTime = System.currentTimeMillis()
        recentlyCreatedDungeons.entries.removeIf { 
            currentTime - it.value > 30000
        }

        Log.d(TAG, "Cache cleanup completed")
    }

    private fun updateOverlay() {
        serviceScope.launch {
            try {
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update overlay", e)
            }
        }
    }

    private fun updateDungeonInDatabase() {
        currentDungeonVisit?.let { visit ->
            if (visit.id > 0) {
                Log.d(TAG, "=== UPDATING DUNGEON IN DATABASE ===")
                Log.d(TAG, "  - ID: ${visit.id}")
                Log.d(TAG, "  - Name: ${visit.name}")
                Log.d(TAG, "  - Floor: ${visit.floor}")
                Log.d(
                    TAG,
                    "  - Orns: ${visit.orns} (battle: ${visit.battleOrns}, floor: ${visit.floorOrns})"
                )
                Log.d(
                    TAG,
                    "  - Gold: ${visit.gold} (battle: ${visit.battleGold}, floor: ${visit.floorGold})"
                )
                Log.d(
                    TAG,
                    "  - Exp: ${visit.experience} (battle: ${visit.battleExperience}, floor: ${visit.floorExperience})"
                )
                Log.d(TAG, "  - Floor rewards: ${visit.floorRewards}")
                serviceScope.launch {
                    try {
                        dungeonRepository.updateVisit(visit)
                        Log.d(TAG, "Database update completed for visit ID: ${visit.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update dungeon in database", e)
                    }
                }
            }
        }
    }

    private suspend fun handleDungeonStateChange(newState: DungeonState, data: List<ScreenData>) {
        // Create mutable copy of state for modifications
        var updatedState = newState

        Log.d(TAG, "=== DUNGEON STATE CHANGE START ===")
        Log.d(TAG, "Current visit: ${currentDungeonVisit?.name} (ID: ${currentDungeonVisit?.id})")
        Log.d(TAG, "New state dungeon: ${updatedState.dungeonName}")
        Log.d(TAG, "Is entering new: ${updatedState.isEnteringNewDungeon}")

        // CRITICAL FIX: If we already have a visit for this dungeon, don't create a new one
        if (currentDungeonVisit != null &&
            currentDungeonVisit?.name == updatedState.dungeonName &&
            updatedState.dungeonName.isNotEmpty() &&
            updatedState.dungeonName != "Unknown Dungeon"
        ) {

            Log.d(TAG, "DUPLICATE PREVENTION: Already tracking ${updatedState.dungeonName}")

            // Update existing visit instead of creating new one
            var visitUpdated = false

            // Update floor if higher
            if (updatedState.floorNumber > (currentDungeonVisit?.floor ?: 0).toInt()) {
                currentDungeonVisit =
                    currentDungeonVisit?.copy(floor = updatedState.floorNumber.toLong())
                visitUpdated = true
                Log.d(TAG, "Updated floor to ${updatedState.floorNumber}")
            }

            // Handle completion/defeat
            if (updatedState.isDone && currentDungeonVisit?.completed != true) {
                val isComplete = data.any { it.text.contains("COMPLETE", ignoreCase = true) }
                currentDungeonVisit = currentDungeonVisit?.copy(
                    completed = isComplete,
                    durationSeconds = java.time.temporal.ChronoUnit.SECONDS.between(
                        currentDungeonVisit!!.startTime,
                        LocalDateTime.now()
                    )
                )
                visitUpdated = true
                Log.d(TAG, "Marked dungeon as ${if (isComplete) "completed" else "failed"}")
            }

            if (visitUpdated) {
                updateDungeonInDatabase()
                updateOverlay()
            }

            // Don't process further - we're updating existing visit
            currentDungeonState = updatedState
            return
        }

        // DEDUPLICATION CHECK: Don't create duplicate dungeons within 5 seconds
        val currentTime = System.currentTimeMillis()
        val timeSinceLastCreation = currentTime - lastDungeonCreationTime

        if (timeSinceLastCreation < 5000 &&
            currentDungeonVisit?.name == updatedState.dungeonName &&
            updatedState.dungeonName.isNotEmpty()
        ) {
            Log.d(
                TAG,
                "Duplicate dungeon creation prevented (too soon) for: ${updatedState.dungeonName}"
            )
            currentDungeonState = updatedState
            return
        }

        // ADDITIONAL CHECK: Don't create if we recently created this dungeon
        val recentCreationTime = recentlyCreatedDungeons[updatedState.dungeonName] ?: 0L
        if (currentTime - recentCreationTime < 10000) { // 10 seconds window
            Log.d(TAG, "Duplicate prevented: ${updatedState.dungeonName} was created ${currentTime - recentCreationTime}ms ago")
            currentDungeonState = updatedState
            return
        }

        Log.d(
            TAG,
            "handleDungeonStateChange: newState=$newState, hasEntered=${newState.hasEntered}, currentState=$currentDungeonState"
        )

        // Extract dungeon name from completion screen if we don't have it
        if (updatedState.dungeonName == "Unknown Dungeon" || updatedState.dungeonName.isEmpty()) {
            // Try multiple strategies to find dungeon name
            if (data.any {
                    it.text.contains("DUNGEON COMPLETE!", ignoreCase = true) ||
                            it.text.contains("DEFEAT", ignoreCase = true) ||
                            (it.text.contains("Floor", ignoreCase = true) && it.text.contains("/"))
                }) {
                val betterName = dungeonScreenParser.extractDungeonNameFromData(data)
                if (betterName != null && betterName != "Unknown Dungeon") {
                    updatedState = updatedState.copy(dungeonName = betterName)
                    Log.d(TAG, "Updated dungeon name to: $betterName")
                }
            }
        }

        // If we detect we're in a dungeon but don't have a name, try harder to find it
        if (updatedState.hasEntered && updatedState.dungeonName.isEmpty()) {
            // Look for any text that could be a dungeon name
            val possibleDungeonName = data.firstOrNull {
                it.text.endsWith(" Dungeon") ||
                        it.text.endsWith(" Gauntlet") ||
                        it.text.contains("Valley of the Gods") ||
                        it.text.contains("Underworld")
            }?.text

            if (possibleDungeonName != null) {
                updatedState = updatedState.copy(dungeonName = possibleDungeonName)
                Log.d(TAG, "Found dungeon name mid-run: $possibleDungeonName")
            } else {
                // Don't create "Unknown Dungeon" entries
                Log.w(TAG, "In dungeon but couldn't determine name, not creating visit yet")
                return
            }
        }

        // Handle entering new dungeon - only if it's truly a different dungeon
        val isDifferentDungeon = updatedState.dungeonName != currentDungeonState?.dungeonName &&
            updatedState.dungeonName.isNotEmpty() &&
            updatedState.dungeonName != "Unknown Dungeon"

        // Check if this is actually a dungeon selection screen (not mid-dungeon)
        val isDungeonSelectionScreen = data.any {
            it.text.contains("world dungeon", ignoreCase = true) ||
            it.text.contains("special dungeon", ignoreCase = true) ||
            it.text.contains("hold to enter", ignoreCase = true)
        }

        if (updatedState.dungeonName != currentDungeonState?.dungeonName &&
            updatedState.dungeonName.isNotEmpty() &&
            updatedState.dungeonName != "Unknown Dungeon" &&
            (isDungeonSelectionScreen || isDifferentDungeon)) {

            Log.d(TAG, "New dungeon detected: ${updatedState.dungeonName}")

            // Put current dungeon on hold if it exists and isn't done
            if (currentDungeonState?.dungeonName?.isNotEmpty() == true &&
                currentDungeonState?.isDone != true &&
                currentDungeonVisit != null
            ) {
                Log.d(TAG, "Putting ${currentDungeonState!!.dungeonName} on hold")
                onHoldVisits[currentDungeonState!!.dungeonName] = currentDungeonVisit!!
                updateDungeonInDatabase() // Save current progress
            }
            // Reset for new dungeon
            currentDungeonVisit = null
        }

        // Handle dungeon entry - ONLY if we don't already have a visit
        if (updatedState.hasEntered &&
            updatedState.dungeonName.isNotEmpty() &&
            updatedState.dungeonName != "Unknown Dungeon" &&
            currentDungeonVisit == null
        ) { // THIS IS THE KEY CHECK

            Log.d(TAG, "Dungeon entered: ${updatedState.dungeonName}")

            // Check if we have this dungeon on hold
            val onHoldVisit = onHoldVisits.remove(updatedState.dungeonName)
            if (onHoldVisit != null) {
                currentDungeonVisit = onHoldVisit
                Log.d(TAG, "Resuming dungeon from hold: ${updatedState.dungeonName}")
            } else {
                // Create new visit only if we don't have one for this dungeon
                currentDungeonVisit = DungeonVisit(
                    name = updatedState.dungeonName,
                    mode = updatedState.mode,
                    sessionId = null,
                    startTime = LocalDateTime.now()
                )
                Log.d(
                    TAG,
                    "Created new dungeon visit: ${updatedState.dungeonName}, mode: ${updatedState.mode}"
                )

                // Track creation time for deduplication
                lastDungeonCreationTime = System.currentTimeMillis()
                recentlyCreatedDungeons[updatedState.dungeonName] = System.currentTimeMillis()

                // Save the initial visit to database
                currentDungeonVisit?.let { visit ->
                    serviceScope.launch {
                        try {
                            val id = dungeonRepository.insertVisit(visit)
                            currentDungeonVisit = visit.copy(id = id)
                            Log.d(TAG, "Saved initial dungeon visit with id: $id")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to save dungeon visit", e)
                        }

                        // 'data' is the parameter of handleDungeonStateChange, correctly captured.
                        data.forEach { item ->
                            if (item.text.contains("VICTORY", ignoreCase = true) ||
                                item.text.contains("orns", ignoreCase = true) ||
                                item.text.contains("gold", ignoreCase = true) ||
                                item.text.contains("experience", ignoreCase = true)
                            ) {
                                Log.d(TAG, "LOOT INDICATOR (in new visit scope): '${item.text}'")
                            }
                        }
                    }
                }
            }

            // Update overlay if enabled
            updateOverlay()
        }

        // Handle floor change
        if (updatedState.hasEntered && currentDungeonVisit != null) {
            val currentFloor = currentDungeonVisit?.floor ?: 0
            val newFloor = updatedState.floorNumber.toLong()

            if (newFloor > currentFloor) {
                currentDungeonVisit = currentDungeonVisit?.copy(floor = newFloor)
                Log.d(TAG, "Floor updated to: $newFloor (was: $currentFloor)")
                updateDungeonInDatabase()
            }
        }

        // Handle godforge
        if (data.any { it.text.lowercase().contains("godforged") } && updatedState.hasEntered) {
            currentDungeonVisit = currentDungeonVisit?.copy(
                godforges = (currentDungeonVisit?.godforges ?: 0) + 1
            )
            Log.d(TAG, "Godforge detected!")
            updateDungeonInDatabase()
        }

        // Handle loot on victory/complete
        if ((data.any { item -> item.text.lowercase().contains("victory") } || // Changed it to item
                    data.any { item ->
                        item.text.lowercase().contains("complete")
                    }) && // Changed it to item
            !updatedState.victoryScreenHandledForFloor && updatedState.hasEntered
        ) {
            // Check if this is a floor completion (has "Floor" text visible)
            val isFloorCompletion = data.any { item -> // Changed it to item
                item.text.lowercase().contains("floor") &&
                        item.text.contains("/")
            }

            val isDungeonCompleteScreen = data.any { item -> // Changed it to item
                item.text.equals("DUNGEON COMPLETE!", ignoreCase = true)
            }

            if (isFloorCompletion || isDungeonCompleteScreen) {
                Log.d(
                    TAG,
                    "Completion detected (floor: $isFloorCompletion, dungeon: $isDungeonCompleteScreen), parsing loot..."
                )
                val loot = dungeonScreenParser.parseLoot(data)
                Log.d(TAG, "Parsed floor loot: $loot")

                // If no loot found, mark that we're waiting for rewards
                if (loot.isEmpty()) {
                    recentVictoryTime = System.currentTimeMillis()
                    awaitingRewards = true
                    Log.d(TAG, "No loot found, marking as awaiting rewards")
                } else {
                    // Pass the specific floor number and the screen data
                    processLootRewards(loot, updatedState.floorNumber, data)
                    updatedState =
                        updatedState.copy(victoryScreenHandledForFloor = true) // Update state after processing
                }
            }
        }

        // Update the state at the end
        currentDungeonState = updatedState
    }

    // Pass 'screenNodeData' (which is the original 'data' from onAccessibilityEvent)
    private fun processLootRewards(
        loot: Map<String, Int>,
        floorNumber: Int,
        screenNodeData: List<ScreenData>
    ) {
        val ornsToAdd = loot["orns"] ?: 0
        val goldToAdd = loot["gold"] ?: 0
        val expToAdd = loot["experience"] ?: 0

        if (ornsToAdd > 0 || goldToAdd > 0 || expToAdd > 0) { // Process if actual loot is found
            Log.d(TAG, "Processing loot: orns=$ornsToAdd, gold=$goldToAdd, exp=$expToAdd")
            currentDungeonVisit?.let { visit ->
                // Create floor reward entry
                val floorReward = FloorReward(
                    floor = floorNumber,
                    orns = ornsToAdd.toLong(),
                    gold = goldToAdd.toLong(),
                    experience = expToAdd.toLong()
                )

                Log.d(TAG, "=== FLOOR LOOT TRACKING ===")
                Log.d(TAG, "Floor ${floorNumber} loot:")
                Log.d(TAG, "  - Orns to add: $ornsToAdd")
                Log.d(TAG, "  - Gold to add: $goldToAdd")
                Log.d(TAG, "  - Exp to add: $expToAdd")
                Log.d(TAG, "Current visit before update:")
                Log.d(TAG, "  - Total orns: ${currentDungeonVisit?.orns}")
                Log.d(TAG, "  - Total gold: ${currentDungeonVisit?.gold}")
                Log.d(TAG, "  - Total exp: ${currentDungeonVisit?.experience}")

                // Add to floor rewards list
                val updatedFloorRewards =
                    currentDungeonVisit?.floorRewards?.toMutableList() ?: mutableListOf()

                // Check if we already have rewards for this floor (update if so)
                val existingIndex = updatedFloorRewards.indexOfFirst { it.floor == floorNumber }
                if (existingIndex >= 0) {
                    updatedFloorRewards[existingIndex] = floorReward
                } else {
                    updatedFloorRewards.add(floorReward)
                }

                currentDungeonVisit = currentDungeonVisit?.copy(
                    floorOrns = (currentDungeonVisit?.floorOrns ?: 0) + ornsToAdd,
                    floorGold = (currentDungeonVisit?.floorGold ?: 0) + goldToAdd,
                    floorExperience = (currentDungeonVisit?.floorExperience ?: 0) + expToAdd,
                    // Update totals
                    orns = (currentDungeonVisit?.orns ?: 0) + ornsToAdd,
                    gold = (currentDungeonVisit?.gold ?: 0) + goldToAdd,
                    experience = (currentDungeonVisit?.experience ?: 0) + expToAdd,
                    floorRewards = updatedFloorRewards
                )

                Log.d(TAG, "Current visit after update:")
                Log.d(TAG, "  - Total orns: ${currentDungeonVisit?.orns}")
                Log.d(TAG, "  - Total gold: ${currentDungeonVisit?.gold}")
                Log.d(TAG, "  - Total exp: ${currentDungeonVisit?.experience}")
                Log.d(TAG, "  - Floor rewards count: ${currentDungeonVisit?.floorRewards?.size}")
                Log.d(TAG, "=== END FLOOR LOOT TRACKING ===")

                Log.d(
                    TAG,
                    "Updated floor loot - orns: +$ornsToAdd (total: ${currentDungeonVisit?.orns}), " +
                            "gold: +$goldToAdd (total: ${currentDungeonVisit?.gold}), " +
                            "exp: +$expToAdd (total: ${currentDungeonVisit?.experience})"
                )
                Log.d(TAG, "Floor rewards: ${currentDungeonVisit?.floorRewards}")

                updateDungeonInDatabase()

            }

            // Mark that we've handled victory screen for this floor in our mutable state
            updateOverlay()

        }

        // Handle dungeon completion
        if (currentDungeonVisit != null && 
            (screenNodeData.any { item ->
                item.text.equals(
                    "DUNGEON COMPLETE!",
                    ignoreCase = true
                )
            } || screenNodeData.any { item ->
                item.text.equals(
                    "DEFEAT",
                    ignoreCase = true
                )
            })
        ) {
            // Only mark as complete if we see "COMPLETE", not "DEFEAT"
            val isComplete = screenNodeData.any { item -> item.text.lowercase().contains("complete") }
            Log.d(TAG, "Dungeon ${if (isComplete) "completed" else "failed"}")

            // Parse final dungeon rewards if it's a completion
            if (isComplete) {
                Log.d(TAG, "Parsing dungeon completion rewards...")
                val dungeonLoot = dungeonScreenParser.parseLoot(screenNodeData)

                if (dungeonLoot.isNotEmpty()) { // Process if actual loot is found
                    currentDungeonVisit = currentDungeonVisit?.copy(
                        floorOrns = (currentDungeonVisit?.floorOrns ?: 0) + (dungeonLoot["orns"]
                            ?: 0),
                        floorGold = (currentDungeonVisit?.floorGold ?: 0) + (dungeonLoot["gold"]
                            ?: 0),
                        floorExperience = (currentDungeonVisit?.floorExperience
                            ?: 0) + (dungeonLoot["experience"] ?: 0),
                        // Update totals
                        orns = (currentDungeonVisit?.orns ?: 0) + (dungeonLoot["orns"] ?: 0),
                        gold = (currentDungeonVisit?.gold ?: 0) + (dungeonLoot["gold"] ?: 0),
                        experience = (currentDungeonVisit?.experience
                            ?: 0) + (dungeonLoot["experience"] ?: 0)
                    )

                    Log.d(
                        TAG,
                        "Added dungeon completion rewards - orns: ${dungeonLoot["orns"]}, gold: ${dungeonLoot["gold"]}, exp: ${dungeonLoot["experience"]}"
                    )

                }
            }

            currentDungeonVisit?.let { visit ->
                // Calculate final duration
                val duration = java.time.temporal.ChronoUnit.SECONDS.between(
                    visit.startTime,
                    LocalDateTime.now()
                )

                Log.d(TAG, "=== DUNGEON COMPLETION ===")
                Log.d(TAG, "Final totals for ${visit.name}:")
                Log.d(TAG, "  - Orns: ${visit.orns}")
                Log.d(TAG, "  - Gold: ${visit.gold}")
                Log.d(TAG, "  - Experience: ${visit.experience}")
                Log.d(TAG, "  - Floor rewards: ${visit.floorRewards}")
                Log.d(TAG, "  - Duration: $duration seconds")

                val completedVisit = visit.copy(
                    completed = isComplete,
                    durationSeconds = duration
                )

                Log.d(TAG, "Dungeon completed: $completedVisit")

                // Clear from on-hold visits if it was there
                onHoldVisits.remove(visit.name)

                serviceScope.launch {
                    try {
                        dungeonRepository.updateVisit(completedVisit)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update completed dungeon", e)
                    }
                    updateOverlay()
                }

                // Only clear current visit if the dungeon is actually done
                if (isComplete || visit.floor > 0) {
                    // Clear current visit
                    currentDungeonVisit = null
                    lastDungeonCreationTime = 0L // Reset creation time

                    // Clear the dungeon state tracker
                    dungeonStateTracker.clear()

                    // Reset current dungeon state
                    currentDungeonState = null
                } else {
                    Log.d(TAG, "Not clearing current visit - dungeon might not be done yet")
                }

                // Clean up old entries from recentlyCreatedDungeons (older than 30 seconds)
                val cutoffTime = System.currentTimeMillis() - 30000
                recentlyCreatedDungeons.entries.removeIf { it.value < cutoffTime }

            }

            updateOverlay()
        }
    }

    /**
     * Helper method to check if a specific text appears in the screen data.
     * This method is optimized for performance by using case-insensitive comparison
     * and early termination.
     *
     * @param screenData The list of screen data items to search
     * @param text The text to search for
     * @param ignoreCase Whether to ignore case when comparing (default: true)
     * @return True if the text is found, false otherwise
     */
    private fun containsText(screenData: List<ScreenData>, text: String, ignoreCase: Boolean = true): Boolean {
        return screenData.any { it.text.equals(text, ignoreCase) }
    }

    /**
     * Determines the type of screen based on its content.
     * This method analyzes the text content to identify what kind of screen is currently displayed.
     *
     * @param screenData The list of screen data items to analyze
     * @return The determined screen type
     */
    private fun determineScreenType(screenData: List<ScreenData>): ScreenType {
        val texts = screenData.map { it.text.lowercase() }

        return when {
            texts.any { it.contains("acquired") } -> ScreenType.ITEM_DETAIL
            texts.any { it.contains("new") && texts.any { it.contains("inventory") } } -> ScreenType.INVENTORY
            texts.any { it.contains("notifications") } -> ScreenType.NOTIFICATIONS
            texts.any { it.contains("special dungeon") || it.contains("world dungeon") } -> ScreenType.DUNGEON_ENTRY
            texts.any { it.contains("battle a series of opponents") } -> ScreenType.DUNGEON_ENTRY
            texts.any { it.contains("codex") && it.contains("skill") } -> ScreenType.BATTLE
            else -> ScreenType.UNKNOWN
        }
    }

    /**
     * Determines the screen type from an accessibility node.
     * This is a simplified version of determineScreenType that works with a node instead of ScreenData.
     *
     * @param node The accessibility node to analyze
     * @return The determined screen type
     */
    private fun determineScreenTypeFromAccessibilityTree(node: AccessibilityNodeInfo?): ScreenType {
        if (node == null) return ScreenType.UNKNOWN

        // Extract text from the node and its children
        val textList = mutableListOf<String>()
        extractTextFromNode(node, textList)

        // Convert to lowercase for case-insensitive matching
        val texts = textList.map { it.lowercase() }

        return when {
            texts.any { it.contains("acquired") } -> ScreenType.ITEM_DETAIL
            texts.any { it.contains("new") && texts.any { it.contains("inventory") } } -> ScreenType.INVENTORY
            texts.any { it.contains("notifications") } -> ScreenType.NOTIFICATIONS
            texts.any { it.contains("special dungeon") || it.contains("world dungeon") } -> ScreenType.DUNGEON_ENTRY
            texts.any { it.contains("battle a series of opponents") } -> ScreenType.DUNGEON_ENTRY
            texts.any { it.contains("codex") && it.contains("skill") } -> ScreenType.BATTLE
            else -> ScreenType.UNKNOWN
        }
    }

    /**
     * Extracts text from an accessibility node and its children.
     *
     * @param node The accessibility node to extract text from
     * @param textList The list to add extracted text to
     */
    private fun extractTextFromNode(node: AccessibilityNodeInfo?, textList: MutableList<String>) {
        if (node == null) return

        // Add the node's text if it has any
        val nodeText = node.text?.toString()
        if (!nodeText.isNullOrBlank()) {
            textList.add(nodeText)
        }

        // Add the node's content description if it has any
        val contentDesc = node.contentDescription?.toString()
        if (!contentDesc.isNullOrBlank()) {
            textList.add(contentDesc)
        }

        // Process child nodes
        for (i in 0 until node.childCount) {
            try {
                val child = node.getChild(i) ?: continue
                extractTextFromNode(child, textList)
            } catch (e: Exception) {
                // Ignore errors and continue with other children
            }
        }
    }

    /**
     * Captures a screenshot of the current screen.
     * This method uses the MediaProjection API to capture a screenshot.
     *
     * @return The captured screenshot as a Bitmap, or null if capture failed
     */
    private fun captureScreenshot(): Bitmap? {
        if (!hasScreenCapturePermission || mediaProjection == null) {
            Log.d(TAG, "No screen capture permission or MediaProjection not initialized")
            return null
        }

        try {
            // Get display metrics if not already available
            if (displayMetrics == null) {
                displayMetrics = resources.displayMetrics
            }

            val width = displayMetrics?.widthPixels ?: 1080
            val height = displayMetrics?.heightPixels ?: 1920

            // Create ImageReader for screen capture
            if (imageReader == null) {
                imageReader = ImageReader.newInstance(
                    width, 
                    height, 
                    PixelFormat.RGBA_8888, 
                    2
                )
            }

            // Create virtual display for screen capture
            val virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width,
                height,
                displayMetrics?.densityDpi ?: 320,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                mediaProjectionHandler
            )

            // Capture image
            val image = imageReader?.acquireLatestImage()
            if (image == null) {
                Log.e(TAG, "Failed to acquire image from ImageReader")
                virtualDisplay?.release()
                return null
            }

            // Convert Image to Bitmap
            val bitmap = imageToBitmap(image)

            // Clean up
            image.close()
            virtualDisplay?.release()

            return bitmap

        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screenshot", e)
            return null
        }
    }

    /**
     * Converts an Image to a Bitmap.
     *
     * @param image The Image to convert
     * @return The converted Bitmap
     */
    private fun imageToBitmap(image: Image): Bitmap? {
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            // Create bitmap
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )

            // Copy data to bitmap
            bitmap.copyPixelsFromBuffer(buffer)

            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error converting Image to Bitmap", e)
            return null
        }
    }
}
