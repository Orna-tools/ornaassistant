package com.lloir.ornaassistant.overlays

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

open class Overlay(
    protected val mWM: WindowManager,
    protected val mCtx: Context,
    protected val mView: View,
    protected val mWidth: Double
) {
    private val mUIHandlerThread = HandlerThread("UIHandlerThread").apply { start() }
    protected val mUIRequestHandler: Handler = Handler(mUIHandlerThread.looper)
    private val mVisible = AtomicBoolean(false)

    protected val mParamFloat = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        width = (mCtx.resources.displayMetrics.widthPixels * mWidth).toInt()
        gravity = Gravity.TOP or Gravity.LEFT
        x = 0
        y = 0
    }

    @SuppressLint("ClickableViewAccessibility")
    open fun show() {
        if (mVisible.compareAndSet(false, true)) {
            mUIRequestHandler.post { mWM.addView(mView, mParamFloat) }
            mView.setOnTouchListener { _, event ->
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()
                if (event.action == MotionEvent.ACTION_DOWN) {
                    hide()
                }
                false
            }
        }
    }

    open fun hide() {
        if (mVisible.compareAndSet(true, false)) {
            mUIRequestHandler.post { mWM.removeViewImmediate(mView) }
        }
    }

    companion object {
        private var activeOverlay: Overlay? = null

        fun startOverlay(context: Context, overlayType: String) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val inflater = LayoutInflater.from(context)
            val layoutId = getOverlayLayout(overlayType)
            if (layoutId == 0) {
                // Log an error or handle the case where the layout for the overlay type doesn't exist
                Log.e("Overlay", "No layout defined for overlay type: $overlayType")
                return
            }
            val view = inflater.inflate(layoutId, null)

            val overlay = when (overlayType) {
                "inviter" -> InviterOverlay(wm, context, view, 0.8)
                "session" -> SessionOverlay(wm, context, view, 0.8)
                "assess" -> AssessOverlay(wm, context, view, 0.8) // Assuming AssessOverlay exists and width 0.8 is okay
                // Add other overlay types here
                else -> {
                    Log.e("Overlay", "Unknown overlay type: $overlayType. Cannot create overlay instance.")
                    null
                }
            }

            overlay?.let {
                activeOverlay?.hide() // Hide previous overlay
                activeOverlay = it
                it.show()
            }
        }

        fun stopOverlay() {
            activeOverlay?.hide()
            activeOverlay = null
        }

        private fun getOverlayLayout(overlayType: String): Int {
            return when (overlayType) {
                "inviter" -> com.lloir.ornaassistant.R.layout.overlay_inviter
                "session" -> com.lloir.ornaassistant.R.layout.overlay_session
                "assess" -> com.lloir.ornaassistant.R.layout.assess_layout // Added assess overlay
                // Add other overlay layouts here
                else -> 0
            }
        }
    }
}
