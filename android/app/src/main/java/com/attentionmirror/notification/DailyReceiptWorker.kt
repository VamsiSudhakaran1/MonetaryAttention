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
import com.attentionmirror.domain.Formatting

/**
 * Runs once a day (see [DailyReceiptScheduler]). Refreshes today's usage and
 * posts the "Attention Receipt" notification — the app's killer feature.
 */
class DailyReceiptWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = AttentionRepository.create(applicationContext)
        if (!repo.hasUsageAccess()) return Result.success()

        repo.refresh()
        val receipt = repo.dailyReceipt()
        if (receipt.totalMinutes <= 0) return Result.success()

        postNotification(
            time = Formatting.minutes(receipt.totalMinutes),
            ads = receipt.estimatedAdsSeen,
            value = Formatting.valueRange(
                receipt.estimatedValueLowInr,
                receipt.estimatedValueHighInr,
            ),
            tagline = Copy.tagline(repo.hardTruthMode),
        )
        return Result.success()
    }

    private fun postNotification(time: String, ads: Int, value: String, tagline: String) {
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
            .setContentTitle("You gave $time of attention today")
            .setContentText("~$ads ads · created $value · returned to you ₹0")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Estimated ads shown: $ads\n" +
                        "Estimated value created: $value\n" +
                        "Paid back to you: ₹0\n\n" +
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
        private const val NOTIF_ID = 1001

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
