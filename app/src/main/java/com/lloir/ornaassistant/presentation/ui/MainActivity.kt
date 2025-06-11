package com.lloir.ornaassistant.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@Composable
fun AccessibilityDisclosureDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "ACCESSIBILITY PERMISSION REQUIRED",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Orna Assistant needs accessibility permission to provide the following features:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                val features = listOf(
                    "• Monitor game screen content to track dungeon runs and battles",
                    "• Display helpful overlays with dungeon statistics and progress",
                    "• Track wayvessel sessions and party notifications",
                    "• Provide item assessment and quality analysis",
                    "• Show real-time battle and loot information"
                )

                features.forEach { feature ->
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DATA USAGE & PRIVACY:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                val privacyItems = listOf(
                    "• Game data is stored locally on your device",
                    "• Item data may be sent to orna.guide API for assessments",
                    "• No personal information is collected or shared",
                    "• Only Orna game content is monitored when active",
                    "• You can disable this permission at any time in Settings"
                )

                privacyItems.forEach { item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "By continuing, you consent to this use of accessibility services for enhancing your Orna gameplay experience.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Decline")
                    }

                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Accept & Continue")
                    }
                }
            }
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var overlayManager: OverlayManager

    @Inject
    lateinit var overlayDebugger: OverlayDebugger

    // State for showing accessibility disclosure dialog
    private var showAccessibilityDisclosure by mutableStateOf(false)

    private val accessibilityEnabled = MutableStateFlow(false)
    private val overlayEnabled = MutableStateFlow(false)

    private companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_CHECK_DELAY = 1000L
        private const val PREF_ACCESSIBILITY_DISCLOSURE_ACCEPTED = "accessibility_disclosure_accepted"
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

                    // Show accessibility disclosure dialog when needed
                    if (showAccessibilityDisclosure) {
                        AccessibilityDisclosureDialog(
                            onAccept = {
                                handleAccessibilityDisclosureAccepted()
                            },
                            onDecline = {
                                showAccessibilityDisclosure = false
                                Log.d(TAG, "User declined accessibility disclosure")
                            }
                        )
                    }

                    OrnaAssistantApp(
                        navController = navController,
                        onRequestOverlayPermission = { requestOverlayPermission() },
                        onRequestAccessibilityPermission = { requestAccessibilityPermissionWithDisclosure() }
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
        lifecycleScope.launch {
            try {
                val hasOverlayPermission = PermissionHelper.hasOverlayPermission(this@MainActivity)
                val hasAccessibilityPermission = PermissionHelper.isAccessibilityServiceEnabled(this@MainActivity)

                Log.d(TAG, "Permission status - Overlay: $hasOverlayPermission, Accessibility: $hasAccessibilityPermission")

                // Update state flows for UI
                accessibilityEnabled.value = hasAccessibilityPermission
                overlayEnabled.value = hasOverlayPermission

                // Initialize overlay manager if overlay permission is granted
                if (hasOverlayPermission) {
                        try {
                            Log.d(TAG, "Overlay permission granted, initializing overlay manager")
                            overlayManager.initialize()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error initializing overlay manager", e)
                        }
                } else {
                    Log.w(TAG, "Overlay permission not granted")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error checking permissions", e)
            }
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

    private fun requestAccessibilityPermissionWithDisclosure() {
        try {
            if (hasUserAcceptedDisclosure()) {
                // User has already accepted disclosure, go directly to settings
                Log.d(TAG, "User previously accepted disclosure, opening accessibility settings")
                openAccessibilitySettings()
            } else {
                // Show disclosure first
                Log.d(TAG, "Showing accessibility disclosure dialog")
                showAccessibilityDisclosure = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting accessibility permission", e)
        }
    }

    private fun handleAccessibilityDisclosureAccepted() {
        try {
            Log.d(TAG, "User accepted accessibility disclosure")

            // Save acceptance to preferences
            saveDisclosureAcceptance()

            // Hide dialog
            showAccessibilityDisclosure = false

            // Open accessibility settings
            openAccessibilitySettings()

        } catch (e: Exception) {
            Log.e(TAG, "Error handling disclosure acceptance", e)
        }
    }

    private fun openAccessibilitySettings() {
        try {
            Log.d(TAG, "Opening accessibility settings")
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening accessibility settings", e)
        }
    }

    private fun hasUserAcceptedDisclosure(): Boolean {
        return getSharedPreferences("orna_assistant_prefs", MODE_PRIVATE)
            .getBoolean(PREF_ACCESSIBILITY_DISCLOSURE_ACCEPTED, false)
    }

    private fun saveDisclosureAcceptance() {
        getSharedPreferences("orna_assistant_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_ACCESSIBILITY_DISCLOSURE_ACCEPTED, true)
            .apply()
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