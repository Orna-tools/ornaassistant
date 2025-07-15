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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.lloir.ornaassistant.domain.model.ColorBlindnessType
import com.lloir.ornaassistant.domain.model.ThemeType
import com.lloir.ornaassistant.utils.AccessibilityUtils

// Premium features have been removed

// Enhanced Orna-inspired color scheme
private val OrnaLight = lightColorScheme(
    // Primary - Vibrant Orna orange with better contrast
    primary = Color(0xFFE64A19),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3E1200),

    // Secondary - Deeper red for better visual hierarchy
    secondary = Color(0xFFC62828),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF410001),

    // Tertiary - Richer green for success states
    tertiary = Color(0xFF2E7D32),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFAEE9A0),
    onTertiaryContainer = Color(0xFF002106),

    // Error states
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),

    // Background and surface - Warmer tones
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF201A17),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A17),

    // Variants with better contrast
    surfaceVariant = Color(0xFFF5DFD7),
    onSurfaceVariant = Color(0xFF53433E),
    outline = Color(0xFF85736D),
    outlineVariant = Color(0xFFD8C2BA),

    // Additional colors
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF362F2D),
    inverseOnSurface = Color(0xFFFBEEE9),
    inversePrimary = Color(0xFFFFB59B)
)

private val OrnaDark = darkColorScheme(
    // Primary - Brighter orange for dark theme visibility
    primary = Color(0xFFFF7D47),
    onPrimary = Color(0xFF2E1500),
    primaryContainer = Color(0xFF872100),
    onPrimaryContainer = Color(0xFFFFDBCF),

    // Secondary - Brighter red for dark theme
    secondary = Color(0xFFFF6B6B),
    onSecondary = Color(0xFF690002),
    secondaryContainer = Color(0xFF930003),
    onSecondaryContainer = Color(0xFFFFDAD6),

    // Tertiary - Brighter green for dark theme
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF00390F),
    tertiaryContainer = Color(0xFF005317),
    onTertiaryContainer = Color(0xFFAEE9A0),

    // Background and surface - Deeper, richer dark theme
    background = Color(0xFF1A1717),
    onBackground = Color(0xFFEDE0DB),
    surface = Color(0xFF252121),
    onSurface = Color(0xFFEDE0DB),

    // Variants with better contrast
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BA),
    outline = Color(0xFF9F8D86),
    outlineVariant = Color(0xFF534341)
)

// Premium themes have been removed

