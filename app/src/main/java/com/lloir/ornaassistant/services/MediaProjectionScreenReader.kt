package com.lloir.ornaassistant.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.lloir.ornaassistant.MainState
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.ScreenData
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume

class MediaProjectionScreenReader : Service() {

    companion object {
        private const val TAG = "MediaProjectionReader"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "media_projection_channel"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CAPTURE = "ACTION_CAPTURE"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
        private const val AUTO_CAPTURE_INTERVAL = 3000L
        private const val MIN_PROCESSING_INTERVAL = 1000L
        private const val MAX_BITMAP_SIZE = 2048 // Reduce bitmap size to prevent OOM
    }

    // Core components
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mainHandler: Handler? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // OCR and processing
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var mainStateRef: WeakReference<MainState>? = null

    // State management
    private val isProcessing = AtomicBoolean(false)
    private val lastProcessTime = AtomicLong(0L)
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    // UI components
    private var captureOverlayRef: WeakReference<View>? = null
    private var isAutoMode = false
    private var autoHandler: Handler? = null
    private var autoRunnable: Runnable? = null
    private var isProjectionActive = false

    // Preference management
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "assess_overlay", "assess", "screen_reader_method", "show_capture_button" -> {
                Log.d(TAG, "Relevant preference changed: $key")
                mainHandler?.post { onPreferenceChanged() }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize handlers
        mainHandler = Handler(Looper.getMainLooper())
        autoHandler = Handler(Looper.getMainLooper())

        // Setup notification
        createNotificationChannel()
        getScreenDimensions()

        // Initialize services
        if (!initializeServices()) {
            stopSelf()
            return
        }

        // Setup preferences
        setupPreferences()

        Log.d(TAG, "MediaProjectionScreenReader service created")
    }

    private fun setupPreferences() {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        isAutoMode = prefs.getBoolean("media_projection_auto_mode", false)
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
        Log.d(TAG, "Mode: ${if (isAutoMode) "Auto" else "Manual"}")
    }

    private fun initializeServices(): Boolean {
        return try {
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            val mainState = MainState(
                windowManager,
                applicationContext,
                inflater.inflate(R.layout.notification_layout, null),
                inflater.inflate(R.layout.wayvessel_overlay, null),
                inflater.inflate(R.layout.assess_layout, null),
                inflater.inflate(R.layout.kg_layout, null),
                null
            )

            mainStateRef = WeakReference(mainState)
            Log.d(TAG, "MainState initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize services", e)
            false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")

        // Start foreground immediately to prevent crash
        startForeground(NOTIFICATION_ID, createNotification())

        when (intent?.action) {
            ACTION_START -> handleStartAction(intent)
            ACTION_STOP -> handleStopAction()
            ACTION_CAPTURE -> handleCaptureAction()
        }
        return START_STICKY
    }

    private fun handleStartAction(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MAX_VALUE)
        val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)

        Log.d(TAG, "Received resultCode: $resultCode, RESULT_OK: ${Activity.RESULT_OK}")

