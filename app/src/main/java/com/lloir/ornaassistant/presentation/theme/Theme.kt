package com.lloir.ornaassistant.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Enhanced Orna-inspired color scheme
private val OrnaLight = lightColorScheme(
    primary = Color(0xFFFF5722),         // Vibrant orange
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCCBC), // Light orange container
    onPrimaryContainer = Color(0xFFBF360C), // Dark orange text

    secondary = Color(0xFFC62828),       // Rich red
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFCDD2), // Light red container
    onSecondaryContainer = Color(0xFFB71C1C), // Dark red text

    tertiary = Color(0xFF558B2F),        // Forest green
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCFF90), // Light green container
    onTertiaryContainer = Color(0xFF33691E), // Dark green text

    error = Color(0xFFBA1A1A),           // Error red
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFFFFBFF),      // Clean white background
    onBackground = Color(0xFF201A18),

    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A18),

    surfaceVariant = Color(0xFFF5DDD6),  // Subtle orange tint
    onSurfaceVariant = Color(0xFF53443E),

    // Additional customizations for a more polished look
    outline = Color(0xFFE0E0E0),         // Subtle outline
    outlineVariant = Color(0xFFCCCCCC),
    scrim = Color(0x52000000),           // Semi-transparent scrim
)

private val OrnaDark = darkColorScheme(
    primary = Color(0xFFFF8A65),         // Lighter orange for dark theme
    onPrimary = Color(0xFF2E2E2E),
    primaryContainer = Color(0xFF9A3412), // Deeper orange container
    onPrimaryContainer = Color(0xFFFFCCBC), // Light orange text

    secondary = Color(0xFFEF5350),       // Vibrant red for dark theme
    onSecondary = Color(0xFF2E2E2E),
    secondaryContainer = Color(0xFF7F1D1D), // Deeper red container
    onSecondaryContainer = Color(0xFFFFCDD2), // Light red text

    tertiary = Color(0xFF81C784),        // Vibrant green
    onTertiary = Color(0xFF2E2E2E),
    tertiaryContainer = Color(0xFF1B4D3E), // Deep green container
    onTertiaryContainer = Color(0xFFCCFF90), // Light green text

    error = Color(0xFFFFB4AB),           // Lighter error for dark theme
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF1A1C1E),      // Rich dark background
    onBackground = Color(0xFFECE0DB),

    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFECE0DB),

    surfaceVariant = Color(0xFF2C2F33),  // Slightly lighter surface variant
    onSurfaceVariant = Color(0xFFD8C2BA),

    // Additional customizations for a more polished look
    outline = Color(0xFF3F4042),         // Subtle outline for dark theme
    outlineVariant = Color(0xFF2A2C2E),
    scrim = Color(0x99000000),           // Darker scrim for dark theme
)

@Composable
fun OrnaAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themeMode: com.lloir.ornaassistant.domain.model.ThemeMode = com.lloir.ornaassistant.domain.model.ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    // Determine if dark theme should be used based on the theme mode
    val isDarkTheme = when (themeMode) {
        com.lloir.ornaassistant.domain.model.ThemeMode.LIGHT -> false
        com.lloir.ornaassistant.domain.model.ThemeMode.DARK -> true
        com.lloir.ornaassistant.domain.model.ThemeMode.SYSTEM -> darkTheme
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDarkTheme -> OrnaDark
        else -> OrnaLight
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Always use transparent status bar for edge-to-edge compatibility (mandatory in Android 16)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.statusBarColor = colorScheme.primary.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
