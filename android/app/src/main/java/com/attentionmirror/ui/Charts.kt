package com.attentionmirror.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private const val GAP_RATIO = 0.45f

/** Map a tap x-position (px) to a bar index using the chart's layout math. */
private fun barIndexAt(x: Float, widthPx: Float, n: Int): Int {
    val barW = widthPx / (n + GAP_RATIO * (n + 1))
    val gap = barW * GAP_RATIO
    return ((x - gap) / (barW + gap)).toInt().coerceIn(0, n - 1)
}

/**
 * A lightweight rounded bar chart drawn with Canvas — no chart dependency, so it
 * stays fast and on-brand. [highlightIndex] is drawn in the accent colour;
 * [onBarTap] (when set) reports the tapped bar index.
 */
@Composable
fun BarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    highlightIndex: Int = -1,
    barColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onBarTap: ((Int) -> Unit)? = null,
) {
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    val n = values.size
    val tap = if (onBarTap != null && n > 0) {
        Modifier.pointerInput(n) {
            detectTapGestures { offset -> onBarTap(barIndexAt(offset.x, size.width.toFloat(), n)) }
        }
    } else {
        Modifier
    }
    Canvas(modifier.then(tap)) {
        if (n == 0) return@Canvas
        val barW = size.width / (n + GAP_RATIO * (n + 1))
        val gap = barW * GAP_RATIO
        val radius = CornerRadius(barW / 2f, barW / 2f)
        values.forEachIndexed { i, v ->
            val x = gap + i * (barW + gap)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(x, 0f),
                size = Size(barW, size.height),
                cornerRadius = radius,
            )
            val h = (size.height * (v / max)).coerceAtLeast(if (v > 0f) barW else 0f)
            if (h > 0f) {
                drawRoundRect(
                    color = if (i == highlightIndex) barColor else barColor.copy(alpha = 0.55f),
                    topLeft = Offset(x, size.height - h),
                    size = Size(barW, h),
                    cornerRadius = radius,
                )
            }
        }
    }
}

/** Bar chart with a single caption row of labels beneath, evenly spaced. */
@Composable
fun LabeledBarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    highlightIndex: Int = -1,
    height: Int = 120,
    onBarTap: ((Int) -> Unit)? = null,
) {
    Column(modifier) {
        BarChart(
            values = values,
            highlightIndex = highlightIndex,
            onBarTap = onBarTap,
            modifier = Modifier.fillMaxWidth().height(height.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEachIndexed { i, label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (i == highlightIndex) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
