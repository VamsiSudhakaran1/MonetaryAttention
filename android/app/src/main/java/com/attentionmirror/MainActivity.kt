package com.attentionmirror

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        ReceiptSharer.share(
                            this,
                            ShareCardText.fromReceipt(receipt, dateLabel, state.hardTruthMode),
                        )
                    },
                    onToggleHardTruth = { viewModel.setHardTruthMode(it) },
                    onToggleAdFree = { pkg, adFree -> viewModel.setAdFree(pkg, adFree) },
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
