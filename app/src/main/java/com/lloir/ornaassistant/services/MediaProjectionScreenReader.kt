package com.lloir.ornaassistant.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.lloir.ornaassistant.MainState
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.ScreenData
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
class MediaProjectionScreenReader : Service() {
    
    companion object {
        private const val TAG = "MediaProjectionReader"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "media_projection_channel"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
        private const val MIN_PROCESS_INTERVAL = 1000L
    }
    
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var handler: Handler? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var mainState: MainState? = null
    private var screenReaderManager: ScreenReaderManager? = null
    
    private val isProcessing = AtomicBoolean(false)
    private var lastProcessTime = 0L
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0
    
    // Screen regions for different content types
    private data class ScreenRegion(
        val xPercent: Float,
        val yPercent: Float,
        val widthPercent: Float,
        val heightPercent: Float,
        val name: String
    )
    
    private val itemScreenRegions = listOf(
        ScreenRegion(0.05f, 0.1f, 0.9f, 0.08f, "item_name"),
        ScreenRegion(0.05f, 0.18f, 0.5f, 0.05f, "level"),
        ScreenRegion(0.05f, 0.25f, 0.9f, 0.5f, "attributes"),
        ScreenRegion(0.05f, 0.8f, 0.9f, 0.1f, "acquired_status")
    )
    
    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
        createNotificationChannel()
        getScreenDimensions()
        initializeServices()
    }
    
    private fun initializeServices() {
        try {
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            
            mainState = MainState(
                windowManager,
                applicationContext,
                inflater.inflate(R.layout.notification_layout, null),
                inflater.inflate(R.layout.wayvessel_overlay, null),
                inflater.inflate(R.layout.assess_layout, null),
                inflater.inflate(R.layout.kg_layout, null),
                null // No AccessibilityService reference for MediaProjection
            )
            
            screenReaderManager = ScreenReaderManager.getInstance(this, mainState!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize services", e)
            stopSelf()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
                if (resultCode != -1 && data != null) {
                    startProjection(resultCode, data)
                } else {
                    Log.e(TAG, "Invalid projection data")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopProjection()
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    private fun getScreenDimensions() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = windowManager.defaultDisplay
            display.getRealMetrics(displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        }
        
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        screenDensity = displayMetrics.densityDpi
        
        Log.d(TAG, "Screen: ${screenWidth}x${screenHeight}, density: $screenDensity")
    }
    
    private fun startProjection(resultCode: Int, data: Intent) {
        try {
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
            
            val virtualDisplay = mediaProjection?.createVirtualDisplay(
                "OrnaAssistant-ScreenCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, handler
            )
            
            imageReader?.setOnImageAvailableListener({ reader ->
                val currentTime = System.currentTimeMillis()
                if (!isProcessing.get() && (currentTime - lastProcessTime) > MIN_PROCESS_INTERVAL) {
                    lastProcessTime = currentTime
                    serviceScope.launch {
                        processImage(reader)
                    }
                }
            }, handler)
            
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d(TAG, "MediaProjection started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start projection", e)
            stopSelf()
        }
    }
    
    private suspend fun processImage(reader: ImageReader) {
        if (!isProcessing.compareAndSet(false, true)) {
            return
        }
        
        val image = reader.acquireLatestImage()
        if (image == null) {
            isProcessing.set(false)
            return
        }
        
        try {
            val bitmap = imageToBitmap(image)
            if (bitmap != null) {
                analyzeOrnaScreen(bitmap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
        } finally {
            image.close()
            isProcessing.set(false)
        }
    }
    
    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth
            
            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            
            if (rowPadding == 0) {
                bitmap
            } else {
                Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert image to bitmap", e)
            null
        }
    }
    
    private suspend fun analyzeOrnaScreen(screenshot: Bitmap) {
        try {
            val screenDataList = convertBitmapToScreenData(screenshot)
            if (screenDataList.isNotEmpty()) {
                screenReaderManager?.processScreenData("com.orna", screenDataList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze screen", e)
        }
    }
    
    private suspend fun convertBitmapToScreenData(screenshot: Bitmap): ArrayList<ScreenData> {
        val screenDataList = arrayListOf<ScreenData>()
        val processingTime = System.currentTimeMillis()
        
        for (region in itemScreenRegions) {
            try {
                val regionBitmap = getRegionBitmap(screenshot, region)
                val text = extractTextFromBitmap(regionBitmap)
                
                if (text.isNotBlank()) {
                    text.lines().forEach { line ->
                        val trimmedLine = line.trim()
                        if (trimmedLine.isNotEmpty() && trimmedLine.length > 1) {
                            val rect = calculateRectFromRegion(region)
                            screenDataList.add(
                                ScreenData(
                                    name = trimmedLine,
                                    rect = rect,
                                    time = processingTime,
                                    depth = 0,
                                    mNodeInfo = null
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing region ${region.name}", e)
            }
        }
        
        return screenDataList
    }
    
    private fun getRegionBitmap(fullBitmap: Bitmap, region: ScreenRegion): Bitmap {
        val x = (fullBitmap.width * region.xPercent).toInt().coerceAtLeast(0)
        val y = (fullBitmap.height * region.yPercent).toInt().coerceAtLeast(0)
        val width = (fullBitmap.width * region.widthPercent).toInt()
            .coerceAtMost(fullBitmap.width - x)
        val height = (fullBitmap.height * region.heightPercent).toInt()
            .coerceAtMost(fullBitmap.height - y)
        
        return Bitmap.createBitmap(fullBitmap, x, y, width, height)
    }
    
    private fun calculateRectFromRegion(region: ScreenRegion): Rect {
        val left = (screenWidth * region.xPercent).toInt()
        val top = (screenHeight * region.yPercent).toInt()
        val right = left + (screenWidth * region.widthPercent).toInt()
        val bottom = top + (screenHeight * region.heightPercent).toInt()
        return Rect(left, top, right, bottom)
    }
    
    private suspend fun extractTextFromBitmap(bitmap: Bitmap): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            textRecognizer.process(image)
                .addOnSuccessListener { result ->
                    continuation.resume(result.text)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "OCR failed", exception)
                    continuation.resume("")
                }
            continuation.invokeOnCancellation { /* cleanup if needed */ }
        }
    }
    
    private fun stopProjection() {
        try {
            mediaProjection?.stop()
            imageReader?.close()
            mainState?.cleanup()
            serviceScope.cancel()
            Log.d(TAG, "MediaProjection stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping projection", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Orna Assistant Screen Reader",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Modern screen reading service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Orna Assistant")
            .setContentText("Modern screen reading active")
            .setSmallIcon(R.drawable.ric_notification)
            .setOngoing(true)
            .build()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopProjection()
    }
}
