package com.lloir.ornaassistant.service.overlay

import android.content.Context
import android.graphics.PixelFormat
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

    suspend fun initialize() {
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

    suspend fun handleScreenUpdate(parsedScreen: ParsedScreen) {
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
            x = 0,
            y = 470,
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
            x = 5,
            y = 5,
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
            x = 50,
            y = 200,
            transparency = settings.overlayTransparency
        )
    }
}

class OverlayWindow(
    private val context: Context,
    private val windowManager: WindowManager,
    private val width: Int,
    private val height: Int,
    private var x: Int,
    private var y: Int,
    private val transparency: Float
) {
    private var composeView: ComposeView? = null
    private var isVisible = false
    private var content: @Composable () -> Unit = { }

    private val layoutParams = WindowManager.LayoutParams().apply {
        this.width = this@OverlayWindow.width
        this.height = this@OverlayWindow.height
        this.x = this@OverlayWindow.x
        this.y = this@OverlayWindow.y
        type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        gravity = Gravity.TOP or Gravity.START
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        alpha = transparency
    }

    fun updateContent(newContent: @Composable () -> Unit) {
        content = newContent
        composeView?.setContent {
            DraggableOverlay(
                onMove = { deltaX, deltaY ->
                    x += deltaX.toInt()
                    y += deltaY.toInt()
                    layoutParams.x = x
                    layoutParams.y = y
                    if (isVisible) {
                        windowManager.updateViewLayout(composeView, layoutParams)
                    }
                },
                onDismiss = { hide() }
            ) {
                content()
            }
        }
    }

    fun show() {
        if (!isVisible) {
            if (composeView == null) {
                composeView = ComposeView(context).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent {
                        DraggableOverlay(
                            onMove = { deltaX, deltaY ->
                                x += deltaX.toInt()
                                y += deltaY.toInt()
                                layoutParams.x = x
                                layoutParams.y = y
                                windowManager.updateViewLayout(this@apply, layoutParams)
                            },
                            onDismiss = { hide() }
                        ) {
                            content()
                        }
                    }
                }
            }

            try {
                windowManager.addView(composeView, layoutParams)
                isVisible = true
            } catch (e: Exception) {
                // Handle overlay permission issues
            }
        }
    }

    fun hide() {
        if (isVisible && composeView != null) {
            try {
                windowManager.removeView(composeView)
                isVisible = false
            } catch (e: Exception) {
                // View might already be removed
            }
        }
    }

    fun remove() {
        hide()
        composeView = null
    }
}