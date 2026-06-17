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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val Muted = Color(0xFF97A1B2)

    val Coral = Color(0xFFFF5A5F)   // "what others earned" — urgent, not angry
    val Amber = Color(0xFFFFC857)   // value / money
    val Mint = Color(0xFF34D399)    // positive / "good day"
    val Sky = Color(0xFF5AA9FF)     // neutral data accent

    val ValueGradient = Brush.linearGradient(listOf(Color(0xFFFF5A5F), Color(0xFFFF8A3D)))
    val NightGradient = Brush.verticalGradient(listOf(Color(0xFF161C2B), Color(0xFF10141F)))
}

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
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 52.sp, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
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
