package com.attentionmirror.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF101418)
private val Accent = Color(0xFFE5484D) // honest, slightly urgent red
private val AccentSoft = Color(0xFF2E7D32)

private val LightColors = lightColorScheme(
    primary = Accent,
    secondary = AccentSoft,
    background = Color(0xFFF7F7F8),
    surface = Color.White,
    onBackground = Ink,
    onSurface = Ink,
)

private val DarkColors = darkColorScheme(
    primary = Accent,
    secondary = AccentSoft,
    background = Ink,
    surface = Color(0xFF181D22),
)

@Composable
fun AttentionMirrorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
