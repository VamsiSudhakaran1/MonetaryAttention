package com.attentionmirror.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Reports("Reports", Icons.Filled.BarChart),
    Receipt("Receipt", Icons.Filled.ReceiptLong),
    Settings("Settings", Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttentionApp(
    state: UiState,
    onGrantAccess: () -> Unit,
    onShareReceipt: () -> Unit,
    onToggleHardTruth: (Boolean) -> Unit,
    onToggleQuirky: (Boolean) -> Unit,
    onPickNotificationTime: () -> Unit,
    onToggleAdFree: (String, Boolean) -> Unit,
    onOpenAdScanner: () -> Unit,
) {
    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!state.hasUsageAccess) {
        PermissionGate(onGrant = onGrantAccess)
        return
    }

    var tab by remember { mutableIntStateOf(0) }
    val dateLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val current = Tab.entries[tab]
                    Text(if (current == Tab.Home) "Attention Mirror" else current.label)
                },
                actions = {
                    if (Tab.entries[tab] == Tab.Home && state.today != null) {
                        IconButton(onClick = onShareReceipt) {
                            Icon(Icons.Filled.IosShare, contentDescription = "Share")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, t ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (Tab.entries[tab]) {
                Tab.Home -> HomeScreen(
                    receipt = state.today,
                    message = state.message,
                    sessions = state.sessions,
                    hourly = state.hourly,
                    adDetails = state.adDetails,
                    dateLabel = dateLabel,
                    onShare = onShareReceipt,
                )
                Tab.Reports -> ReportsScreen(week = state.week)
                Tab.Receipt -> ReceiptScreen(
                    receipt = state.today,
                    dateLabel = dateLabel,
                    tone = com.attentionmirror.domain.Copy.toneOf(state.hardTruthMode, state.quirkyMode),
                    onShare = onShareReceipt,
                )
                Tab.Settings -> SettingsScreen(
                    hardTruthMode = state.hardTruthMode,
                    onToggleHardTruth = onToggleHardTruth,
                    quirkyMode = state.quirkyMode,
                    onToggleQuirky = onToggleQuirky,
                    notificationHour = state.notificationHour,
                    notificationMinute = state.notificationMinute,
                    onPickNotificationTime = onPickNotificationTime,
                    monetizedPlatforms = state.monetizedPlatforms,
                    adFreePackages = state.adFreePackages,
                    onToggleAdFree = onToggleAdFree,
                    onOpenAdScanner = onOpenAdScanner,
                )
            }
        }
    }
}
