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

// Refined Orna-inspired color scheme
private val OrnaLight = lightColorScheme(
    // Primary - Vibrant Orna orange
    primary = Color(0xFFFF5722),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFFBF360C),

    // Secondary - Orna red, slightly refined
    secondary = Color(0xFFD32F2F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF9A0007),

    // Tertiary - Orna green for success states
    tertiary = Color(0xFF558B2F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD7F2BA),
    onTertiaryContainer = Color(0xFF33691E),

    // Error states
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),

    // Background and surface
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A18),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A18),

    // Variants
    surfaceVariant = Color(0xFFF5DDD6),
    onSurfaceVariant = Color(0xFF53443E),
    outline = Color(0xFF85736D),
    outlineVariant = Color(0xFFD8C2BA),

    // Additional colors
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF362F2D),
    inverseOnSurface = Color(0xFFFBEEE9),
    inversePrimary = Color(0xFFFFB59B)
)

private val OrnaDark = darkColorScheme(
    // Primary - Lighter orange for dark theme
    primary = Color(0xFFFF8A65),
    onPrimary = Color(0xFF2E2E2E),
    primaryContainer = Color(0xFFE64A19),
    onPrimaryContainer = Color(0xFFFFDBCF),

    // Secondary - Lighter red for dark theme
    secondary = Color(0xFFEF5350),
    onSecondary = Color(0xFF2E2E2E),
    secondaryContainer = Color(0xFFB71C1C),
    onSecondaryContainer = Color(0xFFFFDAD6),

    // Tertiary - Light green
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF2E2E2E),
    tertiaryContainer = Color(0xFF2E7D32),
    onTertiaryContainer = Color(0xFFCCFF90),

    // Error states
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),

    // Background and surface - Discord-like dark background
    background = Color(0xFF1E2124),
    onBackground = Color(0xFFECE0DB),
    surface = Color(0xFF2C2F33),
    onSurface = Color(0xFFECE0DB),

    // Variants
    surfaceVariant = Color(0xFF53443E),
    onSurfaceVariant = Color(0xFFD8C2BA),
    outline = Color(0xFF9F8D86),
    outlineVariant = Color(0xFF53443E),

    // Additional colors
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFFBEEE9),
    inverseOnSurface = Color(0xFF362F2D),
    inversePrimary = Color(0xFFBF360C)
)

@Composable
fun OrnaAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> OrnaDark
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
