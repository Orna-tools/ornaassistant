package com.lloir.ornaassistant.presentation.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.lloir.ornaassistant.presentation.ui.OrnaAssistantApp
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

@Composable
fun AsusSetupInstructionsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
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
                    text = "ASUS Device Setup",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ASUS devices require special setup for screen reading:",
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "⚠️ Important ASUS Settings",
                            fontWeight = FontWeight.Bold
                        )

                        Text("1. Disable Battery Optimization:", fontWeight = FontWeight.Medium)
                        Text(
                            "• Settings → Battery → Battery optimization\n" +
                                    "• Find 'Orna Assistant' → Don't optimize",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )

                        Text("2. Allow Background Activity:", fontWeight = FontWeight.Medium)
                        Text(
                            "• Settings → Apps → Orna Assistant\n" +
                                    "• Advanced → Battery → Background restriction → Remove",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )

                        Text("3. Auto-start Permission:", fontWeight = FontWeight.Medium)
                        Text(
                            "• Settings → Apps → Orna Assistant\n" +
                                    "• Advanced → Enable 'Auto-start'",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )

                        Text("4. Gaming Mode Exception:", fontWeight = FontWeight.Medium)
                        Text(
                            "• If you have ROG Phone:\n" +
                                    "• Game Genie → Settings → Block list\n" +
                                    "• Add both Orna RPG and Orna Assistant",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enable Accessibility Service:",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "1. Go to Settings → Accessibility\n" +
                            "2. Find 'Downloaded apps' or 'Installed services'\n" +
                            "3. Select 'Orna Assistant'\n" +
                            "4. Toggle ON and confirm\n" +
                            "5. You should see a notification",
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Troubleshooting:",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "• If service stops: Force stop the app and restart\n" +
                            "• If no screen reading: Check notification is visible\n" +
                            "• Restart phone after changing battery settings",
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Add diagnostic toggle (only show in debug mode)
                val isDebugMode = context.applicationInfo.flags and
                        ApplicationInfo.FLAG_DEBUGGABLE != 0

                if (isDebugMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    var diagnosticEnabled by remember {
                        mutableStateOf(
                            context.getSharedPreferences("orna_debug", Context.MODE_PRIVATE)
                                .getBoolean("asus_diagnostic", false)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Enable diagnostic mode",
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = diagnosticEnabled,
                            onCheckedChange = { enabled ->
                                diagnosticEnabled = enabled
                                context.getSharedPreferences("orna_debug", Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("asus_diagnostic", enabled)
                                    .apply()
                            }
                        )
                    }

                    Text(
                        "Creates diagnostic logs to help debug screen reading issues",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = {
                            // Open ASUS battery settings
                            try {
                                val intent = Intent().apply {
                                    component = ComponentName(
                                        "com.android.settings",
                                        "com.android.settings.Settings\$HighPowerApplicationsActivity"
                                    )
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback to general battery settings
                                try {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                } catch (e2: Exception) {
                                    // Last resort - open app settings
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Open Battery Settings")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Got it")
                    }
                }
            }
        }
    }
}

@Composable
fun SamsungSetupInstructionsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
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
                    text = "Samsung Device Setup",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Samsung devices require special setup to prevent the system from stopping the app:",
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "⚠️ Critical Samsung Settings",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text("1. Disable App Sleep:", fontWeight = FontWeight.Medium)
                        Text(
                            "• Settings → Device care → Battery\n" +
                                    "• Background usage limits → Sleeping apps\n" +
                                    "• Remove 'Orna Assistant' if listed\n" +
                                    "• Add to 'Never sleeping apps'",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )

                        Text("2. Disable Battery Optimization:", fontWeight = FontWeight.Medium)
                        Text(
                            "• Settings → Apps → Orna Assistant\n" +
                                    "• Battery → Optimize battery usage → OFF",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )

                        Text("3. Allow Background Activity:", fontWeight = FontWeight.Medium)
                        Text(
                            "• Settings → Apps → Orna Assistant\n" +
                                    "• Battery → Allow background activity → ON",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )

                        Text("4. Disable Adaptive Battery:", fontWeight = FontWeight.Medium)
                        Text(
                            "• Settings → Device care → Battery\n" +
                                    "• More battery settings → Adaptive battery → OFF",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "🎮 Game Launcher Settings",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "If you use Game Launcher/Game Booster:",
                            fontSize = 14.sp
                        )

                        Text(
                            "• Open Game Launcher\n" +
                                    "• Settings → Block during game → OFF\n" +
                                    "• OR remove Orna RPG from Game Launcher",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enable Accessibility Service:",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "1. Settings → Accessibility\n" +
                            "2. Installed apps → Orna Assistant\n" +
                            "3. Toggle ON and confirm\n" +
                            "4. Look for the persistent notification",
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Troubleshooting:",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "• Service stops frequently: Check 'Never sleeping apps'\n" +
                            "• No screen reading: Restart phone after setup\n" +
                            "• Still having issues: Disable 'Device care' optimization",
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = {
                            // Try to open Samsung battery settings
                            try {
                                // Try Samsung-specific intent first
                                val intent = Intent().apply {
                                    component = ComponentName(
                                        "com.samsung.android.lool",
                                        "com.samsung.android.sm.battery.ui.BatteryActivity"
                                    )
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    // Try alternative Samsung settings
                                    val intent = Intent().apply {
                                        component = ComponentName(
                                            "com.samsung.android.sm",
                                            "com.samsung.android.sm.ui.battery.BatteryActivity"
                                        )
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e2: Exception) {
                                    // Fallback to general battery settings
                                    try {
                                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    } catch (e3: Exception) {
                                        // Last resort
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Open Battery Settings")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Got it")
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

    // State for showing dialogs
    private var showAccessibilityDisclosure by mutableStateOf(false)
    private var showAsusDialog by mutableStateOf(false)
    private var showSamsungDialog by mutableStateOf(false)

    private val accessibilityEnabled = MutableStateFlow(false)
    private val overlayEnabled = MutableStateFlow(false)

    private companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_CHECK_DELAY = 1000L
        private const val PREF_ACCESSIBILITY_DISCLOSURE_ACCEPTED = "accessibility_disclosure_accepted"
        private const val PREF_ASUS_DIALOG_SHOWN = "asus_dialog_shown"
        private const val PREF_SAMSUNG_DIALOG_SHOWN = "samsung_dialog_shown"
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

        Log.d(TAG, "MainActivity created on ${Build.MANUFACTURER} ${Build.MODEL}")

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

                        // Check if we should show device-specific dialogs
                        checkForDeviceSpecificSetup()

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

                    // Show ASUS setup dialog when needed
                    if (showAsusDialog) {
                        AsusSetupInstructionsDialog(
                            onDismiss = {
                                showAsusDialog = false
                                // Mark as shown
                                getSharedPreferences("orna_assistant_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean(PREF_ASUS_DIALOG_SHOWN, true)
                                    .apply()
                            }
                        )
                    }

                    // Show Samsung setup dialog when needed
                    if (showSamsungDialog) {
                        SamsungSetupInstructionsDialog(
                            onDismiss = {
                                showSamsungDialog = false
                                // Mark as shown
                                getSharedPreferences("orna_assistant_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean(PREF_SAMSUNG_DIALOG_SHOWN, true)
                                    .apply()
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

            // Re-check for device-specific dialogs on resume
            checkForDeviceSpecificSetup()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "MainActivity destroyed")
        super.onDestroy()
    }

    private fun checkForDeviceSpecificSetup() {
        val manufacturer = Build.MANUFACTURER.lowercase()

        when {
            manufacturer.contains("asus") -> checkForAsusDevice()
            manufacturer.contains("samsung") -> checkForSamsungDevice()
            // Add more manufacturers as needed
        }
    }

    private fun checkForAsusDevice() {
        val prefs = getSharedPreferences("orna_assistant_prefs", MODE_PRIVATE)
        val hasShownAsusDialog = prefs.getBoolean(PREF_ASUS_DIALOG_SHOWN, false)
        val isAccessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(this)

        // Show dialog if:
        // 1. Never shown before, OR
        // 2. Accessibility is not enabled
        if (!hasShownAsusDialog || !isAccessibilityEnabled) {
            Log.d(TAG, "Showing ASUS setup dialog (shown before: $hasShownAsusDialog, accessibility: $isAccessibilityEnabled)")
            showAsusDialog = true
        }
    }

    private fun checkForSamsungDevice() {
        val prefs = getSharedPreferences("orna_assistant_prefs", MODE_PRIVATE)
        val hasShownSamsungDialog = prefs.getBoolean(PREF_SAMSUNG_DIALOG_SHOWN, false)
        val isAccessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(this)

        // Show dialog if not shown before or accessibility is disabled
        if (!hasShownSamsungDialog || !isAccessibilityEnabled) {
            Log.d(TAG, "Showing Samsung setup dialog")
            showSamsungDialog = true
        }
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