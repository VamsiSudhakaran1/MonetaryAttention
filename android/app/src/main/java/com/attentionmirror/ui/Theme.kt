@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.attentionmirror.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attentionmirror.R

/**
 * Brand palette. A confident, dark-first identity (the app's whole premise is
 * "the bill nobody shows you"), tuned to read as a polished, modern product
 * rather than a settings screen.
 */
object Brand {
    val Background = Color(0xFF0B0E14)
    val Surface = Color(0xFF141925)
    val SurfaceElevated = Color(0xFF1B2230)
    val Outline = Color(0xFF2A3344)

    val OnSurface = Color(0xFFECEFF4)
    val Muted = Color(0xFF8C97A8)

    val Coral = Color(0xFFFF6168)   // "what others earned" — urgent, not angry
    val Amber = Color(0xFFFFC857)   // value / money
    val Mint = Color(0xFF34D399)    // positive / "good day"
    val Sky = Color(0xFF6FB1FF)     // neutral data accent

    val ValueGradient = Brush.linearGradient(listOf(Color(0xFFFF6168), Color(0xFFFF8A3D)))
    val NightGradient = Brush.verticalGradient(listOf(Color(0xFF161C2B), Color(0xFF10141F)))
}

// Manrope (variable). A clean, friendly geometric sans that stays legible at
// small sizes for older eyes and reads as modern for younger users.
private fun manrope(weight: FontWeight) = Font(
    resId = R.font.manrope,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val Manrope = FontFamily(
    manrope(FontWeight.Normal),
    manrope(FontWeight.Medium),
    manrope(FontWeight.SemiBold),
    manrope(FontWeight.Bold),
)

private val DarkColors = darkColorScheme(
    primary = Brand.Coral,
    onPrimary = Color.White,
    secondary = Brand.Amber,
    tertiary = Brand.Mint,
    background = Brand.Background,
    onBackground = Brand.OnSurface,
    surface = Brand.Surface,
    onSurface = Brand.OnSurface,
    surfaceVariant = Brand.SurfaceElevated,
    onSurfaceVariant = Brand.Muted,
    outline = Brand.Outline,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// Lighter weights, real line-height and gentle tracking — the opposite of the
// chunky, tightly-spaced look that felt dated.
private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 46.sp, lineHeight = 50.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.sp),
)

@Composable
fun AttentionMirrorTheme(content: @Composable () -> Unit) {
    // Force the brand dark theme for a consistent, designed identity.
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
