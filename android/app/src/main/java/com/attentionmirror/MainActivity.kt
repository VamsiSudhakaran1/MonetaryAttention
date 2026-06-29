package com.attentionmirror

import android.Manifest
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.attentionmirror.notification.DailyReceiptWorker
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.attentionmirror.R
import com.attentionmirror.domain.Copy
import com.attentionmirror.domain.ShareCardText
import com.attentionmirror.ui.AttentionApp
import com.attentionmirror.ui.AttentionMirrorTheme
import com.attentionmirror.ui.AttentionViewModel
import com.attentionmirror.ui.ReceiptSharer
import com.attentionmirror.tracking.UsageStatsCollector
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private val viewModel: AttentionViewModel by viewModels()

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            val state by viewModel.state.collectAsState()
            AttentionMirrorTheme {
                AttentionApp(
                    state = state,
                    onGrantAccess = {
                        startActivity(UsageStatsCollector(this).usageAccessSettingsIntent())
                    },
                    onShareReceipt = {
                        val receipt = state.today ?: return@AttentionApp
                        val dateLabel = LocalDate.now()
                            .format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
                        val tone = Copy.toneOf(state.hardTruthMode, state.quirkyMode)
                        ReceiptSharer.share(
                            this,
                            ShareCardText.fromReceipt(receipt, dateLabel, tone),
                        )
                    },
                    onToggleHardTruth = { viewModel.setHardTruthMode(it) },
                    onToggleQuirky = { viewModel.setQuirkyMode(it) },
                    onPickNotificationTime = {
                        requestNotificationPermissionIfNeeded()
                        TimePickerDialog(
                            this,
                            { _, hour, minute -> viewModel.setNotificationTime(hour, minute) },
                            state.notificationHour,
                            state.notificationMinute,
                            false,
                        ).show()
                    },
                    onSendTestReceipt = {
                        requestNotificationPermissionIfNeeded()
                        // Post directly — bypass WorkManager/Doze to isolate the
                        // notification pipeline from scheduling.
                        com.attentionmirror.notification.ReceiptNotifier.showTest(this)
                        android.widget.Toast.makeText(
                            this,
                            "Test sent. If nothing appears, check Notification health below.",
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    },
                    onToggleAdFree = { pkg, adFree -> viewModel.setAdFree(pkg, adFree) },
                    onMarkAd = { pkg -> viewModel.markAd(pkg) },
                    onAddAdTile = { requestAddAdTile() },
                    onOpenAdScanner = {
                        startActivity(
                            Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                )
            }
        }
    }

    /** Android 13+: ask for notification permission so the daily receipt can show. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** Android 13+: prompt the system to add the "I saw an ad" Quick Settings tile. */
    private fun requestAddAdTile() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            android.widget.Toast.makeText(
                this,
                "Add the \"I saw an ad\" tile from your Quick Settings edit screen.",
                android.widget.Toast.LENGTH_LONG,
            ).show()
            return
        }
        val sbm = getSystemService(android.app.StatusBarManager::class.java)
        sbm.requestAddTileService(
            android.content.ComponentName(this, com.attentionmirror.tracking.AdMarkTileService::class.java),
            getString(R.string.tile_label),
            android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_stat_receipt),
            { it.run() },
            { },
        )
    }

    override fun onResume() {
        super.onResume()
        // Re-check permission + refresh whenever we return (e.g. from Settings).
        viewModel.refresh()
    }

    companion object {
        fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
