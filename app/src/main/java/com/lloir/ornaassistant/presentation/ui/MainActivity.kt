package com.lloir.ornaassistant.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import androidx.navigation.compose.rememberNavController
import com.lloir.ornaassistant.presentation.theme.OrnaAssistantTheme
import com.lloir.ornaassistant.presentation.viewmodel.AccessibilityServiceViewModel
import com.lloir.ornaassistant.presentation.viewmodel.PermissionStatus
import com.lloir.ornaassistant.utils.AccessibilityUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle overlay permission result
        val viewModel: AccessibilityServiceViewModel =
            androidx.lifecycle.ViewModelProvider(this)[AccessibilityServiceViewModel::class.java]

        if (Settings.canDrawOverlays(this)) {
            viewModel.updatePermissionStatus(PermissionStatus.GRANTED)
        } else {
            viewModel.updatePermissionStatus(PermissionStatus.DENIED)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            OrnaAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val serviceViewModel: AccessibilityServiceViewModel = hiltViewModel()

                    // Check permissions on startup
                    LaunchedEffect(Unit) {
                        checkPermissions(serviceViewModel)
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

        // Re-check permissions when returning to app
        val viewModel: AccessibilityServiceViewModel =
            androidx.lifecycle.ViewModelProvider(this)[AccessibilityServiceViewModel::class.java]
        checkPermissions(viewModel)
    }

    private fun checkPermissions(viewModel: AccessibilityServiceViewModel) {
        val hasOverlayPermission = Settings.canDrawOverlays(this)
        val hasAccessibilityPermission = AccessibilityUtils.isAccessibilityServiceEnabled(this)

        when {
            hasOverlayPermission && hasAccessibilityPermission -> {
                viewModel.updatePermissionStatus(PermissionStatus.GRANTED)
            }
            else -> {
                viewModel.updatePermissionStatus(PermissionStatus.NOT_GRANTED)
            }
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
}
