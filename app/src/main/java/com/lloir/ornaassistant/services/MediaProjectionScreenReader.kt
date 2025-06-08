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
import java.util.concurrent.atomic.AtomicBoolean
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
        private const val AUTO_CAPTURE_INTERVAL = 3000L // 3 seconds for auto mode
    }

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var handler: Handler? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var mainState: MainState? = null

    private val isProcessing = AtomicBoolean(false)
    private var lastProcessTime = 0L
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    // Manual capture overlay
    private var captureOverlay: View? = null
    private var isAutoMode = false
    private var autoHandler: Handler? = null
    private var autoRunnable: Runnable? = null
    private var isProjectionActive = false

    // Preference listener for camera button control
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "assess_overlay", "assess", "screen_reader_method", "show_capture_button" -> {
                Log.d(TAG, "Relevant preference changed: $key")
                onPreferenceChanged()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
        autoHandler = Handler(Looper.getMainLooper())
        createNotificationChannel()
        getScreenDimensions()
        initializeServices()

        // Check user preference for auto vs manual mode
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        isAutoMode = prefs.getBoolean("media_projection_auto_mode", false)

        // Register preference change listener
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)

        Log.d(TAG, "MediaProjectionScreenReader service created - Mode: ${if (isAutoMode) "Auto" else "Manual"}")
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
                null
            )

            Log.d(TAG, "MainState initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize services", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")

        // CRITICAL: Start foreground immediately to prevent crash
        startForeground(NOTIFICATION_ID, createNotification())

        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MAX_VALUE)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)

                Log.d(TAG, "Received resultCode: $resultCode")
                Log.d(TAG, "RESULT_OK constant: ${Activity.RESULT_OK}")
                Log.d(TAG, "Received data: $data")
                Log.d(TAG, "Data extras: ${data?.extras}")

                if (resultCode == Activity.RESULT_OK && data != null) {
                    Log.d(TAG, "Starting projection with valid data (RESULT_OK = $resultCode)")
                    startProjection(resultCode, data)
                } else {
                    Log.e(TAG, "Invalid projection data - resultCode: $resultCode (expected ${Activity.RESULT_OK}), data: $data")

                    // Instead of stopping immediately, try to get cached permission
                    val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                    val cachedResultCode = prefs.getInt("media_projection_result_code", -1)

                    if (cachedResultCode != -1) {
                        Log.d(TAG, "Trying with cached result code: $cachedResultCode")
                        Toast.makeText(this, "Please grant screen capture permission again", Toast.LENGTH_LONG).show()
                    } else {
                        Log.e(TAG, "No cached permission data available")
                        Toast.makeText(this, "Please grant screen capture permission first", Toast.LENGTH_LONG).show()
                    }

                    // Don't stop service immediately - let it stay as foreground
                    return START_STICKY
                }
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping projection service")
                stopProjection()
                stopSelf()
            }
            ACTION_CAPTURE -> {
                Log.d(TAG, "Manual capture triggered")
                triggerManualCapture()
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

        Log.d(TAG, "Screen dimensions: ${screenWidth}x${screenHeight}, density: $screenDensity")
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        try {
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

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
                handler
            )

            if (isAutoMode) {
                setupAutoCapture()
            } else {
                setupManualCapture()
            }

            isProjectionActive = true

            // Update notification now that projection is active
            updateNotificationOnProjectionStart()

            Log.d(TAG, "MediaProjection started successfully - Mode: ${if (isAutoMode) "Auto" else "Manual"}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start projection", e)
            Toast.makeText(this, "Failed to start screen capture: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupAutoCapture() {
        imageReader?.setOnImageAvailableListener({ reader ->
            val currentTime = System.currentTimeMillis()
            if (!isProcessing.get() && (currentTime - lastProcessTime) > AUTO_CAPTURE_INTERVAL) {
                lastProcessTime = currentTime
                Log.d(TAG, "Auto processing new image...")
                serviceScope.launch {
                    processImage(reader)
                }
            }
        }, handler)

        // Start auto capture loop
        autoRunnable = object : Runnable {
            override fun run() {
                triggerCapture()
                autoHandler?.postDelayed(this, AUTO_CAPTURE_INTERVAL)
            }
        }
        autoHandler?.post(autoRunnable!!)
    }

    private fun setupManualCapture() {
        // Create floating capture button only if assess overlay is enabled
        updateCaptureOverlayVisibility()
    }

    private fun updateCaptureOverlayVisibility() {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val isScreenReaderEnabled = prefs.getString("screen_reader_method", "accessibility") == "media_projection"
        val isAssessOverlayEnabled = prefs.getBoolean("assess_overlay", true) || prefs.getBoolean("assess", true)
        val showCaptureButton = prefs.getBoolean("show_capture_button", true)

        val shouldShowButton = isScreenReaderEnabled && isAssessOverlayEnabled && showCaptureButton && !isAutoMode && isProjectionActive

        Log.d(TAG, "Updating capture overlay visibility - should show: $shouldShowButton")
        Log.d(TAG, "  - Screen reader enabled: $isScreenReaderEnabled")
        Log.d(TAG, "  - Assess overlay enabled: $isAssessOverlayEnabled")
        Log.d(TAG, "  - Show capture button: $showCaptureButton")
        Log.d(TAG, "  - Is manual mode: ${!isAutoMode}")
        Log.d(TAG, "  - Projection active: $isProjectionActive")

        if (shouldShowButton && captureOverlay == null) {
            // Create the overlay if it should be shown but doesn't exist
            Log.d(TAG, "Creating capture overlay")
            createCaptureOverlay()
        } else if (!shouldShowButton && captureOverlay != null) {
            // Hide the overlay if it shouldn't be shown
            try {
                val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                windowManager.removeView(captureOverlay)
                captureOverlay = null
                Log.d(TAG, "Capture overlay hidden - assess overlay disabled or other conditions not met")
            } catch (e: Exception) {
                Log.w(TAG, "Error hiding capture overlay", e)
            }
        }
    }

    private fun onPreferenceChanged() {
        Log.d(TAG, "Preferences changed - updating capture overlay visibility")
        handler?.post {
            updateCaptureOverlayVisibility()
        }
    }

    private fun createCaptureOverlay() {
        try {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val inflater = LayoutInflater.from(this)

            // Try to create a simple button if capture_overlay.xml doesn't exist
            captureOverlay = try {
                inflater.inflate(R.layout.capture_overlay, null)
            } catch (e: Exception) {
                Log.w(TAG, "capture_overlay.xml not found, creating simple button", e)
                createSimpleCaptureButton()
            }

            val captureButton = captureOverlay?.findViewById<Button>(R.id.btnCapture)

            captureButton?.setOnClickListener {
                Log.d(TAG, "Manual capture button pressed")
                triggerManualCapture()
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
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

            windowManager.addView(captureOverlay, params)
            Log.d(TAG, "Manual capture overlay created")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create capture overlay", e)
        }
    }

    private fun createSimpleCaptureButton(): View {
        val button = Button(this)
        button.id = R.id.btnCapture
        button.text = "📷"
        button.textSize = 24f
        button.setBackgroundColor(Color.parseColor("#FF4CAF50"))
        button.elevation = 8f

        val layoutParams = WindowManager.LayoutParams(
            200, 200, // 200x200 pixels
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        button.layoutParams = layoutParams

        return button
    }

    private fun triggerManualCapture() {
        if (!isProcessing.get()) {
            Log.d(TAG, "Triggering manual capture...")
            serviceScope.launch {
                captureAndProcess()
            }
        } else {
            Log.d(TAG, "Already processing, skipping manual capture")
        }
    }

    private fun triggerCapture() {
        // This forces a new frame to be available
        imageReader?.let { reader ->
            try {
                val image = reader.acquireLatestImage()
                image?.close()
            } catch (e: Exception) {
                // Ignore - just trying to trigger a new frame
            }
        }
    }

    private suspend fun captureAndProcess() {
        if (!isProcessing.compareAndSet(false, true)) {
            return
        }

        try {
            imageReader?.let { reader ->
                // Give the system a moment to capture the current screen
                delay(100)

                val image = reader.acquireLatestImage()
                if (image != null) {
                    Log.d(TAG, "Manual capture successful, processing...")
                    val bitmap = imageToBitmap(image)
                    image.close()

                    if (bitmap != null) {
                        analyzeOrnaScreen(bitmap)
                    }
                } else {
                    Log.w(TAG, "No image available for manual capture")
                }
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

        var image: Image? = null
        try {
            image = reader.acquireLatestImage()
            if (image == null) {
                Log.w(TAG, "Could not acquire image")
                return
            }

            val bitmap = imageToBitmap(image)
            if (bitmap != null) {
                analyzeOrnaScreen(bitmap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
        } finally {
            image?.close()
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
            Log.d(TAG, "Starting OCR analysis...")
            val screenDataList = convertBitmapToScreenData(screenshot)
            Log.d(TAG, "OCR found ${screenDataList.size} text elements")

            if (screenDataList.isNotEmpty()) {
                // First, let's log ALL the text we found for debugging
                Log.d(TAG, "=== ALL OCR TEXT ===")
                screenDataList.forEachIndexed { index, data ->
                    Log.d(TAG, "[$index] '${data.name}'")
                }
                Log.d(TAG, "=== END ALL TEXT ===")

                // Look for item name - it's usually before "Level" or contains quality indicators
                val itemNameCandidates = screenDataList.filter { data ->
                    val name = data.name.trim()
                    // Item names are usually:
                    // 1. Before "Level" text
                    // 2. Contain quality prefixes
                    // 3. Are longer than 3 characters
                    // 4. Don't contain numbers or colons
                    name.length > 3 &&
                            !name.contains(":") &&
                            !name.contains("Level") &&
                            !name.contains("ACQUIRED") &&
                            !name.contains("Max:") &&
                            !name.matches(Regex(".*\\d.*")) && // No numbers
                            (name.contains("Staff") || name.contains("Sword") || name.contains("Bow") ||
                                    name.contains("Armor") || name.contains("Shield") || name.contains("Ring") ||
                                    name.contains("Necklace") || name.contains("Helmet") || name.contains("Gloves") ||
                                    name.contains("Boots") || name.contains("Robe") || name.contains("Dagger") ||
                                    name.contains("Axe") || name.contains("Hammer") || name.contains("Spear") ||
                                    name.contains("Wand") || name.contains("Orb") || name.contains("Tome") ||
                                    name.contains("Nagamaki") || name.contains("Katana") || name.contains("Blade") ||
                                    name.startsWith("Ornate") || name.startsWith("Famed") || name.startsWith("Legendary") ||
                                    name.startsWith("Superior") || name.startsWith("Masterforged") || name.startsWith("Demonforged") ||
                                    name.startsWith("Godforged") || name.startsWith("Broken") || name.startsWith("Poor") ||
                                    // Or just a reasonable length word that looks like an item name
                                    (name.length > 5 && name.length < 40 && name.matches(Regex("[A-Za-z\\s]+.*")))
                                    )
                }

                // Log potential item names
                if (itemNameCandidates.isNotEmpty()) {
                    Log.d(TAG, "=== ITEM NAME CANDIDATES ===")
                    itemNameCandidates.forEach { candidate ->
                        Log.d(TAG, "Candidate: '${candidate.name}'")
                    }
                    Log.d(TAG, "=== END CANDIDATES ===")
                }

                // Filter for Orna stat content
                val ornaStats = screenDataList.filter { data ->
                    val name = data.name.trim()
                    name.length > 2 && (
                            name.contains("Attack") || name.contains("Magic") || name.contains("Defense") ||
                                    name.contains("Level") || name.contains("ACQUIRED") || name.contains("Tier") ||
                                    name.contains("Att:") || name.contains("Mag:") || name.contains("Def:") ||
                                    name.contains("Res:") || name.contains("Dex:") || name.contains("HP:") ||
                                    name.contains("Mana:") || name.contains("Crit:") || name.contains("Ward:") ||
                                    name.matches(Regex(".*:\\s*\\d+.*")) // Stat patterns like "Att: 150"
                            )
                }

                // Check if this looks like an item screen (has ACQUIRED keyword)
                val hasAcquired = screenDataList.any { it.name.contains("ACQUIRED") }

                if (hasAcquired && itemNameCandidates.isNotEmpty()) {
                    Log.d(TAG, "Detected item screen - processing for assessment")

                    // Create clean ScreenData that matches what OrnaViewItem expects
                    val cleanedScreenData = ArrayList<ScreenData>()

                    // Add the best item name candidate first (this becomes the item name)
                    val bestItemName = itemNameCandidates.firstOrNull { candidate ->
                        val name = candidate.name.trim()
                        // Prefer names with quality prefixes or weapon types
                        name.startsWith("Masterforged") || name.startsWith("Demonforged") ||
                                name.startsWith("Godforged") || name.startsWith("Ornate") ||
                                name.startsWith("Famed") || name.startsWith("Legendary") ||
                                name.contains("Nagamaki") || name.contains("Staff") || name.contains("Sword")
                    } ?: itemNameCandidates.firstOrNull()

                    bestItemName?.let { cleanedScreenData.add(it) }

                    // Add level and stats
                    ornaStats.forEach { stat ->
                        cleanedScreenData.add(stat)
                    }

                    // Add ACQUIRED for item detection
                    screenDataList.find { it.name.contains("ACQUIRED") }?.let {
                        cleanedScreenData.add(it)
                    }

                    Log.d(TAG, "Sending ${cleanedScreenData.size} cleaned elements to MainState for item assessment")
                    cleanedScreenData.forEach { data ->
                        Log.d(TAG, "Clean data: '${data.name}'")
                    }

                    // Send to MainState - this should trigger OrnaViewItem and the API assessment
                    mainState?.processData("com.orna", cleanedScreenData)
                } else {
                    Log.d(TAG, "Not an item screen or no item names found - skipping assessment")
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
            Log.d(TAG, "OCR extracted ${text.length} characters total")

            if (text.isNotBlank()) {
                // Split by lines and preserve the original order (top to bottom)
                val lines = text.lines()
                Log.d(TAG, "OCR found ${lines.size} lines of text")

                lines.forEachIndexed { index, line ->
                    val trimmedLine = line.trim()
                    if (trimmedLine.isNotEmpty() && trimmedLine.length > 1) {
                        // Calculate approximate position based on line order
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
                                depth = index, // Use index as depth to preserve order
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
            autoHandler?.removeCallbacks(autoRunnable ?: return)

            captureOverlay?.let { overlay ->
                val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                try {
                    windowManager.removeView(overlay)
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing capture overlay", e)
                }
            }

            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()
            mainState?.cleanup()
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

        // Set content text based on projection status
        if (isProjectionActive) {
            builder.setContentText("Screen reader active - ${if (isAutoMode) "Auto" else "Manual"} mode")

            // Only add capture button if we're in manual mode AND projection is running
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