package com.lloir.ornaassistant.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.lloir.ornaassistant.services.MediaProjectionScreenReader

class PermissionActivity : Activity() {

    companion object {
        const val REQUEST_MEDIA_PROJECTION = 1000
        private const val TAG = "PermissionActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "PermissionActivity started")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requestMediaProjectionPermission()
        } else {
            Toast.makeText(this, "MediaProjection requires Android 5.0+", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun requestMediaProjectionPermission() {
        try {
            Log.d(TAG, "Requesting MediaProjection permission")

            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            if (mediaProjectionManager == null) {
                Log.e(TAG, "MediaProjectionManager is null")
                Toast.makeText(this, "MediaProjection not available on this device", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            val intent = mediaProjectionManager.createScreenCaptureIntent()
            Log.d(TAG, "Starting screen capture intent")
            startActivityForResult(intent, REQUEST_MEDIA_PROJECTION)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to request screen capture permission", e)
            Toast.makeText(this, "Failed to request screen capture permission: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_MEDIA_PROJECTION -> {
                Log.d(TAG, "MediaProjection result - requestCode: $requestCode, resultCode: $resultCode")
                Log.d(TAG, "RESULT_OK constant value: ${Activity.RESULT_OK}")
                Log.d(TAG, "Data: $data")
                Log.d(TAG, "Data extras: ${data?.extras}")

                if (resultCode == Activity.RESULT_OK && data != null) {
                    Log.d(TAG, "Media projection permission granted - resultCode: $resultCode")

                    // Save permission grant to preferences
                    val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putBoolean("media_projection_granted", true)
                        putInt("media_projection_result_code", resultCode)
                        apply()
                    }

                    // Start the service with the permission data
                    startMediaProjectionService(resultCode, data)
                    Toast.makeText(this, "Screen capture permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w(TAG, "Media projection permission denied - resultCode: $resultCode, expected: ${Activity.RESULT_OK}")
                    Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()

                    // Save denial to preferences
                    val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("media_projection_granted", false).apply()
                }
                finish()
            }
        }
    }

    private fun startMediaProjectionService(resultCode: Int, data: Intent) {
        try {
            Log.d(TAG, "Starting MediaProjection service with resultCode: $resultCode")

            // Validate the data before starting service
            if (data.extras == null) {
                Log.w(TAG, "Intent data has no extras, but proceeding anyway")
            }

            Log.d(TAG, "Data extras keys: ${data.extras?.keySet()}")

            val serviceIntent = Intent(this, MediaProjectionScreenReader::class.java).apply {
                action = MediaProjectionScreenReader.ACTION_START
                putExtra(MediaProjectionScreenReader.EXTRA_RESULT_CODE, resultCode)
                putExtra(MediaProjectionScreenReader.EXTRA_DATA, data)

                // Add all extras from the original intent
                data.extras?.let { extras ->
                    putExtras(extras)
                    Log.d(TAG, "Added ${extras.size()} extras to service intent")
                }
            }

            Log.d(TAG, "Service intent created with action: ${serviceIntent.action}")
            Log.d(TAG, "Service intent result code: ${serviceIntent.getIntExtra(MediaProjectionScreenReader.EXTRA_RESULT_CODE, -999)}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
                Log.d(TAG, "Started foreground service")
            } else {
                startService(serviceIntent)
                Log.d(TAG, "Started regular service")
            }

            Log.d(TAG, "MediaProjection service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screen capture service", e)
            Toast.makeText(this, "Failed to start screen capture service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}