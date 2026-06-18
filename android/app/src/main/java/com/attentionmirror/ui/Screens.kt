package com.attentionmirror.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attentionmirror.data.WeekReport
import com.attentionmirror.domain.AttentionReceipt
import com.attentionmirror.domain.Copy
import com.attentionmirror.domain.DynamicMessage
import com.attentionmirror.domain.Formatting
import com.attentionmirror.domain.Timeline
import com.attentionmirror.domain.UsageSession

private val ScreenPadding = 20.dp

@Composable
private fun ScreenColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        content()
    }
}

@Composable
fun PermissionGate(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "See who profited from your scrolling.",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Attention Mirror reads only your aggregate app usage time — never " +
                "your screen, messages, or content. Grant Usage Access to begin.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant) { Text("Grant Usage Access") }
    }
}

@Composable
fun HomeScreen(
    receipt: AttentionReceipt?,
    message: DynamicMessage?,
    sessions: List<UsageSession>,
    hourly: List<Long>,
    dateLabel: String,
    onShare: () -> Unit,
) {
    ScreenColumn {
        Text(
            "TODAY · ${dateLabel.uppercase()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (message != null) {
            Text(message.headline, style = MaterialTheme.typography.titleLarge)
            Text(
                message.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (receipt == null || receipt.totalMinutes <= 0) {
            EmptyState()
            return@ScreenColumn
        }

        val used = receipt.perPlatform.filter { it.minutes >= 1.0 }

        ValueHero(
            valueRange = Formatting.valueRange(receipt.estimatedValueLowInr, receipt.estimatedValueHighInr),
            returnedLabel = "₹${receipt.userReceivedInr}",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MetricTile(
                value = Formatting.minutes(receipt.totalMinutes),
                label = "on monetized apps",
                accent = Brand.Sky,
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                value = "${receipt.estimatedAdsSeen}",
                label = "estimated ads seen",
                accent = Brand.Amber,
                modifier = Modifier.weight(1f),
            )
        }

        Section(title = "Where your time went") {
            val maxMinutes = used.maxOfOrNull { it.minutes } ?: 1.0
            used.forEachIndexed { i, p ->
                if (i > 0) HairlineDivider()
                AppUsageRow(
                    packageName = p.packageName,
                    title = p.platform,
                    primary = Formatting.minutes(p.minutes),
                    secondary = if (p.estimatedAdsSeen > 0)
                        "${p.estimatedAdsSeen} ads · ${Formatting.valueRange(p.valueLowInr.toInt(), p.valueHighInr.toInt())}"
                    else "time only · not monetized",
                    fraction = (p.minutes / maxMinutes).toFloat(),
                    accent = Brand.Coral,
                )
            }
        }

        if (hourly.any { it > 0 }) {
            Section(title = "Your day, hour by hour") {
                val peak = Timeline.peakHour(hourly.toLongArray())
                BarChart(
                    values = hourly.map { it.toFloat() },
                    highlightIndex = peak,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("12a", "6a", "12p", "6p", "11p").forEach {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                EstimateNote("Busiest around ${Formatting.hourLabel(peak)}.")
            }
        }

        if (sessions.isNotEmpty()) {
            Section(title = "Sessions today") {
                sessions.sortedByDescending { it.startMillis }.take(8).forEachIndexed { i, s ->
                    if (i > 0) HairlineDivider()
                    SessionRow(s)
                }
            }
        }

        Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.IosShare, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Share my attention receipt")
        }
    }
}

@Composable
private fun SessionRow(session: UsageSession) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppAvatar(session.packageName, 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                com.attentionmirror.domain.DefaultPlatforms.BY_PACKAGE[session.packageName]?.platform
                    ?: session.packageName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                Formatting.clockRange(session.startMillis, session.endMillis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(Formatting.durationShort(session.durationSeconds), style = MaterialTheme.typography.titleMedium)
    }
}

private val PaperBg = Color(0xFFF6F1E7)
private val PaperInk = Color(0xFF1B1B1B)
private val PaperMuted = Color(0xFF6E6A5F)
private val PaperRule = Color(0x331B1B1B)

@Composable
fun ReceiptScreen(
    receipt: AttentionReceipt?,
    dateLabel: String,
    hardTruthMode: Boolean,
    onShare: () -> Unit,
) {
    ScreenColumn {
        if (receipt == null || receipt.totalMinutes <= 0) {
            Text("Attention Receipt", style = MaterialTheme.typography.headlineSmall)
            EmptyState()
            return@ScreenColumn
        }

        // A printed-receipt aesthetic: warm paper, monospace, dashed rules.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(PaperBg)
                .padding(horizontal = 22.dp, vertical = 26.dp),
        ) {
            PaperCenter("ATTENTION MIRROR", size = 18.sp, color = PaperInk, bold = true, spacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            PaperCenter("· unpaid attention receipt ·", size = 12.sp, color = PaperMuted)
            PaperCenter(dateLabel, size = 12.sp, color = PaperMuted)

            Spacer(Modifier.height(16.dp))
            DashedDivider(PaperRule)
            Spacer(Modifier.height(12.dp))

            receipt.perPlatform.filter { it.minutes >= 1.0 }.forEach { p ->
                PaperRow(p.platform, Formatting.minutes(p.minutes))
            }

            Spacer(Modifier.height(12.dp))
            DashedDivider(PaperRule)
            Spacer(Modifier.height(12.dp))

            PaperRow("Ads seen", "${receipt.estimatedAdsSeen}")
            PaperRow(
                "Value created",
                Formatting.valueRange(receipt.estimatedValueLowInr, receipt.estimatedValueHighInr),
            )
            PaperRow("Returned to you", "₹${receipt.userReceivedInr}", strong = true)

            Spacer(Modifier.height(12.dp))
            DashedDivider(PaperRule)
            Spacer(Modifier.height(14.dp))

            PaperCenter(Copy.conclusion(hardTruthMode), size = 14.sp, color = Brand.Coral, bold = true)
            Spacer(Modifier.height(14.dp))
            PaperCenter("* all values are estimates *", size = 11.sp, color = PaperMuted)
        }

        Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.IosShare, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Share receipt")
        }
    }
}

@Composable
private fun PaperCenter(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    color: Color,
    bold: Boolean = false,
    spacing: androidx.compose.ui.unit.TextUnit = 0.sp,
) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = size,
        letterSpacing = spacing,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = color,
    )
}

