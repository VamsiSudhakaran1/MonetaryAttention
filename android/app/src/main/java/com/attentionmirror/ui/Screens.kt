package com.attentionmirror.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.attentionmirror.domain.AttentionReceipt
import com.attentionmirror.domain.Copy
import com.attentionmirror.domain.Formatting

@Composable
fun PermissionGate(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(
            "See who profited from your scrolling.",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Attention Mirror reads only your aggregate app usage time — never " +
                "your screen, messages, or content. Grant Usage Access to begin.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = onGrant) { Text("Grant Usage Access") }
    }
}

@Composable
fun HomeScreen(receipt: AttentionReceipt?, onViewReceipt: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Text(
            "Today's Unpaid Attention",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        val r = receipt ?: return@Column EmptyDay()
        StatBlock(Formatting.minutes(r.totalMinutes), "spent on monetized platforms")
        StatBlock("${r.estimatedAdsSeen}", "estimated ads seen")
        StatBlock(
            Formatting.valueRange(r.estimatedValueLowInr, r.estimatedValueHighInr),
            "estimated value created",
            emphasize = true,
        )
        StatBlock("₹${r.userReceivedInr}", "returned to you")
        Button(onClick = onViewReceipt, modifier = Modifier.padding(top = 12.dp)) {
            Text("View Receipt")
        }
    }
}

@Composable
fun ReceiptScreen(
    receipt: AttentionReceipt?,
    dateLabel: String,
    hardTruthMode: Boolean,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Text("ATTENTION RECEIPT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(dateLabel, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
        val r = receipt ?: return@Column EmptyDay()

        SectionCard("Time spent") {
            r.perPlatform.forEach {
                PlatformRow(it.platform, Formatting.minutes(it.minutes), "")
            }
        }
        Divider()
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text("Estimated ads seen: ${r.estimatedAdsSeen}")
            Text("Estimated value created: ${Formatting.valueRange(r.estimatedValueLowInr, r.estimatedValueHighInr)}")
            Text("Amount returned to you: ₹${r.userReceivedInr}")
        }
        Text(
            Copy.conclusion(hardTruthMode),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onShare, modifier = Modifier.padding(top = 16.dp)) {
            Text("Share receipt")
        }
    }
}

@Composable
fun SettingsScreen(
    hardTruthMode: Boolean,
    onToggleHardTruth: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        SectionCard("Hard truth mode") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Use stronger wording on your receipt and notification.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Switch(checked = hardTruthMode, onCheckedChange = onToggleHardTruth)
            }
        }

        SectionCard("Calibrate ad counting") {
            Text(
                "Add the \"I saw an ad\" Quick Settings tile, then tap it whenever " +
                    "you spot an ad while scrolling. After ~15 minutes on a platform " +
                    "your real ad frequency replaces our default estimate. Nothing " +
                    "leaves your device.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun WeeklyScreen(week: AttentionReceipt?) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Text("This week", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        val r = week ?: return@Column EmptyDay()
        StatBlock(Formatting.minutes(r.totalMinutes), "spent scrolling")
        StatBlock("${r.estimatedAdsSeen}", "estimated ads seen")
        StatBlock(
            Formatting.valueRange(r.estimatedValueLowInr, r.estimatedValueHighInr),
            "estimated attention value created",
            emphasize = true,
        )
        StatBlock("₹${r.userReceivedInr}", "returned to you")

        // Project a rough yearly figure (52 weeks), kept clearly as an estimate.
        val yearLow = r.estimatedValueLowInr * 52
        val yearHigh = r.estimatedValueHighInr * 52
        Text(
            "At this rate, this habit may create " +
                "${Formatting.valueRange(yearLow, yearHigh)} of monetized attention in a year. " +
                "Estimated.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        SectionCard("Who got your attention?") {
            r.perPlatform.forEach {
                PlatformRow(
                    it.platform,
                    Formatting.minutes(it.minutes),
                    Formatting.valueRange(it.valueLowInr.toInt(), it.valueHighInr.toInt()),
                )
            }
        }
    }
}

@Composable
private fun EmptyDay() {
    Text(
        "No tracked usage yet. Spend some time on a monetized app, then pull to refresh.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Start,
        modifier = Modifier.padding(top = 16.dp),
    )
}
