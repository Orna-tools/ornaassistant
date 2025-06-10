package com.lloir.ornaassistant.utils

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayDebugger @Inject constructor() {

    companion object {
        private const val TAG = "OverlayDebugger"
    }

    /**
     * Comprehensive debugging of overlay-related issues
     */
    fun debugOverlayIssues(context: Context): DebugReport {
        Log.d(TAG, "Starting comprehensive overlay debugging...")

        val report = DebugReport()

        // Basic system info
        report.addSection("System Information")
        report.addItem("Android Version", Build.VERSION.RELEASE)
        report.addItem("SDK Level", Build.VERSION.SDK_INT.toString())
        report.addItem("Device Model", "${Build.MANUFACTURER} ${Build.MODEL}")
        report.addItem("Package Name", context.packageName)

        // Permission checks
        report.addSection("Permission Checks")
        debugPermissions(context, report)

        // Service checks
        report.addSection("Service Status")
        debugServiceStatus(context, report)

        // Window manager checks
        report.addSection("Window Manager")
        debugWindowManager(context, report)

        // Accessibility service specific checks
        if (context is AccessibilityService) {
            report.addSection("Accessibility Service")
            debugAccessibilityService(context, report)
        }

        Log.d(TAG, "Debugging complete:\n${report.toString()}")
        return report
    }

    private fun debugPermissions(context: Context, report: DebugReport) {
        try {
            // Overlay permission
            val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
            report.addItem("Overlay Permission", if (hasOverlay) "✓ GRANTED" else "✗ DENIED")

            // Accessibility permission
            val hasAccessibility = AccessibilityUtils.isAccessibilityServiceEnabled(context)
            report.addItem("Accessibility Permission", if (hasAccessibility) "✓ GRANTED" else "✗ DENIED")

            // Overall status
            val allGranted = hasOverlay && hasAccessibility
            report.addItem("All Permissions", if (allGranted) "✓ GRANTED" else "✗ MISSING")

        } catch (e: Exception) {
            report.addItem("Permission Check Error", e.message ?: "Unknown error")
        }
    }

    private fun debugServiceStatus(context: Context, report: DebugReport) {
        try {
            // Check if running as accessibility service
            val isAccessibilityService = context is AccessibilityService
            report.addItem("Is Accessibility Service", isAccessibilityService.toString())

            if (isAccessibilityService) {
                val service = context as AccessibilityService
                val serviceInfo = service.serviceInfo
                report.addItem("Service Info Available", (serviceInfo != null).toString())

                if (serviceInfo != null) {
                    report.addItem("Event Types", serviceInfo.eventTypes.toString())
                    report.addItem("Feedback Type", serviceInfo.feedbackType.toString())
                    report.addItem("Flags", serviceInfo.flags.toString())
                    report.addItem("Package Names", serviceInfo.packageNames?.joinToString(",") ?: "null")
                }
            }

        } catch (e: Exception) {
            report.addItem("Service Check Error", e.message ?: "Unknown error")
        }
    }

    private fun debugWindowManager(context: Context, report: DebugReport) {
        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            report.addItem("Window Manager Available", (windowManager != null).toString())

            if (windowManager != null) {
                val display = windowManager.defaultDisplay
                report.addItem("Default Display", display.toString())

                val metrics = context.resources.displayMetrics
                report.addItem("Screen Width", metrics.widthPixels.toString())
                report.addItem("Screen Height", metrics.heightPixels.toString())
                report.addItem("Density", metrics.density.toString())
            }

        } catch (e: Exception) {
            report.addItem("Window Manager Error", e.message ?: "Unknown error")
        }
    }

    private fun debugAccessibilityService(service: AccessibilityService, report: DebugReport) {
        try {
            // Service connection status
            report.addItem("Service Connected", "true") // If we can call this, it's connected

            // Root node access
            val rootNode = try {
                service.rootInActiveWindow
            } catch (e: Exception) {
                null
            }
            report.addItem("Root Node Access", (rootNode != null).toString())
            rootNode?.recycle()

        } catch (e: Exception) {
            report.addItem("Accessibility Service Error", e.message ?: "Unknown error")
        }
    }

    /**
     * Test overlay creation capabilities
     */
    fun testOverlayCreation(context: Context): TestResult {
        return try {
            // Basic permission check
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }

            if (!hasPermission) {
                return TestResult.Failure("Overlay permission not granted")
            }

            // Window manager availability
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                ?: return TestResult.Failure("WindowManager not available")

            // Check if we can create layout params
            val layoutParams = WindowManager.LayoutParams(
                1, 1,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                android.graphics.PixelFormat.TRANSLUCENT
            )

            TestResult.Success("Basic overlay setup appears valid")

        } catch (e: Exception) {
            TestResult.Failure("Error testing overlay creation: ${e.message}")
        }
    }

    /**
     * Get recommendations based on debug results
     */
    fun getRecommendations(context: Context): List<String> {
        val recommendations = mutableListOf<String>()

        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            recommendations.add("Grant overlay permission in Settings > Apps > Special access > Display over other apps")
        }

        if (!AccessibilityUtils.isAccessibilityServiceEnabled(context)) {
            recommendations.add("Enable accessibility service in Settings > Accessibility > Downloaded apps")
        }

        // Check service type
        if (context !is AccessibilityService) {
            recommendations.add("Ensure overlay manager is called from accessibility service context")
        }

        // Version-specific recommendations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            recommendations.add("Use TYPE_ACCESSIBILITY_OVERLAY for Android 8.0+")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("All basic checks passed - issue may be timing-related")
            recommendations.add("Try increasing initialization delay")
            recommendations.add("Check logs for specific WindowManager errors")
        }

        return recommendations
    }

    sealed class TestResult {
        data class Success(val message: String) : TestResult()
        data class Failure(val message: String) : TestResult()
    }
}

class DebugReport {
    private val sections = mutableListOf<Section>()
    private var currentSection: Section? = null

    fun addSection(name: String) {
        currentSection = Section(name)
        sections.add(currentSection!!)
    }

    fun addItem(key: String, value: String) {
        currentSection?.items?.add(Item(key, value))
    }

    override fun toString(): String {
        return buildString {
            appendLine("=== OVERLAY DEBUG REPORT ===")
            sections.forEach { section ->
                appendLine("\n[${section.name}]")
                section.items.forEach { item ->
                    appendLine("  ${item.key}: ${item.value}")
                }
            }
            appendLine("\n=== END REPORT ===")
        }
    }

    private data class Section(val name: String, val items: MutableList<Item> = mutableListOf())
    private data class Item(val key: String, val value: String)
}