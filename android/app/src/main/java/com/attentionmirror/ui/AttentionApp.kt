package com.attentionmirror.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
    Receipt("Receipt", Icons.Filled.ReceiptLong),
    Week("Week", Icons.Filled.CalendarMonth),
    Settings("Settings", Icons.Filled.Settings),
}

@Composable
fun AttentionApp(
    state: UiState,
    onGrantAccess: () -> Unit,
    onShareReceipt: () -> Unit,
    onToggleHardTruth: (Boolean) -> Unit,
) {
    if (!state.hasUsageAccess) {
        PermissionGate(onGrant = onGrantAccess)
        return
    }

    var tab by remember { mutableIntStateOf(0) }
    val dateLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
    }

    Scaffold(
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
        val modifier = Modifier.padding(padding)
        when (Tab.entries[tab]) {
            Tab.Home -> androidx.compose.foundation.layout.Box(modifier) {
                HomeScreen(state.today, onViewReceipt = { tab = Tab.Receipt.ordinal })
            }
            Tab.Receipt -> androidx.compose.foundation.layout.Box(modifier) {
                ReceiptScreen(
                    receipt = state.today,
                    dateLabel = dateLabel,
                    hardTruthMode = state.hardTruthMode,
                    onShare = onShareReceipt,
                )
            }
            Tab.Week -> androidx.compose.foundation.layout.Box(modifier) {
                WeeklyScreen(state.week)
            }
            Tab.Settings -> androidx.compose.foundation.layout.Box(modifier) {
                SettingsScreen(
                    hardTruthMode = state.hardTruthMode,
                    onToggleHardTruth = onToggleHardTruth,
                )
            }
        }
    }
}
