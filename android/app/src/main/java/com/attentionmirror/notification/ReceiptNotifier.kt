package com.attentionmirror.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.attentionmirror.MainActivity
import com.attentionmirror.R

/**
 * Direct notification posting + health checks. Used by the "Test" button and the
 * Settings "Notification health" panel so notification problems are diagnosable
 * in-app, without going through WorkManager/Doze/the worker (which can each fail
 * silently).
 */
object ReceiptNotifier {

    private const val TEST_NOTIF_ID = 1002

    /** True if the app may actually show notifications right now. */
    fun notificationsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** True unless the user/OEM has muted our channel specifically. */
    fun channelEnabled(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(DailyReceiptWorker.CHANNEL_ID) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** Post a test notification immediately — no WorkManager, no Doze, no worker. */
    fun showTest(context: Context) {
        DailyReceiptWorker.ensureChannel(context)
        if (!notificationsAllowed(context)) return
        val notification = NotificationCompat.Builder(context, DailyReceiptWorker.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_receipt)
            .setContentTitle("Test: notifications are working ✅")
            .setContentText("Your real receipt will arrive at your chosen time.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "If you can see this, notifications are set up correctly.\n" +
                        "Your daily Attention Receipt will arrive at the time you picked in Settings.",
                ),
            )
            .setContentIntent(MainActivity.pendingIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(TEST_NOTIF_ID, notification)
    }
}
