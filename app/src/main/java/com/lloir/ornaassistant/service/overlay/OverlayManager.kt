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

    // Overlay views
    private var sessionOverlayView: DraggableOverlayView? = null
    private var invitesOverlayView: DraggableOverlayView? = null
    private var assessOverlayView: DraggableOverlayView? = null

    private val overlayScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    internal var currentTransparency = 0.8f

    companion object {
        private const val TAG = "OverlayManager"
        private const val ORNA_PACKAGE = "playorna.com.orna"

        // Touch constants
        const val TAP_DURATION_MS = 200L
        const val LONG_PRESS_DURATION_MS = 500L
        const val DRAG_THRESHOLD = 10f
    }

    enum class OverlayType { SESSION, INVITES, ASSESSMENT }

    fun setAccessibilityService(service: AccessibilityService) {
        accessibilityServiceRef = WeakReference(service)
        Log.d(TAG, "Accessibility service reference set")
    }

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
                if (itemName != null && isOrnaInForeground()) {
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

    private fun isOrnaInForeground(): Boolean {
        val service = accessibilityServiceRef?.get() ?: return false

        return try {
            val rootNode = service.rootInActiveWindow
            val packageName = rootNode?.packageName?.toString()
            rootNode?.recycle()

            packageName == ORNA_PACKAGE
        } catch (e: Exception) {
            Log.e(TAG, "Error checking foreground app", e)
            true // Assume Orna is in foreground to avoid blocking overlays
        }
    }

    suspend fun handleScreenUpdate(parsedScreen: ParsedScreen) {
        val service = accessibilityServiceRef?.get()
        if (!isInitialized || service == null || !canDrawOverlays() || !isOrnaInForeground()) {
            Log.w(TAG, "Cannot show overlays - not ready or Orna not in foreground")
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

    fun showSessionOverlay(wayvesselSession: WayvesselSession?, dungeonVisit: DungeonVisit?) {
        val service = accessibilityServiceRef?.get() ?: return
        if (!isOrnaInForeground()) return

        if (wayvesselSession == null && dungeonVisit == null) {
            Log.d(TAG, "No session or dungeon to show")
            return
        }

        if (sessionOverlayView == null) {
            sessionOverlayView = DraggableOverlayView(
                service = service,
                type = OverlayType.SESSION,
                transparencyProvider = { currentTransparency },
                onDismiss = { sessionOverlayView = null }
            )
            sessionOverlayView?.show(0, 470, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        sessionOverlayView?.updateSessionContent(wayvesselSession, dungeonVisit)
    }

    fun hideSessionOverlay() {
        sessionOverlayView?.dismiss()
        sessionOverlayView = null
    }

    fun updateSessionOverlay(wayvesselSession: WayvesselSession?, dungeonVisit: DungeonVisit?) {
        if (!isOrnaInForeground()) return

        if (sessionOverlayView == null) {
            showSessionOverlay(wayvesselSession, dungeonVisit)
        } else {
            sessionOverlayView?.updateSessionContent(wayvesselSession, dungeonVisit)
        }
    }

    private fun updateAssessmentOverlay(itemName: String, assessment: AssessmentResult?) {
        val service = accessibilityServiceRef?.get() ?: return
        if (!isOrnaInForeground()) return

        if (assessOverlayView == null) {
            assessOverlayView = DraggableOverlayView(
                service = service,
                type = OverlayType.ASSESSMENT,
                transparencyProvider = { currentTransparency },
                onDismiss = {
                    assessOverlayView = null
                    itemScreenParser.clearCurrentAssessment()
                }
            )
            assessOverlayView?.show(20, 200, 300)
        }

        assessOverlayView?.updateAssessmentContent(itemName, assessment)
    }

    fun hideAssessmentOverlay() {
        assessOverlayView?.dismiss()
        assessOverlayView = null
    }

    private fun showInvitesOverlay(service: AccessibilityService, text: String) {
        if (invitesOverlayView == null) {
            invitesOverlayView = DraggableOverlayView(
                service = service,
                type = OverlayType.INVITES,
                transparencyProvider = { currentTransparency },
                onDismiss = { invitesOverlayView = null }
            )
            invitesOverlayView?.show(5, 5, 600)
        }

        invitesOverlayView?.updateSimpleContent(text)
    }

    fun hideInvitesOverlay() {
        invitesOverlayView?.dismiss()
        invitesOverlayView = null
    }

    fun hideAllOverlays() {
        sessionOverlayView?.dismiss()
        invitesOverlayView?.dismiss()
        assessOverlayView?.dismiss()

        sessionOverlayView = null
        invitesOverlayView = null
        assessOverlayView = null
    }

    fun setOverlayTransparency(transparency: Float) {
        currentTransparency = transparency

        sessionOverlayView?.alpha = transparency
        invitesOverlayView?.alpha = transparency
        assessOverlayView?.alpha = transparency
    }

    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up overlay manager...")
            hideAllOverlays()
            clearAccessibilityService()
            overlayScope.cancel()
            isInitialized = false
            Log.d(TAG, "Overlay manager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}

// Separate class for draggable overlay views
class DraggableOverlayView(
    private val service: AccessibilityService,
    private val type: OverlayManager.OverlayType,
    private val transparencyProvider: () -> Float,
    private val onDismiss: () -> Unit
) : LinearLayout(service) {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var layoutParams: WindowManager.LayoutParams? = null

    // Touch handling
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var touchStartTime = 0L

    // Content views
    private var contentTextView: TextView? = null
    private var titleView: TextView? = null
    private var qualityView: TextView? = null
    private var statsView: TextView? = null
    private var materialsView: TextView? = null

    init {
        setupLayout()
        setupTouchHandling()
    }

    private fun setupLayout() {
        orientation = VERTICAL
        setBackgroundColor(Color.BLACK)
        alpha = transparencyProvider()
        setPadding(16, 16, 16, 16)
        elevation = 10f

        when (type) {
            OverlayManager.OverlayType.SESSION -> {
                contentTextView = TextView(service).apply {
                    setTextColor(Color.WHITE)
                    textSize = 11f
                    setPadding(8, 8, 8, 8)
                    setShadowLayer(4f, 2f, 2f, Color.BLACK)
                }
                addView(contentTextView)
            }
            OverlayManager.OverlayType.INVITES -> {
                contentTextView = TextView(service).apply {
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    setPadding(8, 8, 8, 8)
                    setShadowLayer(4f, 2f, 2f, Color.BLACK)
                }
                addView(contentTextView)
            }
            OverlayManager.OverlayType.ASSESSMENT -> {
                titleView = TextView(service).apply {
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    setPadding(0, 0, 0, 4)
                }
                addView(titleView)

                qualityView = TextView(service).apply {
                    textSize = 11f
                    setPadding(0, 0, 0, 2)
                }
                addView(qualityView)

                statsView = TextView(service).apply {
                    setTextColor(Color.CYAN)
                    textSize = 10f
                    setPadding(0, 0, 0, 2)
                }
                addView(statsView)

                materialsView = TextView(service).apply {
                    setTextColor(Color.LTGRAY)
                    textSize = 10f
                }
                addView(materialsView)
            }
        }
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
                    touchStartTime = System.currentTimeMillis()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    val touchDuration = System.currentTimeMillis() - touchStartTime

                    if (!isDragging &&
                        touchDuration > OverlayManager.LONG_PRESS_DURATION_MS &&
                        (Math.abs(deltaX) > OverlayManager.DRAG_THRESHOLD ||
                                Math.abs(deltaY) > OverlayManager.DRAG_THRESHOLD)) {
                        isDragging = true
                        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        alpha = Math.min(1f, transparencyProvider() + 0.2f)
                    }

                    if (isDragging) {
                        layoutParams?.let { params ->
                            params.x = (initialX + deltaX).toInt()
                            params.y = (initialY + deltaY).toInt()
                            windowManager.updateViewLayout(this, params)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val touchDuration = System.currentTimeMillis() - touchStartTime

                    if (!isDragging && touchDuration < OverlayManager.TAP_DURATION_MS) {
                        dismiss()
                    }

                    if (isDragging) {
                        alpha = transparencyProvider()
                    }

                    isDragging = false
                    true
                }

                else -> false
            }
        }
    }

    fun show(x: Int, y: Int, width: Int) {
        val overlayType = type // Capture type before apply block

        layoutParams = WindowManager.LayoutParams().apply {
            this.width = width
            this.height = WindowManager.LayoutParams.WRAP_CONTENT
            this.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            this.gravity = when (overlayType) {
                OverlayManager.OverlayType.ASSESSMENT -> Gravity.TOP or Gravity.RIGHT
                else -> Gravity.TOP or Gravity.LEFT
            }
            this.format = PixelFormat.TRANSPARENT
            this.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            this.x = x
            this.y = y
        }

        windowManager.addView(this, layoutParams)
        isVisible = true
    }

    fun updateSimpleContent(text: String) {
        contentTextView?.text = text
    }

    fun updateSessionContent(wayvesselSession: WayvesselSession?, dungeonVisit: DungeonVisit?) {
        val displayText = buildString {
            if (wayvesselSession != null) {
                appendLine("@${wayvesselSession.name}")
                if (wayvesselSession.dungeonsVisited > 1) {
                    append("Session: ${formatNumber(wayvesselSession.orns)} orns")
                    if (dungeonVisit?.mode?.type != DungeonMode.Type.ENDLESS) {
                        append(", ${formatNumber(wayvesselSession.gold)} gold")
                    } else {
                        append(", ${formatNumber(wayvesselSession.experience)} exp")
                    }
                    appendLine()
                }
            }
            if (dungeonVisit != null) {
                append("${dungeonVisit.name} ${dungeonVisit.mode}")
                if (dungeonVisit.floor > 0) {
                    append(" Floor ${dungeonVisit.floor}")
                }
                appendLine()
                append("${formatNumber(dungeonVisit.orns)} orns")
                if (dungeonVisit.mode.type == DungeonMode.Type.ENDLESS) {
                    append(", ${formatNumber(dungeonVisit.experience)} exp")
                } else {
                    append(", ${formatNumber(dungeonVisit.gold)} gold")
                }
                if (dungeonVisit.godforges > 0) {
                    append(" [GF: ${dungeonVisit.godforges}]")
                }
            }
        }
        contentTextView?.text = displayText
    }

    fun updateAssessmentContent(itemName: String, assessment: AssessmentResult?) {
        titleView?.text = itemName

        if (assessment != null) {
            val qualityColor = when {
                assessment.quality >= 1.8 -> Color.GREEN
                assessment.quality >= 1.5 -> Color.YELLOW
                else -> Color.WHITE
            }
            qualityView?.apply {
                text = "Quality: ${String.format("%.2f", assessment.quality)}"
                setTextColor(qualityColor)
            }

            if (assessment.stats.isNotEmpty()) {
                val statsText = assessment.stats.mapNotNull { (statName, values) ->
                    if (values.size >= 2) "$statName: ${values[1]}" else null
                }.joinToString("  ")
                statsView?.text = statsText
            } else {
                statsView?.text = ""
            }

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

    fun dismiss() {
        try {
            windowManager.removeView(this)
            onDismiss()
        } catch (e: Exception) {
            Log.w("DraggableOverlay", "Error removing overlay", e)
        }
    }

    private fun formatNumber(value: Long): String {
        return when {
            value >= 1_000_000 -> "%.1f m".format(value / 1_000_000.0)
            value >= 1_000 -> "%.1f k".format(value / 1_000.0)
            else -> value.toString()
        }
    }
}