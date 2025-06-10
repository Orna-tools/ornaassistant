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

// Orna-inspired color scheme
private val OrnaLight = lightColorScheme(
    primary = Color(0xFFFF5722), // Orna orange
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCCBC),
    onPrimaryContainer = Color(0xFFBF360C),
    secondary = Color(0xFFC62828), // Orna red
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFCDD2),
    onSecondaryContainer = Color(0xFFB71C1C),
    tertiary = Color(0xFF558B2F), // Green for success states
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCFF90),
    onTertiaryContainer = Color(0xFF33691E),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A18),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A18),
    surfaceVariant = Color(0xFFF5DDD6),
    onSurfaceVariant = Color(0xFF53443E),
)

private val OrnaDark = darkColorScheme(
    primary = Color(0xFFFF8A65), // Lighter orange for dark theme
    onPrimary = Color(0xFF2E2E2E),
    primaryContainer = Color(0xFFD84315),
    onPrimaryContainer = Color(0xFFFFCCBC),
    secondary = Color(0xFFEF5350), // Lighter red for dark theme
    onSecondary = Color(0xFF2E2E2E),
    secondaryContainer = Color(0xFFC62828),
    onSecondaryContainer = Color(0xFFFFCDD2),
    tertiary = Color(0xFF81C784), // Light green
    onTertiary = Color(0xFF2E2E2E),
    tertiaryContainer = Color(0xFF388E3C),
    onTertiaryContainer = Color(0xFFCCFF90),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF2C2F33), // Discord-like dark background
    onBackground = Color(0xFFECE0DB),
    surface = Color(0xFF2C2F33),
    onSurface = Color(0xFFECE0DB),
    surfaceVariant = Color(0xFF53443E),
    onSurfaceVariant = Color(0xFFD8C2BA),
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
