package com.lloir.ornaassistant.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Custom Orna-themed font family using system fonts
val OrnaFontFamily = FontFamily.SansSerif

// Font scaling factor for accessibility
private const val LARGE_FONT_SCALE_FACTOR = 1.3f

// Function to scale font size for accessibility
private fun scaleFontSize(size: Float, useLargerFontSize: Boolean): Float {
    return if (useLargerFontSize) size * LARGE_FONT_SCALE_FACTOR else size
}

// Function to get scaled typography based on accessibility settings
fun getScaledTypography(useLargerFontSize: Boolean): Typography {
    return Typography(
        // Display styles
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = scaleFontSize(57f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(64f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(-0.25f, useLargerFontSize).sp
        ),
        displayMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = scaleFontSize(45f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(52f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0f, useLargerFontSize).sp
        ),
        displaySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = scaleFontSize(36f, useLargerFontSize).sp,
            lineHeight = scaleFontSize(44f, useLargerFontSize).sp,
            letterSpacing = scaleFontSize(0f, useLargerFontSize).sp
        ),

        // Headline styles - Using Orna font family
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

        // Title styles - Using Orna font family
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

        // Body styles
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

        // Label styles
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
