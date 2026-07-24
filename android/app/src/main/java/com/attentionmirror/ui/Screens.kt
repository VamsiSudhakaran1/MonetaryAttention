package com.attentionmirror.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attentionmirror.R
import com.attentionmirror.data.WeekReport
import com.attentionmirror.domain.AttentionReceipt
import com.attentionmirror.domain.Copy
import com.attentionmirror.domain.Currencies
import com.attentionmirror.domain.Currency
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
            stringResource(R.string.gate_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.gate_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant) { Text(stringResource(R.string.gate_button)) }
    }
}

@Composable
fun HomeScreen(
    receipt: AttentionReceipt?,
    message: DynamicMessage?,
    sessions: List<UsageSession>,
    hourly: List<Long>,
    adDetails: List<com.attentionmirror.domain.AdDetail>,
    currency: Currency,
    dateLabel: String,
    onShare: () -> Unit,
) {
    ScreenColumn {
        Text(
            stringResource(R.string.today_label, dateLabel.uppercase()),
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
            valueRange = Formatting.valueRange(receipt.estimatedValueLowInr, receipt.estimatedValueHighInr, currency),
            returnedLabel = Formatting.money(receipt.userReceivedInr, currency),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MetricTile(
                value = Formatting.minutes(receipt.totalMinutes),
                label = stringResource(R.string.lbl_on_monetized),
                accent = Brand.Sky,
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                value = "${receipt.estimatedAdsSeen}",
                label = stringResource(R.string.lbl_ads_seen),
                accent = Brand.Amber,
                modifier = Modifier.weight(1f),
            )
        }

        val zone = java.time.ZoneId.systemDefault()
        val dayStart = remember {
            java.time.LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        }
        var selectedHour by remember { mutableStateOf(-1) }
        var expandedApp by remember { mutableStateOf<String?>(null) }

        Section(title = stringResource(R.string.sec_where_time)) {
            val maxMinutes = used.maxOfOrNull { it.minutes } ?: 1.0
            used.forEachIndexed { i, p ->
                if (i > 0) HairlineDivider()
                AppUsageRow(
                    packageName = p.packageName,
                    title = p.platform,
                    primary = Formatting.minutes(p.minutes),
                    secondary = if (p.estimatedAdsSeen > 0)
                        "${p.estimatedAdsSeen} ads · ${Formatting.valueRange(p.valueLowInr.toInt(), p.valueHighInr.toInt(), currency)}"
                    else stringResource(R.string.lbl_not_monetized),
                    fraction = (p.minutes / maxMinutes).toFloat(),
                    accent = Brand.Coral,
                )
            }
        }

        Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.IosShare, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_share_home))
        }

        if (hourly.any { it > 0 }) {
            Section(title = stringResource(R.string.sec_hour)) {
                val peak = Timeline.peakHour(hourly.toLongArray())
                BarChart(
                    values = hourly.map { it.toFloat() },
                    highlightIndex = if (selectedHour >= 0) selectedHour else peak,
                    onBarTap = { selectedHour = it },
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
                Spacer(Modifier.height(12.dp))
                if (selectedHour >= 0) {
                    val apps = Timeline.appsInHour(sessions, dayStart, selectedHour)
                        .entries.sortedByDescending { it.value }
                    Text(
                        stringResource(R.string.apps_around, Formatting.hourLabel(selectedHour)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (apps.isEmpty()) {
                        Text(
                            stringResource(R.string.msg_no_apps_hour),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        apps.forEach { (pkg, secs) -> MiniAppRow(pkg, Formatting.durationShort(secs)) }
                    }
                } else {
                    EstimateNote(stringResource(R.string.busiest_around, Formatting.hourLabel(peak)))
                }
            }
        }

        if (adDetails.isNotEmpty()) {
            Section(title = stringResource(R.string.sec_ads_detected)) {
                adDetails.forEachIndexed { i, ad ->
                    if (i > 0) HairlineDivider()
                    AdDetailRow(ad)
                }
                Spacer(Modifier.height(8.dp))
                EstimateNote(stringResource(R.string.note_ads_measured))
            }
        }

        if (sessions.isNotEmpty()) {
            Section(title = stringResource(R.string.sec_sessions)) {
                val grouped = sessions.groupBy { it.packageName }
                    .map { (pkg, list) -> Triple(pkg, list.sumOf { it.durationSeconds }, list) }
                    .sortedByDescending { it.second }
                grouped.forEachIndexed { i, (pkg, totalSeconds, list) ->
                    if (i > 0) HairlineDivider()
                    SessionGroupRow(
                        packageName = pkg,
                        totalSeconds = totalSeconds,
                        count = list.size,
                        expanded = expandedApp == pkg,
                        onToggle = { expandedApp = if (expandedApp == pkg) null else pkg },
                    )
                    if (expandedApp == pkg) {
                        list.sortedByDescending { it.startMillis }.take(12).forEach { SessionDetailRow(it) }
                    }
                }
            }
        }
    }
}

/** Compact icon + name + value row used in the hour drill-down. */
@Composable
private fun MiniAppRow(packageName: String, trailing: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppAvatar(packageName, 28.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                com.attentionmirror.domain.DefaultPlatforms.BY_PACKAGE[packageName]?.platform ?: packageName,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(trailing, style = MaterialTheme.typography.titleMedium)
    }
}

/** Grouped session header: app + total time + count, tappable to expand. */
@Composable
private fun SessionGroupRow(
    packageName: String,
    totalSeconds: Long,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppAvatar(packageName, 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                com.attentionmirror.domain.DefaultPlatforms.BY_PACKAGE[packageName]?.platform ?: packageName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                pluralStringResource(R.plurals.sessions_count, count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(Formatting.durationShort(totalSeconds), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One session line shown under an expanded group. */
@Composable
private fun SessionDetailRow(session: UsageSession) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 48.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            Formatting.clockRange(session.startMillis, session.endMillis),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            Formatting.durationShort(session.durationSeconds),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AdDetailRow(ad: com.attentionmirror.domain.AdDetail) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppAvatar(ad.packageName, 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(ad.platform, style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(R.plurals.ads_count, ad.count, ad.count),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(
                    R.string.ad_stats,
                    ad.adsPerMinute,
                    ad.totalAdSeconds,
                    ad.avgAdSeconds.toInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
    tone: com.attentionmirror.domain.Tone,
    currency: Currency,
    onShare: () -> Unit,
) {
    ScreenColumn {
        if (receipt == null || receipt.totalMinutes <= 0) {
            Text(stringResource(R.string.receipt_title), style = MaterialTheme.typography.headlineSmall)
            EmptyState()
            return@ScreenColumn
        }

        // A polished printed-receipt aesthetic: warm paper, brand mark, barcode.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(PaperBg)
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Box(
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Brand.Coral),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))
            PaperCenter("ATTENTION MIRROR", size = 19.sp, color = PaperInk, bold = true, spacing = 3.sp)
            Spacer(Modifier.height(3.dp))
            PaperCenter(stringResource(R.string.receipt_unpaid), size = 10.sp, color = PaperMuted, spacing = 2.sp)
            Spacer(Modifier.height(2.dp))
            PaperCenter(dateLabel, size = 12.sp, color = PaperMuted)

            Spacer(Modifier.height(18.dp))
            DashedDivider(PaperRule)
            Spacer(Modifier.height(14.dp))

            receipt.perPlatform.filter { it.minutes >= 1.0 }.forEach { p ->
                PaperRow(p.platform, Formatting.minutes(p.minutes))
            }

            Spacer(Modifier.height(14.dp))
            DashedDivider(PaperRule)
            Spacer(Modifier.height(14.dp))

            PaperRow(stringResource(R.string.receipt_ads_seen), "${receipt.estimatedAdsSeen}")
            PaperRow(
                stringResource(R.string.receipt_value_created),
                Formatting.valueRange(receipt.estimatedValueLowInr, receipt.estimatedValueHighInr, currency),
                big = true,
            )
            PaperRow(stringResource(R.string.receipt_returned), Formatting.money(receipt.userReceivedInr, currency), strong = true)

            Spacer(Modifier.height(16.dp))
            DashedDivider(PaperRule)
            Spacer(Modifier.height(16.dp))

            PaperCenter(Copy.conclusion(tone), size = 15.sp, color = Brand.Coral, bold = true)

            Spacer(Modifier.height(20.dp))
            ReceiptBarcode()
            Spacer(Modifier.height(8.dp))
            PaperCenter(stringResource(R.string.receipt_estimated), size = 9.sp, color = PaperMuted, spacing = 1.5.sp)
        }

        HowCalculated(receipt, currency)

        Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.IosShare, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_share_receipt))
        }
    }
}

/** Expandable transparency panel: shows the working behind each estimate. */
@Composable
private fun HowCalculated(receipt: AttentionReceipt, currency: Currency) {
    var expanded by remember { mutableStateOf(false) }
    Section {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.q_how_calculated), style = MaterialTheme.typography.titleMedium)
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
            )
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.calc_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            receipt.perPlatform.filter { it.minutes >= 1.0 }.forEach { p ->
                val config = com.attentionmirror.domain.DefaultPlatforms.BY_PACKAGE[p.packageName]
                val perMin = if (p.minutes > 0) p.estimatedAdsSeen / p.minutes else 0.0
                Spacer(Modifier.height(12.dp))
                Text(p.platform, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.calc_ads, Formatting.minutes(p.minutes), perMin, p.estimatedAdsSeen),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (config != null && p.estimatedAdsSeen > 0) {
                        stringResource(
                            R.string.calc_cpm,
                            Formatting.valueRange(config.lowCpmInr.toInt(), config.highCpmInr.toInt(), currency),
                            Formatting.valueRange(p.valueLowInr.toInt(), p.valueHighInr.toInt(), currency),
                        )
                    } else {
                        stringResource(R.string.calc_adfree, Formatting.money(0, currency))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
private fun PaperRow(label: String, value: String, strong: Boolean = false, big: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = if (big) 7.dp else 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = if (big) 15.sp else 14.sp,
            fontWeight = if (big) FontWeight.SemiBold else FontWeight.Normal,
            color = PaperInk.copy(alpha = 0.7f),
        )
        Text(
            value,
            fontSize = if (big) 20.sp else 14.sp,
            fontWeight = if (strong || big) FontWeight.Bold else FontWeight.Medium,
            color = if (big) Brand.Coral else PaperInk,
        )
    }
}

/** Decorative receipt barcode (no data encoded) — a designed footer flourish. */
@Composable
private fun ReceiptBarcode() {
    val widths = listOf(
        3f, 1f, 2f, 1f, 4f, 1f, 2f, 3f, 1f, 2f, 1f, 3f, 2f, 1f, 4f,
        1f, 2f, 1f, 3f, 1f, 2f, 4f, 1f, 2f, 1f, 3f, 1f, 2f, 3f, 1f,
    )
    Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
        val unit = size.width / widths.sum()
        var x = 0f
        widths.forEachIndexed { i, w ->
            if (i % 2 == 0) {
                drawRect(color = PaperInk, topLeft = Offset(x, 0f), size = Size(w * unit, size.height))
            }
            x += w * unit
        }
    }
}

