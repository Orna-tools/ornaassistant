package com.lloir.ornaassistant.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Custom Orna-themed font family using system fonts
val OrnaFontFamily = FontFamily.SansSerif

// Font scaling factors for accessibility
private const val SMALL_FONT_SCALE_FACTOR = 0.9f
private const val MEDIUM_FONT_SCALE_FACTOR = 1.0f
private const val LARGE_FONT_SCALE_FACTOR = 1.3f
private const val EXTRA_LARGE_FONT_SCALE_FACTOR = 1.6f
private const val HUGE_FONT_SCALE_FACTOR = 2.0f

// Function to scale font size for accessibility based on FontScaleLevel
private fun scaleFontSize(size: Float, fontScaleLevel: com.lloir.ornaassistant.domain.model.FontScaleLevel): Float {
    return when (fontScaleLevel) {
        com.lloir.ornaassistant.domain.model.FontScaleLevel.SMALL -> size * SMALL_FONT_SCALE_FACTOR
        com.lloir.ornaassistant.domain.model.FontScaleLevel.MEDIUM -> size * MEDIUM_FONT_SCALE_FACTOR
        com.lloir.ornaassistant.domain.model.FontScaleLevel.LARGE -> size * LARGE_FONT_SCALE_FACTOR
        com.lloir.ornaassistant.domain.model.FontScaleLevel.EXTRA_LARGE -> size * EXTRA_LARGE_FONT_SCALE_FACTOR
        com.lloir.ornaassistant.domain.model.FontScaleLevel.HUGE -> size * HUGE_FONT_SCALE_FACTOR
    }
}

// Legacy function for backward compatibility
private fun scaleFontSize(size: Float, useLargerFontSize: Boolean): Float {
    return if (useLargerFontSize) size * LARGE_FONT_SCALE_FACTOR else size
}

// Enhanced function to get scaled typography based on FontScaleLevel
fun getScaledTypography(fontScaleLevel: com.lloir.ornaassistant.domain.model.FontScaleLevel): Typography {
    return Typography(
        // Display styles with more character
        displayLarge = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaleFontSize(57f, fontScaleLevel).sp,
            lineHeight = scaleFontSize(64f, fontScaleLevel).sp,
            letterSpacing = scaleFontSize(-0.25f, fontScaleLevel).sp
        ),
        displayMedium = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaleFontSize(45f, fontScaleLevel).sp,
            lineHeight = scaleFontSize(52f, fontScaleLevel).sp,
            letterSpacing = scaleFontSize(0f, fontScaleLevel).sp
        ),
        displaySmall = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaleFontSize(36f, fontScaleLevel).sp,
            lineHeight = scaleFontSize(44f, fontScaleLevel).sp,
            letterSpacing = scaleFontSize(0f, fontScaleLevel).sp
        ),

        // Headline styles with more personality
        headlineLarge = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaleFontSize(32f, fontScaleLevel).sp,
            lineHeight = scaleFontSize(40f, fontScaleLevel).sp,
            letterSpacing = scaleFontSize(0f, fontScaleLevel).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaleFontSize(28f, fontScaleLevel).sp,
            lineHeight = scaleFontSize(36f, fontScaleLevel).sp,
            letterSpacing = scaleFontSize(0f, fontScaleLevel).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaleFontSize(24f, fontScaleLevel).sp,
            lineHeight = scaleFontSize(32f, fontScaleLevel).sp,
            letterSpacing = scaleFontSize(0f, fontScaleLevel).sp
        ),

        // Title styles with better spacing
        titleLarge = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaleFontSize(22f, fontScaleLevel).sp,
            lineHeight = scaleFontSize(28f, fontScaleLevel).sp,
            letterSpacing = scaleFontSize(0f, fontScaleLevel).sp
        ),
        titleMedium = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaleFontSize(16f, fontScaleLevel).sp,
            lineHeight = scaleFontSize(24f, fontScaleLevel).sp,
            letterSpacing = scaleFontSize(0.15f, fontScaleLevel).sp
        ),
        titleSmall = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = scaleFontSize(14f, fontScaleLevel).sp,
            lineHeight = scaleFontSize(20f, fontScaleLevel).sp,
            letterSpacing = scaleFontSize(0.1f, fontScaleLevel).sp
        ),

        // Body styles with improved readability
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = scaleFontSize(16f, fontScaleLevel).sp,
            lineHeight = scaleFontSize(24f, fontScaleLevel).sp,
            letterSpacing = scaleFontSize(0.5f, fontScaleLevel).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = scaleFontSize(14f, fontScaleLevel).sp,
            lineHeight = scaleFontSize(20f, fontScaleLevel).sp,
            letterSpacing = scaleFontSize(0.25f, fontScaleLevel).sp
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = scaleFontSize(12f, fontScaleLevel).sp,
            lineHeight = scaleFontSize(16f, fontScaleLevel).sp,
            letterSpacing = scaleFontSize(0.4f, fontScaleLevel).sp
        ),

        // Label styles with better distinction
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = scaleFontSize(14f, fontScaleLevel).sp,
            lineHeight = scaleFontSize(20f, fontScaleLevel).sp,
            letterSpacing = scaleFontSize(0.1f, fontScaleLevel).sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = scaleFontSize(12f, fontScaleLevel).sp,
            lineHeight = scaleFontSize(16f, fontScaleLevel).sp,
            letterSpacing = scaleFontSize(0.5f, fontScaleLevel).sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = scaleFontSize(11f, fontScaleLevel).sp,
            lineHeight = scaleFontSize(16f, fontScaleLevel).sp,
            letterSpacing = scaleFontSize(0.5f, fontScaleLevel).sp
        )
    )
}

// Legacy function for backward compatibility
fun getScaledTypography(useLargerFontSize: Boolean): Typography {
    return Typography(
        // Display styles with more character
        displayLarge = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaleFontSize(57f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(64f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(-0.25f, useLargerFontSize).sp
        ),
        displayMedium = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaleFontSize(45f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(52f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0f, useLargerFontSize).sp
        ),
        displaySmall = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaleFontSize(36f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(44f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0f, useLargerFontSize).sp
        ),

        // Headline styles with more personality
        headlineLarge = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaleFontSize(32f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(40f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0f, useLargerFontSize).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaleFontSize(28f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(36f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0f, useLargerFontSize).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaleFontSize(24f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(32f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0f, useLargerFontSize).sp
        ),

        // Title styles with better spacing
        titleLarge = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaleFontSize(22f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(28f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0f, useLargerFontSize).sp
        ),
        titleMedium = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaleFontSize(16f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(24f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0.15f, useLargerFontSize).sp
        ),
        titleSmall = TextStyle(
            fontFamily = OrnaFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = scaleFontSize(14f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(20f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0.1f, useLargerFontSize).sp
        ),

        // Body styles with improved readability
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = scaleFontSize(16f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(24f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0.5f, useLargerFontSize).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = scaleFontSize(14f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(20f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0.25f, useLargerFontSize).sp
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = scaleFontSize(12f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(16f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0.4f, useLargerFontSize).sp
        ),

        // Label styles with better distinction
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = scaleFontSize(14f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(20f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0.1f, useLargerFontSize).sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = scaleFontSize(12f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(16f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0.5f, useLargerFontSize).sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = scaleFontSize(11f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(16f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0.5f, useLargerFontSize).sp
        )
    )
}

// Default typography with no scaling
val Typography = getScaledTypography(false)
