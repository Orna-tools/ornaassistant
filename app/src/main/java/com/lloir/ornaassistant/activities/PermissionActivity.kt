package com.lloir.ornaassistant.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import com.lloir.ornaassistant.services.MediaProjectionScreenReader

class PermissionActivity : Activity() {
    
    companion object {
        const val REQUEST_MEDIA_PROJECTION = 1000
        private const val TAG = "PermissionActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMediaProjectionPermission()
    }
    
    private fun requestMediaProjectionPermission() {
        try {
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent = mediaProjectionManager.createScreenCaptureIntent()
            startActivityForResult(intent, REQUEST_MEDIA_PROJECTION)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to request screen capture permission", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_MEDIA_PROJECTION -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    startMediaProjectionService(resultCode, data)
                    Toast.makeText(this, "Screen capture started", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        finish()
    }
    
    private fun startMediaProjectionService(resultCode: Int, data: Intent) {
        try {
            val serviceIntent = Intent(this, MediaProjectionScreenReader::class.java).apply {
                action = MediaProjectionScreenReader.ACTION_START
                putExtra(MediaProjectionScreenReader.EXTRA_RESULT_CODE, resultCode)
                putExtra(MediaProjectionScreenReader.EXTRA_DATA, data)
            }
            startForegroundService(serviceIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start screen capture service", Toast.LENGTH_LONG).show()
        }
    }
}