@Composable
fun OrnaAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    useLargerFontSize: Boolean = false,
    fontScaleLevel: com.lloir.ornaassistant.domain.model.FontScaleLevel = com.lloir.ornaassistant.domain.model.FontScaleLevel.MEDIUM,
    useHighContrastMode: Boolean = false,
    useAmoledDarkMode: Boolean = false,
    enhancedDarkModeContrast: Boolean = false,
    colorBlindnessType: ColorBlindnessType = ColorBlindnessType.NONE,
    useColorBlindnessSimulation: Boolean = false,
    usePatternSupplements: Boolean = false,
    enableKeyboardNavigation: Boolean = false,
    enableKeyboardShortcuts: Boolean = false,
    enhanceFocusIndicators: Boolean = false,
    themeType: ThemeType = ThemeType.DEFAULT,
    content: @Composable () -> Unit
) {
    // Theme

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
            // Don't set status bar color here - use enableEdgeToEdge() in MainActivity instead
            // window.statusBarColor is deprecated in Android 15
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Apply theme modifications based on settings
    val finalColorScheme = when {
        // High contrast mode takes precedence
        useHighContrastMode -> {
            if (darkTheme) {
                colorScheme.copy(
                    // Increase contrast for dark theme
                    background = Color.Black,
                    surface = Color(0xFF121212),
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onPrimary = Color.Black,
                    onSecondary = Color.Black,
                    onTertiary = Color.Black
                )
            } else {
                colorScheme.copy(
                    // Increase contrast for light theme
                    background = Color.White,
                    surface = Color.White,
                    onBackground = Color.Black,
                    onSurface = Color.Black,
                    primary = Color(0xFFD84315), // Darker orange for better contrast
                    secondary = Color(0xFFC62828) // Darker red for better contrast
                )
            }
        }
        // AMOLED dark mode (true black for battery savings)
        darkTheme && useAmoledDarkMode -> {
            colorScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceVariant = Color(0xFF121212),
                surfaceTint = Color.Black
            )
        }
        // Enhanced dark mode contrast
        darkTheme && enhancedDarkModeContrast -> {
            colorScheme.copy(
                // Increase contrast between elements
                background = Color(0xFF000000),
                surface = Color(0xFF121212),
                surfaceVariant = Color(0xFF252525),
                onBackground = Color.White,
                onSurface = Color.White,
                onSurfaceVariant = Color(0xFFF0F0F0),
                primary = Color(0xFFFF9E80),      // Brighter orange
                secondary = Color(0xFFFF8A80),    // Brighter red
                tertiary = Color(0xFFB9F6CA),     // Brighter green
                outline = Color(0xFFDADADA)       // Brighter outline
            )
        }
        // Default color scheme
        else -> colorScheme
    }

    // Apply color blindness transformations if enabled
    val colorBlindnessAdjustedScheme = if (useColorBlindnessSimulation && colorBlindnessType != ColorBlindnessType.NONE) {
        finalColorScheme.copy(
            // Transform primary colors
            primary = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.primary, colorBlindnessType),
            onPrimary = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.onPrimary, colorBlindnessType),
            primaryContainer = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.primaryContainer, colorBlindnessType),
            onPrimaryContainer = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.onPrimaryContainer, colorBlindnessType),

            // Transform secondary colors
            secondary = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.secondary, colorBlindnessType),
            onSecondary = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.onSecondary, colorBlindnessType),
            secondaryContainer = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.secondaryContainer, colorBlindnessType),
            onSecondaryContainer = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.onSecondaryContainer, colorBlindnessType),

            // Transform tertiary colors
            tertiary = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.tertiary, colorBlindnessType),
            onTertiary = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.onTertiary, colorBlindnessType),
            tertiaryContainer = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.tertiaryContainer, colorBlindnessType),
            onTertiaryContainer = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.onTertiaryContainer, colorBlindnessType),

            // Transform error colors
            error = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.error, colorBlindnessType),
            onError = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.onError, colorBlindnessType),
            errorContainer = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.errorContainer, colorBlindnessType),
            onErrorContainer = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.onErrorContainer, colorBlindnessType),

            // Transform background and surface colors
            background = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.background, colorBlindnessType),
            onBackground = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.onBackground, colorBlindnessType),
            surface = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.surface, colorBlindnessType),
            onSurface = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.onSurface, colorBlindnessType),

            // Transform variant colors
            surfaceVariant = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.surfaceVariant, colorBlindnessType),
            onSurfaceVariant = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.onSurfaceVariant, colorBlindnessType),
            outline = AccessibilityUtils.transformColorForColorBlindness(finalColorScheme.outline, colorBlindnessType)
        )
    } else {
        finalColorScheme
    }

    // Apply focus indicator enhancements if enabled
    val focusColor = if (enhanceFocusIndicators) {
        AccessibilityUtils.getFocusIndicatorColor(
            colorBlindnessAdjustedScheme.primary,
            enhanceFocusIndicators
        )
    } else {
        colorBlindnessAdjustedScheme.primary
    }

    // Create a custom Material Theme with enhanced focus indicators
    MaterialTheme(
        colorScheme = colorBlindnessAdjustedScheme.copy(
            // Use enhanced focus color for primary (used for focus indicators)
            primary = focusColor
        ),
        typography = getScaledTypography(fontScaleLevel),
        content = content
    )
}
