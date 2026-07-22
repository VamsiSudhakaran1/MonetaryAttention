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
import androidx.compose.ui.res.stringResource
import com.attentionmirror.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class Tab(val labelRes: Int, val icon: ImageVector) {
    Home(R.string.tab_home, Icons.Filled.Home),
    Reports(R.string.tab_reports, Icons.Filled.BarChart),
    Receipt(R.string.tab_receipt, Icons.Filled.ReceiptLong),
    Settings(R.string.tab_settings, Icons.Filled.Settings),
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
    onSendTestReceipt: () -> Unit,
    onToggleAdFree: (String, Boolean) -> Unit,
    onSetCurrency: (String?) -> Unit,
    onMarkAd: (String) -> Unit,
    onAddAdTile: () -> Unit,
    onOpenAdScanner: () -> Unit,
    onSetLanguage: (String) -> Unit,
    currentLanguageTag: String,
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
                    Text(if (current == Tab.Home) stringResource(R.string.app_name) else stringResource(current.labelRes))
                },
                actions = {
                    if (Tab.entries[tab] == Tab.Home && state.today != null) {
                        IconButton(onClick = onShareReceipt) {
                            Icon(Icons.Filled.IosShare, contentDescription = stringResource(R.string.tab_receipt))
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
                        icon = { Icon(t.icon, contentDescription = stringResource(t.labelRes)) },
                        label = { Text(stringResource(t.labelRes)) },
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
                    currency = state.currency,
                    dateLabel = dateLabel,
                    onShare = onShareReceipt,
                )
                Tab.Reports -> ReportsScreen(week = state.week, currency = state.currency)
                Tab.Receipt -> ReceiptScreen(
                    receipt = state.today,
                    dateLabel = dateLabel,
                    tone = com.attentionmirror.domain.Copy.toneOf(state.hardTruthMode, state.quirkyMode),
                    currency = state.currency,
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
                    onSendTestReceipt = onSendTestReceipt,
                    currency = state.currency,
                    onSetCurrency = onSetCurrency,
                    monetizedPlatforms = state.monetizedPlatforms,
                    adFreePackages = state.adFreePackages,
                    onToggleAdFree = onToggleAdFree,
                    onMarkAd = onMarkAd,
                    onAddAdTile = onAddAdTile,
                    onOpenAdScanner = onOpenAdScanner,
                    onSetLanguage = onSetLanguage,
                    currentLanguageTag = currentLanguageTag,
                )
            }
        }
    }
}
