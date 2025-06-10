package com.lloir.ornaassistant.service.overlay

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
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var sessionOverlay: OverlayWindow? = null
    private var invitesOverlay: OverlayWindow? = null
    private var assessOverlay: OverlayWindow? = null

    companion object {
        private const val TAG = "OverlayManager"
    }

    suspend fun initialize() {
        // Check if we have overlay permission
        if (!canDrawOverlays()) {
            Log.w(TAG, "Overlay permission not granted")
            return
        }

        val settings = settingsRepository.getSettings()

        if (settings.showSessionOverlay) {
            sessionOverlay = createSessionOverlay()
        }

        if (settings.showInvitesOverlay) {
            invitesOverlay = createInvitesOverlay()
        }

        if (settings.showAssessOverlay) {
            assessOverlay = createAssessOverlay()
        }
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    suspend fun handleScreenUpdate(parsedScreen: ParsedScreen) {
        if (!canDrawOverlays()) {
            Log.w(TAG, "Cannot show overlays - permission not granted")
            return
        }

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
    }

    fun hideAllOverlays() {
        sessionOverlay?.hide()
        invitesOverlay?.hide()
        assessOverlay?.hide()
    }

    fun cleanup() {
        sessionOverlay?.remove()
        invitesOverlay?.remove()
        assessOverlay?.remove()
    }

    private suspend fun createSessionOverlay(): OverlayWindow {
        val settings = settingsRepository.getSettings()
        return OverlayWindow(
            context = context,
            windowManager = windowManager,
            width = (context.resources.displayMetrics.widthPixels * 0.4).toInt(),
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            initialX = 0,
            initialY = 470,
            transparency = settings.overlayTransparency
        )
    }

    private suspend fun createInvitesOverlay(): OverlayWindow {
        val settings = settingsRepository.getSettings()
        return OverlayWindow(
            context = context,
            windowManager = windowManager,
            width = (context.resources.displayMetrics.widthPixels * 0.8).toInt(),
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            initialX = 5,
            initialY = 5,
            transparency = settings.overlayTransparency
        )
    }

    private suspend fun createAssessOverlay(): OverlayWindow {
        val settings = settingsRepository.getSettings()
        return OverlayWindow(
            context = context,
            windowManager = windowManager,
            width = (context.resources.displayMetrics.widthPixels * 0.7).toInt(),
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

    private var positionX = initialX
    private var positionY = initialY

    companion object {
        private const val TAG = "OverlayWindow"
    }

    private val layoutParams = WindowManager.LayoutParams(
        width,
        height,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
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
        composeView?.setContent {
            DraggableOverlay(
                onMove = { deltaX, deltaY -> moveOverlay(deltaX.toInt(), deltaY.toInt()) },
                onDismiss = { hide() }
            ) {
                content()
            }
        }
    }

    private fun moveOverlay(deltaX: Int, deltaY: Int) {
        positionX += deltaX
        positionY += deltaY
        layoutParams.x = positionX
        layoutParams.y = positionY

        if (isVisible && composeView != null) {
            try {
                windowManager.updateViewLayout(composeView, layoutParams)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update overlay position", e)
            }
        }
    }

    fun show() {
        if (isVisible) {
            Log.d(TAG, "Overlay already visible, skipping show")
            return
        }

        try {
            if (composeView == null) {
                composeView = ComposeView(context).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                    setContent {
                        DraggableOverlay(
                            onMove = { deltaX, deltaY -> moveOverlay(deltaX.toInt(), deltaY.toInt()) },
                            onDismiss = { hide() }
                        ) {
                            content()
                        }
                    }
                }
            }

            layoutParams.x = positionX
            layoutParams.y = positionY

            windowManager.addView(composeView, layoutParams)
            isVisible = true
            Log.d(TAG, "Overlay shown successfully")

        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "Failed to show overlay - bad token (permission issue?)", e)
            isVisible = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
            isVisible = false
        }
    }

    fun hide() {
        if (!isVisible || composeView == null) return

        try {
            windowManager.removeView(composeView)
            isVisible = false
            Log.d(TAG, "Overlay hidden successfully")
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "View already removed")
            isVisible = false
        } catch (e: Exception) {
            Log.w(TAG, "Error hiding overlay", e)
            isVisible = false
        }
    }

    fun remove() {
        hide()
        composeView = null
    }
}