@Composable
private fun PaperRow(label: String, value: String, strong: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = PaperInk.copy(alpha = 0.7f),
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Medium,
            color = PaperInk,
        )
    }
}

@Composable
fun ReportsScreen(week: WeekReport?) {
    ScreenColumn {
        Text("This week", style = MaterialTheme.typography.headlineSmall)

        if (week == null || week.total.totalMinutes <= 0) {
            EmptyState()
            return@ScreenColumn
        }

        val total = week.total
        val trend = Formatting.percentChange(total.totalMinutes, week.previousTotalMinutes)
        val trendColor = if (trend.startsWith("-")) Brand.Mint else Brand.Coral

        Section {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Formatting.minutes(total.totalMinutes), style = MaterialTheme.typography.displaySmall)
                if (trend != "—") Pill("$trend vs last week", trendColor)
            }
            Text(
                "spent scrolling this week",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            val values = week.days.map { it.receipt.totalMinutes.toFloat() }
            LabeledBarChart(
                values = values,
                labels = week.days.map { Formatting.dayInitial(it.date) },
                highlightIndex = week.days.lastIndex,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MetricTile(
                value = Formatting.valueRange(total.estimatedValueLowInr, total.estimatedValueHighInr),
                label = "value created",
                accent = Brand.Coral,
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                value = "${total.estimatedAdsSeen}",
                label = "ads seen",
                accent = Brand.Amber,
                modifier = Modifier.weight(1f),
            )
        }

        Section(title = "Who got your attention?") {
            val used = total.perPlatform.filter { it.minutes >= 1.0 }
            val maxMinutes = used.maxOfOrNull { it.minutes } ?: 1.0
            used.forEachIndexed { i, p ->
                if (i > 0) HairlineDivider()
                AppUsageRow(
                    packageName = p.packageName,
                    title = p.platform,
                    primary = Formatting.minutes(p.minutes),
                    secondary = Formatting.valueRange(p.valueLowInr.toInt(), p.valueHighInr.toInt()),
                    fraction = (p.minutes / maxMinutes).toFloat(),
                    accent = Brand.Coral,
                )
            }
        }

        Section(title = "If this keeps up") {
            val yearLow = total.estimatedValueLowInr * 52
            val yearHigh = total.estimatedValueHighInr * 52
            Text(
                Formatting.valueRange(yearLow, yearHigh),
                style = MaterialTheme.typography.displaySmall,
                color = Brand.Coral,
            )
            Text(
                "of monetized attention you may create in a year — and ₹0 of it returns to you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EstimateNote("Projection from this week. Estimated, not a forecast.")
        }
    }
}

@Composable
fun SettingsScreen(
    hardTruthMode: Boolean,
    onToggleHardTruth: (Boolean) -> Unit,
) {
    ScreenColumn {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        Section {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Hard truth mode", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Stronger wording on your receipt and notification. The numbers never change.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = hardTruthMode, onCheckedChange = onToggleHardTruth)
            }
        }

        Section(title = "Calibrate ad counting") {
            Text(
                "Add the \"I saw an ad\" Quick Settings tile, then tap it whenever " +
                    "you spot an ad while scrolling. After ~15 minutes on a platform " +
                    "your real ad frequency replaces our default estimate. Nothing " +
                    "leaves your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Section(title = "How we estimate") {
            Text(
                "We read only aggregate app usage time — never screen content, " +
                    "messages, or ads themselves. Value is a transparent estimate from " +
                    "typical ad load and CPM ranges, shown as a range to stay honest.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            "Attention Mirror · v0.1.0",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun EmptyState() {
    Section {
        Text("No tracked usage yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Spend some time on a monetized app, then return here. Your receipt builds automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
