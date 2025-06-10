package com.lloir.ornaassistant.service.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private var accessibilityServiceRef: WeakReference<AccessibilityService>? = null
    private var isInitialized = false

    // Simple overlay views - NO COMPOSE
    private var sessionOverlayView: LinearLayout? = null
    private var invitesOverlayView: LinearLayout? = null
    private var assessOverlayView: LinearLayout? = null

    companion object {
        private const val TAG = "OverlayManager"
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

            Log.d(TAG, "Initializing simple overlays...")
            isInitialized = true
            Log.i(TAG, "Overlay manager initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize overlay manager", e)
            isInitialized = false
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
                    if (settings.showAssessOverlay) {
                        showAssessOverlay(service, "Item Assessment")
                    }
                }
                com.lloir.ornaassistant.domain.model.ScreenType.DUNGEON_ENTRY,
                com.lloir.ornaassistant.domain.model.ScreenType.WAYVESSEL -> {
                    if (settings.showSessionOverlay) {
                        showSessionOverlay(service, "Session Stats")
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

    private fun showSessionOverlay(service: AccessibilityService, text: String) {
        try {
            // Remove existing overlay if any
            removeOverlay(service, sessionOverlayView)

            // Create simple overlay using traditional Android views
            sessionOverlayView = createSimpleOverlay(service, text, 0, 470, 400, 100)
            Log.d(TAG, "Session overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing session overlay", e)
        }
    }

    private fun showInvitesOverlay(service: AccessibilityService, text: String) {
        try {
            // Remove existing overlay if any
            removeOverlay(service, invitesOverlayView)

            // Create simple overlay using traditional Android views
            invitesOverlayView = createSimpleOverlay(service, text, 5, 5, 600, 150)
            Log.d(TAG, "Invites overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing invites overlay", e)
        }
    }

    private fun showAssessOverlay(service: AccessibilityService, text: String) {
        try {
            // Remove existing overlay if any
            removeOverlay(service, assessOverlayView)

            // Create simple overlay using traditional Android views
            assessOverlayView = createSimpleOverlay(service, text, 50, 200, 500, 120)
            Log.d(TAG, "Assess overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing assess overlay", e)
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

            // Create LinearLayout (like your old working code)
            val layout = LinearLayout(service)
            layout.orientation = LinearLayout.VERTICAL
            layout.setBackgroundColor(Color.BLACK)
            layout.alpha = 0.8f

            // Create TextView (like your old working code)
            val textView = TextView(service)
            textView.text = text
            textView.setTextColor(Color.WHITE)
            textView.setPadding(16, 16, 16, 16)
            textView.textSize = 14f

            layout.addView(textView)

            // Create layout parameters exactly like your old working code
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.width = width
            layoutParams.height = height
            layoutParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            layoutParams.gravity = Gravity.TOP or Gravity.LEFT
            layoutParams.format = PixelFormat.TRANSPARENT
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            layoutParams.x = x
            layoutParams.y = y

            // Add to window manager
            windowManager.addView(layout, layoutParams)
            layout.isVisible = true

            Log.d(TAG, "Simple overlay created successfully at ($x, $y) with size ($width, $height)")
            return layout

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create simple overlay", e)
            return null
        }
    }

    private fun removeOverlay(service: AccessibilityService, overlay: LinearLayout?) {
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

            Log.d(TAG, "All overlays hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding overlays", e)
        }
    }

    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up overlay manager...")
            hideAllOverlays()
            clearAccessibilityService()
            isInitialized = false
            Log.d(TAG, "Overlay manager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}