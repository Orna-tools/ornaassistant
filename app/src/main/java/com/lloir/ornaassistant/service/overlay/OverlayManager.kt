package com.lloir.ornaassistant.service.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import com.lloir.ornaassistant.service.overlay.AssessmentOverlayData
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.model.DungeonMode
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.DungeonVisit
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import com.lloir.ornaassistant.service.parser.impl.ItemScreenParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val itemScreenParser: ItemScreenParser,
    private val dungeonScreenParser: com.lloir.ornaassistant.service.parser.impl.DungeonScreenParser
) {
    private var accessibilityServiceRef: WeakReference<AccessibilityService>? = null
    private var isInitialized = false

    // Overlay views
    private var assessOverlayView: DraggableAssessmentOverlay? = null
    private var dungeonOverlayView: DraggableDungeonOverlay? = null

    private val overlayScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Cache for recent assessments to avoid repeated API calls
    private val assessmentCache = mutableMapOf<String, CachedAssessment>()
    private val cacheExpiryMs = 30000L // 30 seconds

    internal var currentTransparency = 0.8f

    companion object {
        private const val TAG = "OverlayManager"
        private const val ORNA_PACKAGE = "playorna.com.orna"
    }

    data class CachedAssessment(
        val result: AssessmentResult,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > 30000L
    }

    /**
     * Check if Orna is the currently active application
     * 
     * @return true if Orna is active, false otherwise
     */
    private fun isOrnaActive(): Boolean {
        val service = accessibilityServiceRef?.get() ?: run {
            Log.d(TAG, "Cannot check if Orna is active - no accessibility service reference")
            return false
        }

        return try {
            val rootNode = service.rootInActiveWindow ?: run {
                Log.d(TAG, "Cannot check if Orna is active - no active window")
                return false
            }

            try {
                val packageName = rootNode.packageName?.toString()
                packageName == ORNA_PACKAGE
            } finally {
                // Always recycle the node to prevent memory leaks
                rootNode.recycle()
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Error checking active package - accessibility service may be disconnected", e)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "Security error checking active package - missing permissions", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error checking active package", e)
            false
        }
    }

    /**
     * Set the accessibility service reference - called from the service when connected
     */
    fun setAccessibilityService(service: AccessibilityService) {
        accessibilityServiceRef = WeakReference(service)
        Log.d(TAG, "Accessibility service reference set")
    }

    /**
     * Clear the accessibility service reference - called when service disconnects
     */
    fun clearAccessibilityService() {
        accessibilityServiceRef?.clear()
        accessibilityServiceRef = null
        Log.d(TAG, "Accessibility service reference cleared")
    }

    /**
     * Initialize the overlay manager
     * This sets up observers for assessment and dungeon data
     */
    suspend fun initialize() {
        // First check if we're already initialized
        if (isInitialized) {
            Log.d(TAG, "Overlay manager already initialized")
            return
        }

        try {
            // Check for accessibility service
            val service = accessibilityServiceRef?.get()
            if (service == null) {
                Log.w(TAG, "No accessibility service available for overlay creation")
                isInitialized = false
                return
            }

            // Check for overlay permission
            if (!canDrawOverlays()) {
                Log.w(TAG, "Overlay permission not granted - overlays will not be shown")
                isInitialized = false
                return
            }

            Log.d(TAG, "Initializing overlay manager...")

            try {
                // Start observing updates
                startAssessmentObserver()
                startDungeonObserver()

                isInitialized = true
                Log.i(TAG, "Overlay manager initialized successfully")
            } catch (e: CancellationException) {
                // Coroutine was cancelled - this is expected during cleanup
                Log.d(TAG, "Initialization cancelled")
                isInitialized = false
                throw e  // Re-throw to properly cancel the coroutine
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start observers", e)
                isInitialized = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize overlay manager", e)
            isInitialized = false
        }
    }

    private fun startAssessmentObserver() {
        overlayScope.launch {
            combine(
                itemScreenParser.currentItemName,
                itemScreenParser.currentAssessment
            ) { itemName, assessment ->
                Pair(itemName, assessment)
            }.collect { (itemName, assessment) ->
                if (itemName != null) {
                    updateAssessmentOverlay(itemName, assessment)
                } else {
                    hideAssessmentOverlay()
                }
            }
        }
    }

    private fun startDungeonObserver() {
        overlayScope.launch {
            dungeonScreenParser.currentDungeonVisit.collect { dungeonVisit ->
                if (dungeonVisit != null) {
                    updateDungeonOverlay(dungeonVisit)
                } else {
                    hideDungeonOverlay()
                }
            }
        }
    }

    /**
     * Check if the app has permission to draw overlays
     * 
     * @return true if overlay permission is granted, false otherwise
     */
    private fun canDrawOverlays(): Boolean {
        val service = accessibilityServiceRef?.get() ?: run {
            Log.d(TAG, "Cannot check overlay permission - no accessibility service reference")
            return false
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val hasPermission = Settings.canDrawOverlays(service)
                if (!hasPermission) {
                    Log.w(TAG, "Overlay permission not granted. User needs to enable it in settings.")
                }
                hasPermission
            } else {
                // Permission is implicitly granted on older Android versions
                true
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error checking overlay permission", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error checking overlay permission", e)
            false
        }
    }

    suspend fun handleScreenUpdate(parsedScreen: ParsedScreen) {
        val service = accessibilityServiceRef?.get()
        if (!isInitialized || service == null || !canDrawOverlays()) {
            Log.w(TAG, "Cannot show overlays - not ready (initialized: $isInitialized, service: ${service != null})")
            return
        }

        // Only show overlays when Orna is active
        if (!isOrnaActive()) {
            Log.d(TAG, "Orna is not active, not showing overlays")
            hideAllOverlays()
            return
        }

        try {
            val settings = settingsRepository.getSettings()

            when (parsedScreen.screenType) {
                com.lloir.ornaassistant.domain.model.ScreenType.ITEM_DETAIL -> {
                    // Assessment overlay is handled by the observer, not here
                    // This prevents constant recreation
                }
                else -> {
                    if (settings.autoHideOverlays) {
                        hideAllOverlays()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling screen update", e)
        }
    }

    private fun updateAssessmentOverlay(itemName: String, assessment: AssessmentResult?, adornmentWarning: com.lloir.ornaassistant.service.parser.impl.ItemScreenParser.AdornmentWarning? = null) {
        val service = accessibilityServiceRef?.get() ?: return

        // Only show when Orna is active
        if (!isOrnaActive()) {
            Log.d(TAG, "Orna is not active, not showing assessment overlay")
            return
        }

        // Check if assessment overlay is enabled in settings
        val settings = runBlocking { settingsRepository.getSettings() }
        if (!settings.showAssessOverlay) {
            Log.d(TAG, "Assessment overlay is disabled in settings, not showing")
            hideAssessmentOverlay() // Hide if it's currently showing
            return
        }

        try {
            // Removed adornment warning handling

            if (assessOverlayView == null) {
                // Create new overlay if it doesn't exist
                assessOverlayView = createAssessmentOverlay(service, itemName, assessment)
                Log.d(TAG, "Created new assessment overlay")
            } else {
                // Just update the existing overlay content
                assessOverlayView?.updateContent(AssessmentOverlayData(itemName, assessment))
                Log.d(TAG, "Updated existing assessment overlay")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating assessment overlay", e)
        }
    }

    fun hideAssessmentOverlay() {
        assessOverlayView?.dismiss()
        assessOverlayView = null
        Log.d(TAG, "Assessment overlay hidden")
    }

    private fun createAssessmentOverlay(
        service: AccessibilityService,
        itemName: String,
        assessment: AssessmentResult?
    ): DraggableAssessmentOverlay? {
        try {
            val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val overlay = DraggableAssessmentOverlay(service, windowManager, settingsRepository)
            overlay.create()
            overlay.updateContent(AssessmentOverlayData(itemName, assessment))
            overlay.updateTransparency(currentTransparency)
            return overlay
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create draggable assessment overlay", e)
            return null
        }
    }

    private fun updateDungeonOverlay(dungeonVisit: DungeonVisit) {
        val service = accessibilityServiceRef?.get() ?: return

        // Only show when Orna is active
        if (!isOrnaActive()) {
            Log.d(TAG, "Orna is not active, not showing dungeon overlay")
            return
        }

        try {
            if (dungeonOverlayView == null) {
                // Create new overlay if it doesn't exist
                dungeonOverlayView = createDungeonOverlay(service, dungeonVisit)
                Log.d(TAG, "Created new dungeon overlay")
            } else {
                // Just update the existing overlay content
                dungeonOverlayView?.updateContent(DungeonOverlayData(dungeonVisit, dungeonVisit.floor))
                Log.d(TAG, "Updated existing dungeon overlay")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating dungeon overlay", e)
        }
    }

    fun hideDungeonOverlay() {
        dungeonOverlayView?.dismiss()
        dungeonOverlayView = null
        Log.d(TAG, "Dungeon overlay hidden")
    }

    private fun createDungeonOverlay(
        service: AccessibilityService,
        dungeonVisit: DungeonVisit
    ): DraggableDungeonOverlay? {
        try {
            val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val overlay = DraggableDungeonOverlay(service, windowManager)
            overlay.create()
            overlay.updateContent(DungeonOverlayData(dungeonVisit, dungeonVisit.floor))
            overlay.updateTransparency(currentTransparency)
            return overlay
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create draggable dungeon overlay", e)
            return null
        }
    }

    fun hideAllOverlays() {
        try {
            assessOverlayView?.dismiss()
            assessOverlayView = null

            dungeonOverlayView?.dismiss()
            dungeonOverlayView = null

            // Clear assessment cache periodically
            cleanupAssessmentCache()

            Log.d(TAG, "All overlays hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding overlays", e)
        }
    }

    private fun cleanupAssessmentCache() {
        val iterator = assessmentCache.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.isExpired()) {
                iterator.remove()
            }
        }
    }

    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up overlay manager...")

            // Cancel all coroutines first to prevent new overlays from being created
            overlayScope.cancel("OverlayManager being cleaned up")

            // Hide and clear all overlays
            hideAllOverlays()

            // Clear service reference
            clearAccessibilityService()

            // Clear all caches and state
            assessmentCache.clear()
            isInitialized = false

            // Ensure garbage collection can reclaim resources
            System.gc()

            Log.d(TAG, "Overlay manager cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    fun setOverlayTransparency(transparency: Float) {
        currentTransparency = transparency

        // Update existing overlays
        assessOverlayView?.updateTransparency(transparency)
        dungeonOverlayView?.updateTransparency(transparency)

        Log.d(TAG, "Overlay transparency updated to: $transparency")
    }
}
