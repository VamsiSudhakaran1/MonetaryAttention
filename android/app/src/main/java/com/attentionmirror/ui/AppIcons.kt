package com.attentionmirror.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.attentionmirror.domain.DefaultPlatforms

/** Brand-ish fallback colours used when an app's launcher icon isn't available. */
private val BrandColors = mapOf(
    "com.google.android.youtube" to Color(0xFFFF0000),
    "com.facebook.katana" to Color(0xFF1877F2),
    "com.instagram.android" to Color(0xFFE1306C),
    "com.twitter.android" to Color(0xFF1D9BF0),
    "com.reddit.frontpage" to Color(0xFFFF4500),
    "com.snapchat.android" to Color(0xFFFFFC00),
    "in.mohalla.sharechat" to Color(0xFF00AA63),
    "in.mohalla.video" to Color(0xFFFF2D55),
    "com.eterno.shortvideos" to Color(0xFFEF3E56),
    "com.android.chrome" to Color(0xFF4285F4),
    "com.whatsapp" to Color(0xFF25D366),
)

private fun brandColor(packageName: String): Color =
    BrandColors[packageName] ?: Color(0xFF5AA9FF)

@Composable
private fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName)
                .toBitmap(width = 144, height = 144)
                .asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

/** Real launcher icon, or a coloured monogram tile if the app isn't installed. */
@Composable
fun AppAvatar(packageName: String, size: Dp = 44.dp) {
    val shape = RoundedCornerShape(size / 3)
    val icon = rememberAppIcon(packageName)
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier.size(size).clip(shape),
        )
    } else {
        val letter = (DefaultPlatforms.BY_PACKAGE[packageName]?.platform ?: "?").take(1).uppercase()
        Box(
            modifier = Modifier.size(size).clip(shape).background(brandColor(packageName)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                letter,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp,
            )
        }
    }
}