        if (resultCode == Activity.RESULT_OK && data != null) {
            Log.d(TAG, "Starting projection with valid data")
            startProjection(resultCode, data)
        } else {
            Log.e(TAG, "Invalid projection data")
            handleInvalidProjectionData()
        }
    }

    private fun handleInvalidProjectionData() {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val cachedResultCode = prefs.getInt("media_projection_result_code", -1)

        if (cachedResultCode != -1) {
            Log.d(TAG, "Trying with cached result code: $cachedResultCode")
            Toast.makeText(this, "Please grant screen capture permission again", Toast.LENGTH_LONG).show()
        } else {
            Log.e(TAG, "No cached permission data available")
            Toast.makeText(this, "Please grant screen capture permission first", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleStopAction() {
        Log.d(TAG, "Stopping projection service")
        stopProjection()
        stopSelf()
    }

    private fun handleCaptureAction() {
        Log.d(TAG, "Manual capture triggered")
        if (isProjectionActive) {
            serviceScope.launch {
                captureAndProcess()
            }
        }
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

        // Optimize dimensions to prevent OOM
        val scale = if (displayMetrics.widthPixels > MAX_BITMAP_SIZE || displayMetrics.heightPixels > MAX_BITMAP_SIZE) {
            minOf(
                MAX_BITMAP_SIZE.toFloat() / displayMetrics.widthPixels,
                MAX_BITMAP_SIZE.toFloat() / displayMetrics.heightPixels
            )
        } else 1f

        screenWidth = (displayMetrics.widthPixels * scale).toInt()
        screenHeight = (displayMetrics.heightPixels * scale).toInt()
        screenDensity = displayMetrics.densityDpi

        Log.d(TAG, "Screen dimensions: ${screenWidth}x${screenHeight}, density: $screenDensity, scale: $scale")
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        try {
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            if (mediaProjectionManager == null) {
                Log.e(TAG, "MediaProjectionManager is null")
                Toast.makeText(this, "MediaProjection not available", Toast.LENGTH_LONG).show()
                return
            }

            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

            // Use optimized image format and reduced size
            imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "OrnaAssistant-ScreenCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                mainHandler
            )

            if (isAutoMode) {
                setupAutoCapture()
            } else {
                setupManualCapture()
            }

            isProjectionActive = true
            updateNotificationOnProjectionStart()
            Log.d(TAG, "MediaProjection started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start projection", e)
            Toast.makeText(this, "Failed to start screen capture: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupAutoCapture() {
        imageReader?.setOnImageAvailableListener({ reader ->
            val currentTime = System.currentTimeMillis()
            if (!isProcessing.get() && (currentTime - lastProcessTime.get()) > MIN_PROCESSING_INTERVAL) {
                lastProcessTime.set(currentTime)
                serviceScope.launch {
                    processImage(reader)
                }
            }
        }, mainHandler)

        autoRunnable = object : Runnable {
            override fun run() {
                if (isProjectionActive) {
                    triggerCapture()
                    autoHandler?.postDelayed(this, AUTO_CAPTURE_INTERVAL)
                }
            }
        }
        autoHandler?.post(autoRunnable!!)
    }

    private fun setupManualCapture() {
        updateCaptureOverlayVisibility()
    }

    private fun updateCaptureOverlayVisibility() {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val isScreenReaderEnabled = prefs.getString("screen_reader_method", "accessibility") == "media_projection"
        val isAssessOverlayEnabled = prefs.getBoolean("assess_overlay", true) || prefs.getBoolean("assess", true)
        val showCaptureButton = prefs.getBoolean("show_capture_button", true)

        val shouldShowButton = isScreenReaderEnabled && isAssessOverlayEnabled && showCaptureButton && !isAutoMode && isProjectionActive

        if (shouldShowButton && captureOverlayRef?.get() == null) {
            createCaptureOverlay()
        } else if (!shouldShowButton) {
            hideCaptureOverlay()
        }
    }

    private fun createCaptureOverlay() {
        try {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val button = Button(this).apply {
                id = R.id.btnCapture
                text = "📷"
                textSize = 24f
                setBackgroundColor(Color.parseColor("#FF4CAF50"))
                elevation = 8f
                setOnClickListener {
                    Log.d(TAG, "Manual capture button pressed")
                    handleCaptureAction()
                }
            }

            val params = WindowManager.LayoutParams(
                200, 200,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 20
                y = 200
            }

            windowManager.addView(button, params)
            captureOverlayRef = WeakReference(button)
            Log.d(TAG, "Manual capture overlay created")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create capture overlay", e)
        }
    }

    private fun hideCaptureOverlay() {
        captureOverlayRef?.get()?.let { overlay ->
            try {
                val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                windowManager.removeView(overlay)
                captureOverlayRef = null
                Log.d(TAG, "Capture overlay hidden")
            } catch (e: Exception) {
                Log.w(TAG, "Error hiding capture overlay", e)
            }
        }
    }

    private fun onPreferenceChanged() {
        updateCaptureOverlayVisibility()
    }

    private fun triggerCapture() {
        // Forces a new frame to be available
        try {
            imageReader?.acquireLatestImage()?.close()
        } catch (e: Exception) {
            // Ignore - just trying to trigger a new frame
        }
    }

    private suspend fun captureAndProcess() {
        if (!isProcessing.compareAndSet(false, true)) {
            return
        }

        try {
            imageReader?.let { reader ->
                delay(100) // Allow system to capture current screen

                reader.acquireLatestImage()?.use { image ->
                    Log.d(TAG, "Manual capture successful, processing...")
                    val bitmap = imageToBitmap(image)
                    bitmap?.let {
                        analyzeOrnaScreen(it)
                        // Explicitly recycle bitmap to free memory
                        if (!it.isRecycled) it.recycle()
                    }
                } ?: Log.w(TAG, "No image available for manual capture")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in manual capture", e)
        } finally {
            isProcessing.set(false)
        }
    }

    private suspend fun processImage(reader: ImageReader) {
        if (!isProcessing.compareAndSet(false, true)) {
            return
        }

        try {
            reader.acquireLatestImage()?.use { image ->
                val bitmap = imageToBitmap(image)
                bitmap?.let {
                    analyzeOrnaScreen(it)
                    // Explicitly recycle bitmap to free memory
                    if (!it.isRecycled) it.recycle()
                }
            } ?: Log.w(TAG, "Could not acquire image")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
        } finally {
            isProcessing.set(false)
        }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            if (rowPadding == 0) {
                bitmap
            } else {
                Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
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
                // Check if this looks like an item screen
                val hasAcquired = screenDataList.any { it.name.contains("ACQUIRED", ignoreCase = true) }

                if (hasAcquired) {
                    Log.d(TAG, "Detected item screen - processing for assessment")
                    mainStateRef?.get()?.processData("com.orna", screenDataList)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze screen", e)
        }
    }

    private suspend fun convertBitmapToScreenData(screenshot: Bitmap): ArrayList<ScreenData> {
        val screenDataList = arrayListOf<ScreenData>()
        val processingTime = System.currentTimeMillis()

        try {
            val text = extractTextFromBitmap(screenshot)

            if (text.isNotBlank()) {
                val lines = text.lines().filter { it.trim().length > 1 }

                lines.forEachIndexed { index, line ->
                    val trimmedLine = line.trim()
                    if (trimmedLine.isNotEmpty()) {
                        val lineHeight = screenshot.height / maxOf(lines.size, 40)
                        val rect = Rect(
                            0,
                            index * lineHeight,
                            screenshot.width,
                            (index + 1) * lineHeight
                        )

                        screenDataList.add(
                            ScreenData(
                                name = trimmedLine,
                                rect = rect,
                                time = processingTime,
                                depth = index,
                                mNodeInfo = null
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting bitmap to screen data", e)
        }

        return screenDataList
    }

    private suspend fun extractTextFromBitmap(bitmap: Bitmap): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                textRecognizer.process(image)
                    .addOnSuccessListener { result ->
                        continuation.resume(result.text)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "OCR failed", exception)
                        continuation.resume("")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in extractTextFromBitmap", e)
                continuation.resume("")
            }
        }
    }

    private fun updateNotificationOnProjectionStart() {
        val notification = createNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun stopProjection() {
        try {
            Log.d(TAG, "Stopping projection...")

            // Stop auto capture
            autoHandler?.removeCallbacks(autoRunnable ?: return)

            // Clean up UI
            hideCaptureOverlay()

            // Release media projection resources
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()

            // Clean up main state
            mainStateRef?.get()?.cleanup()
            mainStateRef = null

            // Cancel coroutines
            serviceScope.cancel()

            isProjectionActive = false
            Log.d(TAG, "MediaProjection stopped successfully")
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
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, MediaProjectionScreenReader::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Orna Assistant")
            .setSmallIcon(R.drawable.ric_notification)
            .setOngoing(true)
            .addAction(R.drawable.ric_notification, "Stop", stopPendingIntent)

        if (isProjectionActive) {
            builder.setContentText("Screen reader active - ${if (isAutoMode) "Auto" else "Manual"} mode")

            if (!isAutoMode) {
                val captureIntent = Intent(this, MediaProjectionScreenReader::class.java).apply {
                    action = ACTION_CAPTURE
                }
                val capturePendingIntent = PendingIntent.getService(
                    this, 1, captureIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ric_notification, "Capture", capturePendingIntent)
            }
        } else {
            builder.setContentText("Screen reader starting...")
        }

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")

        // Unregister preference listener
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)

        stopProjection()
    }
}