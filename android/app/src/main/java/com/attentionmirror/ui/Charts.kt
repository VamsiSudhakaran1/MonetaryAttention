package com.attentionmirror.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.unit.dp

/**
 * A lightweight rounded bar chart drawn with Canvas — no chart dependency, so it
 * stays fast and on-brand. [highlightIndex] is drawn in the accent colour.
 */
@Composable
fun BarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    highlightIndex: Int = -1,
    barColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Canvas(modifier) {
        val n = values.size
        if (n == 0) return@Canvas
        val gapRatio = 0.45f
        val barW = size.width / (n + gapRatio * (n + 1))
        val gap = barW * gapRatio
        val radius = CornerRadius(barW / 2f, barW / 2f)
        values.forEachIndexed { i, v ->
            val x = gap + i * (barW + gap)
            // Track (full height, faint) gives the bars structure even when empty.
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
) {
    Column(modifier) {
        BarChart(
            values = values,
            highlightIndex = highlightIndex,
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
