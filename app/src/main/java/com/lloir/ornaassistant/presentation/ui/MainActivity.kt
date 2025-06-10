package com.lloir.ornaassistant.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.lloir.ornaassistant.presentation.theme.OrnaAssistantTheme
import com.lloir.ornaassistant.presentation.viewmodel.AccessibilityServiceViewModel
import com.lloir.ornaassistant.presentation.viewmodel.PermissionStatus
import com.lloir.ornaassistant.service.overlay.OverlayManager
import com.lloir.ornaassistant.utils.PermissionHelper
import com.lloir.ornaassistant.utils.OverlayDebugger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var overlayManager: OverlayManager

    @Inject
    lateinit var overlayDebugger: OverlayDebugger

    private companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_CHECK_DELAY = 1000L
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        lifecycleScope.launch {
            // Add a small delay to ensure permission state is updated
            delay(500)
            checkAndUpdatePermissions()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "MainActivity created")

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            OrnaAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val serviceViewModel: AccessibilityServiceViewModel = hiltViewModel()

                    LaunchedEffect(Unit) {
                        Log.d(TAG, "Starting initial permission check")
                        checkAndUpdatePermissions()

                        // Debug overlay issues
                        debugOverlaySetup()
                    }

                    OrnaAssistantApp(
                        navController = navController,
                        onRequestOverlayPermission = { requestOverlayPermission() },
                        onRequestAccessibilityPermission = { requestAccessibilityPermission() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity resumed, checking permissions")

        lifecycleScope.launch {
            // Add delay to ensure any permission changes are reflected
            delay(PERMISSION_CHECK_DELAY)
            checkAndUpdatePermissions()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "MainActivity destroyed")
        super.onDestroy()
    }

    private fun checkAndUpdatePermissions() {
        try {
            val viewModel: AccessibilityServiceViewModel =
                androidx.lifecycle.ViewModelProvider(this)[AccessibilityServiceViewModel::class.java]

            val hasOverlayPermission = PermissionHelper.hasOverlayPermission(this)
            val hasAccessibilityPermission = PermissionHelper.isAccessibilityServiceEnabled(this)

            Log.d(TAG, "Permission status - Overlay: $hasOverlayPermission, Accessibility: $hasAccessibilityPermission")

            // Initialize overlay manager if overlay permission is granted
            if (hasOverlayPermission) {
                lifecycleScope.launch {
                    try {
                        Log.d(TAG, "Overlay permission granted, initializing overlay manager")
                        overlayManager.initialize()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error initializing overlay manager", e)
                    }
                }
            } else {
                Log.w(TAG, "Overlay permission not granted")
            }

            // Update permission status in ViewModel
            val permissionStatus = when {
                hasOverlayPermission && hasAccessibilityPermission -> {
                    Log.i(TAG, "All permissions granted")
                    PermissionStatus.GRANTED
                }
                else -> {
                    Log.w(TAG, "Some permissions missing")
                    PermissionStatus.NOT_GRANTED
                }
            }

            viewModel.updatePermissionStatus(permissionStatus)

        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
        }
    }

    private fun requestOverlayPermission() {
        try {
            if (!PermissionHelper.hasOverlayPermission(this)) {
                Log.d(TAG, "Requesting overlay permission")
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            } else {
                Log.d(TAG, "Overlay permission already granted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting overlay permission", e)
        }
    }

    private fun requestAccessibilityPermission() {
        try {
            Log.d(TAG, "Opening accessibility settings")
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening accessibility settings", e)
        }
    }

    private fun debugOverlaySetup() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Running overlay debug diagnostics...")

                // Run comprehensive debugging
                val debugReport = overlayDebugger.debugOverlayIssues(this@MainActivity)
                Log.i(TAG, "Debug report:\n$debugReport")

                // Test overlay creation
                val testResult = overlayDebugger.testOverlayCreation(this@MainActivity)
                when (testResult) {
                    is OverlayDebugger.TestResult.Success -> {
                        Log.i(TAG, "Overlay test passed: ${testResult.message}")
                    }
                    is OverlayDebugger.TestResult.Failure -> {
                        Log.w(TAG, "Overlay test failed: ${testResult.message}")
                    }
                }

                // Get recommendations
                val recommendations = overlayDebugger.getRecommendations(this@MainActivity)
                if (recommendations.isNotEmpty()) {
                    Log.i(TAG, "Recommendations:")
                    recommendations.forEach { recommendation ->
                        Log.i(TAG, "  - $recommendation")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error running overlay diagnostics", e)
            }
        }
    }
}