@Composable
fun ReportsScreen(week: WeekReport?, currency: Currency) {
    ScreenColumn {
        Text(stringResource(R.string.sec_this_week), style = MaterialTheme.typography.headlineSmall)

        if (week == null || week.total.totalMinutes <= 0) {
            EmptyState()
            return@ScreenColumn
        }

        val total = week.total
        val trend = Formatting.percentChange(total.totalMinutes, week.previousTotalMinutes)
        val trendColor = if (trend.startsWith("-")) Brand.Mint else Brand.Coral
        var selectedDay by remember { mutableStateOf(-1) }

        Section {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Formatting.minutes(total.totalMinutes), style = MaterialTheme.typography.displaySmall)
                if (trend != "—") Pill(stringResource(R.string.vs_last_week, trend), trendColor)
            }
            Text(
                stringResource(R.string.lbl_spent_week),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            val values = week.days.map { it.receipt.totalMinutes.toFloat() }
            LabeledBarChart(
                values = values,
                labels = week.days.map { Formatting.dayInitial(it.date) },
                highlightIndex = if (selectedDay >= 0) selectedDay else week.days.lastIndex,
                onBarTap = { selectedDay = it },
                modifier = Modifier.fillMaxWidth(),
            )
            if (selectedDay in week.days.indices) {
                val day = week.days[selectedDay]
                val dayApps = day.receipt.perPlatform.filter { it.minutes >= 1.0 }
                Spacer(Modifier.height(14.dp))
                Text(
                    day.date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM")) +
                        " · ${Formatting.minutes(day.receipt.totalMinutes)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                if (dayApps.isEmpty()) {
                    Text(
                        stringResource(R.string.msg_no_apps_day),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    dayApps.forEach { p -> MiniAppRow(p.packageName, Formatting.minutes(p.minutes)) }
                }
            } else {
                EstimateNote(stringResource(R.string.note_tap_day))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MetricTile(
                value = Formatting.valueRange(total.estimatedValueLowInr, total.estimatedValueHighInr, currency),
                label = stringResource(R.string.lbl_value_created),
                accent = Brand.Coral,
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                value = "${total.estimatedAdsSeen}",
                label = stringResource(R.string.lbl_ads_seen_short),
                accent = Brand.Amber,
                modifier = Modifier.weight(1f),
            )
        }

        Section(title = stringResource(R.string.sec_who_attention)) {
            val used = total.perPlatform.filter { it.minutes >= 1.0 }
            val maxMinutes = used.maxOfOrNull { it.minutes } ?: 1.0
            used.forEachIndexed { i, p ->
                if (i > 0) HairlineDivider()
                AppUsageRow(
                    packageName = p.packageName,
                    title = p.platform,
                    primary = Formatting.minutes(p.minutes),
                    secondary = Formatting.valueRange(p.valueLowInr.toInt(), p.valueHighInr.toInt(), currency),
                    fraction = (p.minutes / maxMinutes).toFloat(),
                    accent = Brand.Coral,
                )
            }
        }

        Section(title = stringResource(R.string.sec_if_keeps)) {
            val yearLow = total.estimatedValueLowInr * 52
            val yearHigh = total.estimatedValueHighInr * 52
            Text(
                Formatting.valueRange(yearLow, yearHigh, currency),
                style = MaterialTheme.typography.displayLarge,
                color = Brand.Coral,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(yearlyLineRes(yearHigh), Formatting.minutes(total.totalMinutes * 52)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EstimateNote(stringResource(R.string.note_projection))
        }
    }
}

/** Yearly-projection sentence (as a string resource) that shifts tone with the
 *  size of the number. The %1$s placeholder is the projected yearly time. */
private fun yearlyLineRes(yearHigh: Int): Int = when {
    yearHigh >= 12000 -> R.string.yearly_line_1
    yearHigh >= 5000 -> R.string.yearly_line_2
    yearHigh >= 1500 -> R.string.yearly_line_3
    else -> R.string.yearly_line_4
}

@Composable
fun SettingsScreen(
    hardTruthMode: Boolean,
    onToggleHardTruth: (Boolean) -> Unit,
    quirkyMode: Boolean,
    onToggleQuirky: (Boolean) -> Unit,
    notificationHour: Int,
    notificationMinute: Int,
    onPickNotificationTime: () -> Unit,
    onSendTestReceipt: () -> Unit,
    currency: Currency,
    onSetCurrency: (String?) -> Unit,
    monetizedPlatforms: List<com.attentionmirror.domain.PlatformConfig>,
    adFreePackages: Set<String>,
    onToggleAdFree: (String, Boolean) -> Unit,
    onMarkAd: (String) -> Unit,
    onAddAdTile: () -> Unit,
    onOpenAdScanner: () -> Unit,
    onSetLanguage: (String) -> Unit,
    currentLanguageTag: String,
) {
    ScreenColumn {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)

        NotificationHealth(onSendTestReceipt)

        Section(title = stringResource(R.string.section_daily_receipt)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(stringResource(R.string.lbl_notif_time), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.desc_notif_time),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onPickNotificationTime) {
                    Text(Formatting.timeOfDay(notificationHour, notificationMinute))
                }
            }
            Spacer(Modifier.height(10.dp))
            HairlineDivider()
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Send one now to check notifications work.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
                Button(onClick = onSendTestReceipt) { Text(stringResource(R.string.btn_test)) }
            }
        }

        Section(title = stringResource(R.string.section_currency)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(stringResource(R.string.lbl_display_currency), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.desc_currency),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { expanded = true }) { Text(currency.code) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        Currencies.ALL.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.code}  ${c.symbol.trim()}") },
                                onClick = {
                                    expanded = false
                                    onSetCurrency(c.code)
                                },
                            )
                        }
                    }
                }
            }
        }

        Section(title = stringResource(R.string.section_language)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(stringResource(R.string.language_label), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.language_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val languages = listOf(
                    "" to stringResource(R.string.lang_system),
                    "en" to "English",
                    "hi" to "हिन्दी",
                    "ta" to "தமிழ்",
                    "te" to "తెలుగు",
                )
                val currentName = languages.firstOrNull { it.first == currentLanguageTag }?.second
                    ?: languages.firstOrNull { it.first.isNotEmpty() && currentLanguageTag.startsWith(it.first) }?.second
                    ?: languages.first().second
                var langExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { langExpanded = true }) { Text(currentName) }
                    DropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                        languages.forEach { (tag, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    langExpanded = false
                                    onSetLanguage(tag)
                                },
                            )
                        }
                    }
                }
            }
        }

        Section(title = stringResource(R.string.section_tone)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(stringResource(R.string.lbl_hard_truth), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.desc_hard_truth),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = hardTruthMode, onCheckedChange = onToggleHardTruth)
            }
            HairlineDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(stringResource(R.string.lbl_quirky), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.desc_quirky),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = quirkyMode, onCheckedChange = onToggleQuirky)
            }
        }

        Section(title = stringResource(R.string.section_adfree)) {
            Text(
                stringResource(R.string.desc_adfree),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            monetizedPlatforms.forEachIndexed { i, p ->
                if (i > 0) HairlineDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppAvatar(p.packageName, 32.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(p.platform, style = MaterialTheme.typography.titleMedium)
                    }
                    Switch(
                        checked = p.packageName in adFreePackages,
                        onCheckedChange = { onToggleAdFree(p.packageName, it) },
                    )
                }
            }
        }

        if (com.attentionmirror.BuildConfig.HAS_AD_SCANNER) {
            Section(title = stringResource(R.string.section_auto_ad)) {
                Text(
                    stringResource(R.string.desc_auto_ad),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Button(onClick = onOpenAdScanner) {
                    Text(stringResource(R.string.btn_open_accessibility))
                }
            }
        }

        Section(title = stringResource(R.string.section_calibrate)) {
            val context = LocalContext.current
            Text(
                stringResource(R.string.desc_calibrate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            monetizedPlatforms.forEachIndexed { i, p ->
                if (i > 0) HairlineDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onMarkAd(p.packageName)
                            Toast.makeText(
                                context,
                                context.getString(R.string.recorded_ad, p.platform),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppAvatar(p.packageName, 32.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(p.platform, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        stringResource(R.string.lbl_plus_ad),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.desc_calibrate_tile),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAddAdTile) { Text(stringResource(R.string.btn_add_tile)) }
        }

        Section(title = stringResource(R.string.section_how_estimate)) {
            Text(
                stringResource(R.string.desc_how_estimate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            "Attention Mirror · v0.1.1",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Shows whether every notification precondition is met, with one-tap fixes. */
@Composable
private fun NotificationHealth(onSendTestReceipt: () -> Unit) {
    val context = LocalContext.current
    val notifier = com.attentionmirror.notification.ReceiptNotifier
    val allowed = notifier.notificationsAllowed(context)
    val channelOn = notifier.channelEnabled(context)
    val batteryOk = run {
        val pm = context.getSystemService(android.os.PowerManager::class.java)
        pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    Section(title = stringResource(R.string.section_notif_health)) {
        StatusRow(stringResource(R.string.health_notifs), allowed)
        HairlineDivider()
        StatusRow(stringResource(R.string.health_channel), channelOn)
        HairlineDivider()
        StatusRow(stringResource(R.string.health_battery), batteryOk)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onSendTestReceipt) { Text(stringResource(R.string.btn_send_test)) }
            OutlinedButton(onClick = {
                context.startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }) { Text(stringResource(R.string.btn_notif_settings)) }
        }
        if (!batteryOk) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                context.startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }) { Text(stringResource(R.string.btn_allow_bg)) }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.health_tip),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Icon(
            if (ok) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = if (ok) stringResource(R.string.cd_ok) else stringResource(R.string.cd_problem),
            tint = if (ok) Brand.Mint else Brand.Coral,
        )
    }
}

@Composable
private fun EmptyState() {
    Section {
        Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
