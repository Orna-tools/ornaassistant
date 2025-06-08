package com.lloir.ornaassistant.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
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
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent = mediaProjectionManager.createScreenCaptureIntent()
            startActivityForResult(intent, REQUEST_MEDIA_PROJECTION)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to request screen capture permission: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_MEDIA_PROJECTION -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // Save permission grant to preferences
                    val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("media_projection_granted", true).apply()

                    // Start the service with the permission data
                    startMediaProjectionService(resultCode, data)
                    Toast.makeText(this, "Screen capture permission granted", Toast.LENGTH_SHORT).show()
                } else {
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
            val serviceIntent = Intent(this, MediaProjectionScreenReader::class.java).apply {
                action = MediaProjectionScreenReader.ACTION_START
                putExtra(MediaProjectionScreenReader.EXTRA_RESULT_CODE, resultCode)
                putExtra(MediaProjectionScreenReader.EXTRA_DATA, data)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start screen capture service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}