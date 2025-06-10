package com.lloir.ornaassistant.service.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import com.lloir.ornaassistant.presentation.ui.overlay.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var sessionOverlay: OverlayWindow? = null
    private var invitesOverlay: OverlayWindow? = null
    private var assessOverlay: OverlayWindow? = null

    private var isInitialized = false
    private var initializationJob: Job? = null

    // Keep a weak reference to the accessibility service for context
    private var accessibilityServiceRef: WeakReference<AccessibilityService>? = null

    companion object {
        private const val TAG = "OverlayManager"
        private const val INITIALIZATION_DELAY = 3000L // Increased to 3 seconds
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
        // Cancel any existing initialization
        initializationJob?.cancel()

        initializationJob = managerScope.launch {
            try {
                Log.d(TAG, "Starting overlay initialization with ${INITIALIZATION_DELAY}ms delay...")
                delay(INITIALIZATION_DELAY)

                val service = accessibilityServiceRef?.get()
                if (service == null) {
                    Log.w(TAG, "No accessibility service available for overlay creation")
                    return@launch
                }

                if (!canDrawOverlays()) {
                    Log.w(TAG, "Overlay permission not granted")
                    return@launch
                }

                val settings = settingsRepository.getSettings()
                Log.d(TAG, "Initializing overlays with settings: $settings")

                // Create overlays using the accessibility service context
                if (settings.showSessionOverlay) {
                    sessionOverlay = createSessionOverlay(service)
                    Log.d(TAG, "Session overlay created")
                }

                if (settings.showInvitesOverlay) {
                    invitesOverlay = createInvitesOverlay(service)
                    Log.d(TAG, "Invites overlay created")
                }

                if (settings.showAssessOverlay) {
                    assessOverlay = createAssessOverlay(service)
                    Log.d(TAG, "Assess overlay created")
                }

                isInitialized = true
                Log.i(TAG, "Overlay manager initialized successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize overlay manager", e)
                isInitialized = false

                // Cleanup on failure
                cleanup()
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
        // Don't proceed if not initialized, no service, or can't draw overlays
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
                        invitesOverlay?.updateContent {
                            InvitesOverlayContent(parsedScreen)
                        }
                        invitesOverlay?.show()
                    }
                }
                com.lloir.ornaassistant.domain.model.ScreenType.ITEM_DETAIL -> {
                    if (settings.showAssessOverlay) {
                        assessOverlay?.updateContent {
                            AssessOverlayContent(parsedScreen)
                        }
                        assessOverlay?.show()
                    }
                }
                com.lloir.ornaassistant.domain.model.ScreenType.DUNGEON_ENTRY,
                com.lloir.ornaassistant.domain.model.ScreenType.WAYVESSEL -> {
                    if (settings.showSessionOverlay) {
                        sessionOverlay?.updateContent {
                            SessionOverlayContent(parsedScreen)
                        }
                        sessionOverlay?.show()
                    }
                }
                else -> {
                    // Hide overlays for other screen types if auto-hide is enabled
                    if (settings.autoHideOverlays) {
                        hideAllOverlays()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling screen update", e)
        }
    }

    fun hideAllOverlays() {
        managerScope.launch {
            try {
                sessionOverlay?.hide()
                invitesOverlay?.hide()
                assessOverlay?.hide()
                Log.d(TAG, "All overlays hidden")
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding overlays", e)
            }
        }
    }

    fun cleanup() {
        managerScope.launch {
            try {
                Log.d(TAG, "Cleaning up overlay manager...")
                initializationJob?.cancel()
                sessionOverlay?.remove()
                invitesOverlay?.remove()
                assessOverlay?.remove()
                clearAccessibilityService()
                isInitialized = false
                Log.d(TAG, "Overlay manager cleaned up")
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }

    private suspend fun createSessionOverlay(service: AccessibilityService): OverlayWindow {
        val settings = settingsRepository.getSettings()
        return OverlayWindow(
            context = service, // Use service context instead of application context
            windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager,
            width = (service.resources.displayMetrics.widthPixels * 0.4).toInt(),
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            initialX = 0,
            initialY = 470,
            transparency = settings.overlayTransparency
        )
    }

    private suspend fun createInvitesOverlay(service: AccessibilityService): OverlayWindow {
        val settings = settingsRepository.getSettings()
        return OverlayWindow(
            context = service, // Use service context instead of application context
            windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager,
            width = (service.resources.displayMetrics.widthPixels * 0.8).toInt(),
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            initialX = 5,
            initialY = 5,
            transparency = settings.overlayTransparency
        )
    }

    private suspend fun createAssessOverlay(service: AccessibilityService): OverlayWindow {
        val settings = settingsRepository.getSettings()
        return OverlayWindow(
            context = service, // Use service context instead of application context
            windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager,
            width = (service.resources.displayMetrics.widthPixels * 0.7).toInt(),
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            initialX = 50,
            initialY = 200,
            transparency = settings.overlayTransparency
        )
    }
}

class OverlayWindow(
    private val context: Context,
    private val windowManager: WindowManager,
    private val width: Int,
    private val height: Int,
    initialX: Int,
    initialY: Int,
    private val transparency: Float
) {
    private var composeView: ComposeView? = null
    private var isVisible = false
    private var content: @Composable () -> Unit = {}

    // Use a scope tied to this overlay instance
    private val overlayScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var positionX = initialX
    private var positionY = initialY

    companion object {
        private const val TAG = "OverlayWindow"
    }

    private val windowType: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    } else {
        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
    }

    private val layoutParams = WindowManager.LayoutParams(
        width,
        height,
        windowType,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = positionX
        y = positionY
        alpha = transparency
    }

    fun updateContent(newContent: @Composable () -> Unit) {
        content = newContent
        overlayScope.launch {
            try {
                composeView?.setContent {
                    DraggableOverlay(
                        onMove = { deltaX, deltaY -> moveOverlay(deltaX.toInt(), deltaY.toInt()) },
                        onDismiss = { hide() }
                    ) {
                        content()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating content", e)
            }
        }
    }

    private fun moveOverlay(deltaX: Int, deltaY: Int) {
        overlayScope.launch {
            try {
                positionX += deltaX
                positionY += deltaY
                layoutParams.x = positionX
                layoutParams.y = positionY

                if (isVisible && composeView != null) {
                    windowManager.updateViewLayout(composeView, layoutParams)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update overlay position", e)
            }
        }
    }

    fun show() {
        overlayScope.launch {
            if (isVisible) {
                Log.d(TAG, "Overlay already visible, skipping show")
                return@launch
            }

            try {
                // Verify we can still draw overlays
                val canDraw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else {
                    true
                }

                if (!canDraw) {
                    Log.w(TAG, "Cannot draw overlays - permission not granted")
                    return@launch
                }

                // Always create a fresh ComposeView to avoid token issues
                composeView?.let { existingView ->
                    try {
                        if (isVisible) {
                            windowManager.removeView(existingView)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error removing existing view", e)
                    }
                }

                composeView = ComposeView(context).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                    setContent {
                        DraggableOverlay(
                            onMove = { deltaX, deltaY -> moveOverlay(deltaX.toInt(), deltaY.toInt()) },
                            onDismiss = { hide() }
                        ) {
                            content()
                        }
                    }
                }

                layoutParams.x = positionX
                layoutParams.y = positionY

                windowManager.addView(composeView, layoutParams)
                isVisible = true
                Log.d(TAG, "Overlay shown successfully at ($positionX, $positionY)")

            } catch (e: WindowManager.BadTokenException) {
                Log.e(TAG, "Failed to show overlay - bad token. Context: ${context.javaClass.simpleName}", e)
                isVisible = false
                composeView = null
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to show overlay - security exception", e)
                isVisible = false
                composeView = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show overlay - unexpected error", e)
                isVisible = false
                composeView = null
            }
        }
    }

    fun hide() {
        overlayScope.launch {
            if (!isVisible || composeView == null) return@launch

            try {
                windowManager.removeView(composeView)
                isVisible = false
                Log.d(TAG, "Overlay hidden successfully")
            } catch (e: IllegalArgumentException) {
                Log.d(TAG, "View already removed or not attached")
                isVisible = false
            } catch (e: Exception) {
                Log.w(TAG, "Error hiding overlay", e)
                isVisible = false
            }
        }
    }

    fun remove() {
        overlayScope.launch {
            try {
                hide()
                composeView = null
                overlayScope.cancel() // Cancel the coroutine scope
                Log.d(TAG, "Overlay removed and scope cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay", e)
            }
        }
    }
}