package com.lloir.ornaassistant.overlays

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

open class Overlay(
    val mWM: WindowManager,
    val mCtx: Context,
    val mView: View,
    val mWidth: Double
) {
    var mVisible = AtomicBoolean(false)
    private val mUIHandlerThread = HandlerThread("UIHandlerThread")
    var mUIRequestHandler: Handler

    object mPos {
        var x = 0
        var y = 0
        var startX = 0
        var startY = 0
        var eventStartX = 0
        var eventStartY = 0
        var moveEvents = 0
    }

    private var mParamFloat: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        width = (mCtx.resources.displayMetrics.widthPixels * mWidth).toInt()
        gravity = Gravity.TOP or Gravity.LEFT
        x = mPos.x
        y = mPos.y
    }

    fun isVisible(): Boolean {
        return mVisible.get()
    }

    @SuppressLint("ClickableViewAccessibility")open fun show() {
        if (mVisible.compareAndSet(false, true)) {
            if (mView.parent == null) {
                mUIRequestHandler.post {
                    mWM.addView(mView, mParamFloat)
                }

                mView.setOnTouchListener { v, event ->
                    val x = event.rawX.toInt()
                    val y = event.rawY.toInt()
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        mPos.startX = mPos.x
                        mPos.startY = mPos.y
                        mPos.eventStartX = x
                        mPos.eventStartY = y
                        mPos.moveEvents = 0
                    } else if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_UP) {
                        val dist = move(x, y)
                        if (event.action == MotionEvent.ACTION_UP && dist < 20.0) {
                            hide()
                        }
                    }
                    false
                }
            }
            mVisible.set(true)

            Log.i("OrnaOverlay", "SHOW DONE!")
        }
    }

    fun move(x: Int, y: Int): Double {
        val eventX = mPos.startX + x - mPos.eventStartX
        val eventY = mPos.startY + y - mPos.eventStartY
        val distX = abs(mPos.eventStartX - x)
        val distY = abs(mPos.eventStartY - y)
        val dist = sqrt(distX.toDouble().pow(2.0) + distY.toDouble().pow(2.0))
        mPos.x = eventX
        mPos.y = eventY
        val params = mParamFloat
        params.x = mPos.x
        params.y = mPos.y
        mWM.updateViewLayout(mView, params)
        return dist
    }

    open fun hide() {
        if (mVisible.compareAndSet(true, false)) {
            Log.i("OrnaOverlay", "HIDE!")
            if (mView.parent != null) {
                mUIRequestHandler.post {
                    mWM.removeViewImmediate(mView)
                }
                mVisible.set(false)
            }
        }
    }

    init {
        mUIHandlerThread.start()
        mUIRequestHandler = Handler(mUIHandlerThread.looper)
    }
}