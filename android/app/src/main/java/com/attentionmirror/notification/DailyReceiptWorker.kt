package com.attentionmirror.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.attentionmirror.MainActivity
import com.attentionmirror.R
import com.attentionmirror.data.AttentionRepository
import com.attentionmirror.domain.Copy
import com.attentionmirror.domain.DynamicMessages
import com.attentionmirror.domain.Formatting
import java.time.LocalDate

/**
 * Runs once a day (see [DailyReceiptScheduler]). Refreshes today's usage and
 * posts the "Attention Receipt" notification — the app's killer feature.
 */
class DailyReceiptWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val isTest = inputData.getBoolean(KEY_TEST, false)
        val repo = AttentionRepository.create(applicationContext)
        if (!repo.hasUsageAccess() && !isTest) return Result.success()

        if (repo.hasUsageAccess()) repo.refresh()
        val receipt = repo.dailyReceipt()
        if (receipt.totalMinutes <= 0 && !isTest) return Result.success()

        val message = if (receipt.totalMinutes > 0) {
            // A fresh, day-specific message so the notification never repeats.
            DynamicMessages.forDay(
                receipt = receipt,
                yesterdayMinutes = null,
                peakHourLabel = null,
                date = LocalDate.now(),
                tone = Copy.toneOf(repo.hardTruthMode, repo.quirkyMode),
                currency = repo.currency(),
            )
        } else {
            com.attentionmirror.domain.DynamicMessage(
                "Test: your daily receipt",
                "Notifications are working. Your real receipt appears once you've used a tracked app.",
            )
        }

        val currency = repo.currency()
        postNotification(
            title = message.headline,
            ads = receipt.estimatedAdsSeen,
            value = Formatting.valueRange(
                receipt.estimatedValueLowInr,
                receipt.estimatedValueHighInr,
                currency,
            ),
            returned = Formatting.money(receipt.userReceivedInr, currency),
            tagline = message.body,
        )
        return Result.success()
    }

    private fun postNotification(title: String, ads: Int, value: String, returned: String, tagline: String) {
        ensureChannel(applicationContext)

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return // user hasn't granted notifications; nothing to do.
        }

        val openApp = MainActivity.pendingIntent(applicationContext)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_receipt)
            .setContentTitle(title)
            .setContentText("~$ads ads · created $value · returned to you $returned")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Estimated ads shown: $ads\n" +
                        "Estimated value created: $value\n" +
                        "Paid back to you: $returned\n\n" +
                        "$tagline\n" +
                        "Tap to see your Attention Receipt.",
                ),
            )
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIF_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "daily_receipt"
        const val KEY_TEST = "test"
        private const val NOTIF_ID = 1001

        /** Fire the receipt notification immediately (used by the test button). */
        fun runNow(context: Context) {
            ensureChannel(context)
            val request = androidx.work.OneTimeWorkRequestBuilder<DailyReceiptWorker>()
                .setInputData(androidx.work.workDataOf(KEY_TEST to true))
                .build()
            androidx.work.WorkManager.getInstance(context).enqueue(request)
        }

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Daily attention receipt",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Your end-of-day unpaid attention summary."
            }
            manager.createNotificationChannel(channel)
        }
    }
}
