package com.attentionmirror.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** An airy section: a quiet uppercase header over content — no surrounding box. */
@Composable
fun Section(
    title: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (title != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                trailing?.invoke()
            }
        }
        content()
    }
}

/** A thin separator used between list rows instead of stacking cards. */
@Composable
fun HairlineDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    )
}

/**
 * The emotional centrepiece: a gradient card showing the value others made from
 * the user's attention, contrasted with the ₹0 they got back.
 */
@Composable
fun ValueHero(valueRange: String, returnedLabel: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brand.ValueGradient)
            .padding(22.dp),
    ) {
        Column {
            Text(
                "ESTIMATED VALUE OTHERS MADE",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                valueRange,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    "Returned to you   $returnedLabel",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
        }
    }
}

/** A flat stat: a coloured figure with a quiet caption. No box. */
@Composable
fun MetricTile(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.displaySmall, color = accent)
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A rounded progress track used in the per-app breakdown. */
@Composable
fun TrackBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

/** App icon + name + usage bar + time/value — the core list row. */
@Composable
fun AppUsageRow(
    packageName: String,
    title: String,
    primary: String,
    secondary: String,
    fraction: Float,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppAvatar(packageName, 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(primary, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            TrackBar(fraction = fraction, color = accent)
            if (secondary.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    secondary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A small rounded badge, e.g. a trend chip like "+24% vs last week". */
@Composable
fun Pill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/** A horizontal dashed rule — used on the paper-style receipt. */
@Composable
fun DashedDivider(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
        )
    }
}

/** Quiet, honest disclaimer line used wherever estimates appear. */
@Composable
fun EstimateNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}
