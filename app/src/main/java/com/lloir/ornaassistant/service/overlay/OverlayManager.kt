package com.lloir.ornaassistant.service.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.domain.model.DungeonMode
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.DungeonVisit
import com.lloir.ornaassistant.domain.model.WayvesselSession
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import com.lloir.ornaassistant.service.parser.impl.ItemScreenParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val itemScreenParser: ItemScreenParser
) {
    private var accessibilityServiceRef: WeakReference<AccessibilityService>? = null
    private var isInitialized = false

    // Simple overlay views - NO COMPOSE
    private var sessionOverlayView: LinearLayout? = null
    private var invitesOverlayView: LinearLayout? = null
    private var assessOverlayView: DraggableAssessmentOverlay? = null

    private val overlayScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Cache for recent assessments to avoid repeated API calls
    private val assessmentCache = mutableMapOf<String, CachedAssessment>()
    private val cacheExpiryMs = 30000L // 30 seconds

    companion object {
        private const val TAG = "OverlayManager"
    }

    data class CachedAssessment(
        val result: AssessmentResult,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > 30000L
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

    suspend fun initialize() {
        try {
            val service = accessibilityServiceRef?.get()
            if (service == null) {
                Log.w(TAG, "No accessibility service available for overlay creation")
                return
            }

            if (!canDrawOverlays()) {
                Log.w(TAG, "Overlay permission not granted")
                return
            }

            Log.d(TAG, "Initializing overlay manager...")

            // Start observing assessment updates
            startAssessmentObserver()

            isInitialized = true
            Log.i(TAG, "Overlay manager initialized successfully")

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

    private fun canDrawOverlays(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking overlay permission", e)
            false
        }
    }

    suspend fun handleScreenUpdate(parsedScreen: ParsedScreen) {
        val service = accessibilityServiceRef?.get()
        if (!isInitialized || service == null || !canDrawOverlays()) {
            Log.w(TAG, "Cannot show overlays - not ready (initialized: $isInitialized, service: ${service != null})")
            return
        }

        try {
            val settings = settingsRepository.getSettings()

            when (parsedScreen.screenType) {
                com.lloir.ornaassistant.domain.model.ScreenType.NOTIFICATIONS -> {
                    if (settings.showInvitesOverlay) {
                        showInvitesOverlay(service, "Party Invites")
                    }
                }
                com.lloir.ornaassistant.domain.model.ScreenType.ITEM_DETAIL -> {
                    // Assessment overlay is handled by the observer, not here
                    // This prevents constant recreation
                }
                com.lloir.ornaassistant.domain.model.ScreenType.DUNGEON_ENTRY,
                com.lloir.ornaassistant.domain.model.ScreenType.WAYVESSEL -> {
                    if (settings.showSessionOverlay) {
                        // Session overlay should be handled by the accessibility service with actual data
                        // This is just a placeholder
                    }
                }
                else -> {
                    if (settings.autoHideOverlays) {
                        hideAllOverlays(service)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling screen update", e)
        }
    }
    
    fun showSessionOverlay(wayvesselSession: WayvesselSession?, dungeonVisit: DungeonVisit?) {
        val service = accessibilityServiceRef?.get() ?: return
        showSessionOverlay(service, wayvesselSession, dungeonVisit)
    }

    private fun updateAssessmentOverlay(itemName: String, assessment: AssessmentResult?) {
        val service = accessibilityServiceRef?.get() ?: return

        try {
            if (assessOverlayView == null) {
                // Create new overlay if it doesn't exist
                assessOverlayView = createDraggableAssessmentOverlay(service, itemName, assessment)
                Log.d(TAG, "Created new assessment overlay")
            } else {
                // Just update the existing overlay content
                assessOverlayView?.updateContent(itemName, assessment)
                Log.d(TAG, "Updated existing assessment overlay")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating assessment overlay", e)
        }
    }

    // hideAssessmentOverlay is now public (moved above)

    private fun showSessionOverlay(service: AccessibilityService, wayvesselSession: WayvesselSession?, dungeonVisit: DungeonVisit?) {
        try {
            if (sessionOverlayView != null) return // Don't recreate if exists
            
            val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            val layout = LinearLayout(service)
            layout.orientation = LinearLayout.VERTICAL
            layout.setBackgroundColor(Color.BLACK)
            layout.alpha = 0.8f
            layout.setPadding(16, 16, 16, 16)
            
            // Build session display text
            val textView = TextView(service)
            val displayText = buildString {
                if (wayvesselSession != null) {
                    appendLine("@${wayvesselSession.name}")
                    if (wayvesselSession.dungeonsVisited > 1) {
                        appendLine("Session: ${formatNumber(wayvesselSession.orns)} orns, ${formatNumber(wayvesselSession.gold)} gold")
                    }
                }
                if (dungeonVisit != null) {
                    appendLine("${dungeonVisit.name} ${dungeonVisit.mode}")
                    append("${formatNumber(dungeonVisit.orns)} orns, ")
                    if (dungeonVisit.mode.type == DungeonMode.Type.ENDLESS) {
                        append("${formatNumber(dungeonVisit.experience)} exp")
                    } else {
                        append("${formatNumber(dungeonVisit.gold)} gold")
                    }
                }
            }
            
            textView.text = displayText
            textView.setTextColor(Color.WHITE)
            textView.textSize = 12f
            
            layout.addView(textView)
            
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.width = 400
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            layoutParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            layoutParams.gravity = Gravity.TOP or Gravity.LEFT
            layoutParams.format = PixelFormat.TRANSPARENT
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            layoutParams.x = 0
            layoutParams.y = 470
            
            windowManager.addView(layout, layoutParams)
            layout.isVisible = true
            
            sessionOverlayView = layout
            Log.d(TAG, "Session overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing session overlay", e)
        }
    }

    fun hideSessionOverlay() {
        val service = accessibilityServiceRef?.get() ?: return
        removeOverlay(service, sessionOverlayView)
        sessionOverlayView = null
        Log.d(TAG, "Session overlay hidden")
    }

    fun updateSessionOverlay(wayvesselSession: WayvesselSession?, dungeonVisit: DungeonVisit?) {
        val service = accessibilityServiceRef?.get() ?: return
        
        if (sessionOverlayView == null) {
            // Create new overlay if it doesn't exist
            showSessionOverlay(wayvesselSession, dungeonVisit)
            return
        }
        
        // Update existing overlay
        try {
            val textView = sessionOverlayView?.getChildAt(0) as? TextView ?: return
            
            val displayText = buildString {
                if (wayvesselSession != null) {
                    appendLine("@${wayvesselSession.name}")
                    if (wayvesselSession.dungeonsVisited > 1) {
                        appendLine("Session: ${formatNumber(wayvesselSession.orns)} orns, ${formatNumber(wayvesselSession.gold)} gold")
                    }
                }
                if (dungeonVisit != null) {
                    appendLine("${dungeonVisit.name} ${dungeonVisit.mode}")
                    append("${formatNumber(dungeonVisit.orns)} orns, ")
                    if (dungeonVisit.mode.type == com.lloir.ornaassistant.domain.model.DungeonMode.Type.ENDLESS) {
                        append("${formatNumber(dungeonVisit.experience)} exp")
                    } else {
                        append("${formatNumber(dungeonVisit.gold)} gold")
                    }
                }
            }
            
            textView.text = displayText
            Log.d(TAG, "Session overlay updated")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating session overlay", e)
        }
    }

    fun hideInvitesOverlay() {
        val service = accessibilityServiceRef?.get() ?: return
        removeOverlay(service, invitesOverlayView)
        invitesOverlayView = null
        Log.d(TAG, "Invites overlay hidden")
    }

    fun hideAssessmentOverlay() {
        val service = accessibilityServiceRef?.get() ?: return
        removeOverlay(service, assessOverlayView)
        assessOverlayView = null
        Log.d(TAG, "Assessment overlay hidden")
    }

    private fun showInvitesOverlay(service: AccessibilityService, text: String) {
        try {
            if (invitesOverlayView != null) return // Don't recreate if exists
            invitesOverlayView = createSimpleOverlay(service, text, 5, 5, 600, 150)
            Log.d(TAG, "Invites overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing invites overlay", e)
        }
    }

    private fun createSimpleOverlay(
        service: AccessibilityService,
        text: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): LinearLayout? {
        try {
            val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layout = LinearLayout(service)
            layout.orientation = LinearLayout.VERTICAL
            layout.setBackgroundColor(Color.BLACK)
            layout.alpha = 0.8f

            val textView = TextView(service)
            textView.text = text
            textView.setTextColor(Color.WHITE)
            textView.setPadding(16, 16, 16, 16)
            textView.textSize = 14f

            layout.addView(textView)

            val layoutParams = WindowManager.LayoutParams()
            layoutParams.width = width
            layoutParams.height = height
            layoutParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            layoutParams.gravity = Gravity.TOP or Gravity.LEFT
            layoutParams.format = PixelFormat.TRANSPARENT
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            layoutParams.x = x
            layoutParams.y = y

            windowManager.addView(layout, layoutParams)
            layout.isVisible = true

            return layout

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create simple overlay", e)
            return null
        }
    }

    private fun createDraggableAssessmentOverlay(
        service: AccessibilityService,
        itemName: String,
        assessment: AssessmentResult?
    ): DraggableAssessmentOverlay? {
        try {
            val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val overlay = DraggableAssessmentOverlay(service, windowManager)
            overlay.create(itemName, assessment)
            return overlay
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create draggable assessment overlay", e)
            return null
        }
    }

    private fun removeOverlay(service: AccessibilityService, overlay: View?) {
        if (overlay != null) {
            try {
                val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager.removeView(overlay)
                Log.d(TAG, "Overlay removed successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Error removing overlay", e)
            }
        }
    }

    private fun formatNumber(value: Long): String {
        return when {
            value >= 1_000_000 -> "%.1fm".format(value / 1_000_000.0)
            value >= 1_000 -> "%.1fk".format(value / 1_000.0)
            else -> value.toString()
        }
    }

    fun hideAllOverlays() {
        val service = accessibilityServiceRef?.get()
        if (service != null) {
            hideAllOverlays(service)
        }
    }

    private fun hideAllOverlays(service: AccessibilityService) {
        try {
            removeOverlay(service, sessionOverlayView)
            removeOverlay(service, invitesOverlayView)
            removeOverlay(service, assessOverlayView)

            sessionOverlayView = null
            invitesOverlayView = null
            assessOverlayView = null

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
            hideAllOverlays()
            clearAccessibilityService()
            overlayScope.cancel()
            assessmentCache.clear()
            isInitialized = false
            Log.d(TAG, "Overlay manager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}

class DraggableAssessmentOverlay(
    private val service: AccessibilityService,
    private val windowManager: WindowManager
) : LinearLayout(service) {

    private var layoutParams: WindowManager.LayoutParams? = null
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var clickStartTime = 0L

    private var titleView: TextView? = null
    private var qualityView: TextView? = null
    private var statsView: TextView? = null
    private var materialsView: TextView? = null

    companion object {
        private const val CLICK_DURATION_MS = 200L
        private const val DRAG_THRESHOLD = 10f
        private const val TAG = "DraggableOverlay"
    }

    fun create(itemName: String, assessment: AssessmentResult?) {
        setupLayout()
        setupTouchHandling()
        updateContent(itemName, assessment)
        addToWindow()
    }

    private fun setupLayout() {
        orientation = VERTICAL
        setBackgroundColor(Color.BLACK)
        alpha = 0.9f
        setPadding(12, 8, 12, 8)

        // Title
        titleView = TextView(service).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(0, 0, 0, 4)
        }
        addView(titleView)

        // Quality
        qualityView = TextView(service).apply {
            textSize = 11f
            setPadding(0, 0, 0, 2)
        }
        addView(qualityView)

        // Stats
        statsView = TextView(service).apply {
            setTextColor(Color.CYAN)
            textSize = 10f
            setPadding(0, 0, 0, 2)
        }
        addView(statsView)

        // Materials
        materialsView = TextView(service).apply {
            setTextColor(Color.LTGRAY)
            textSize = 10f
        }
        addView(materialsView)
    }

    private fun setupTouchHandling() {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x?.toFloat() ?: 0f
                    initialY = layoutParams?.y?.toFloat() ?: 0f
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    clickStartTime = System.currentTimeMillis()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY

                    if (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD) {
                        isDragging = true

                        layoutParams?.let { params ->
                            params.x = (initialX + deltaX).toInt()
                            params.y = (initialY + deltaY).toInt()
                            windowManager.updateViewLayout(this, params)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val clickDuration = System.currentTimeMillis() - clickStartTime

                    if (!isDragging && clickDuration < CLICK_DURATION_MS) {
                        // This was a tap - close the overlay
                        Log.d(TAG, "Tap detected - closing overlay")
                        closeOverlay()
                    }

                    isDragging = false
                    true
                }

                else -> false
            }
        }
    }

    fun updateContent(itemName: String, assessment: AssessmentResult?) {
        titleView?.text = itemName

        if (assessment != null) {
            // Quality with color coding
            val qualityColor = when {
                assessment.quality >= 1.8 -> Color.GREEN
                assessment.quality >= 1.5 -> Color.YELLOW
                else -> Color.WHITE
            }
            qualityView?.apply {
                text = "Quality: ${String.format("%.2f", assessment.quality)}"
                setTextColor(qualityColor)
            }

            // Stats - show current values
            if (assessment.stats.isNotEmpty()) {
                val statsText = assessment.stats.mapNotNull { (statName, values) ->
                    if (values.size >= 2) "$statName: ${values[1]}" else null
                }.joinToString("  ")

                statsView?.text = statsText
            } else {
                statsView?.text = ""
            }

            // Materials
            if (assessment.materials.size >= 3) {
                materialsView?.text = "MF: ${assessment.materials[1]} | DF: ${assessment.materials[2]}"
            } else {
                materialsView?.text = ""
            }
        } else {
            qualityView?.apply {
                text = "Assessing..."
                setTextColor(Color.YELLOW)
            }
            statsView?.text = ""
            materialsView?.text = ""
        }
    }

    private fun addToWindow() {
        layoutParams = WindowManager.LayoutParams().apply {
            width = 300
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.TOP or Gravity.RIGHT
            format = PixelFormat.TRANSPARENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            x = 20
            y = 200
        }

        windowManager.addView(this, layoutParams)
        isVisible = true
    }

    private fun closeOverlay() {
        try {
            windowManager.removeView(this)
        } catch (e: Exception) {
            // Overlay might already be removed
            Log.w(TAG, "Error removing overlay", e)
        }
    }
}