package com.attentionmirror.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.attentionmirror.data.SettingsStore
import java.time.LocalTime

/**
 * Fired by [DailyReceiptScheduler]'s daily alarm: runs the receipt worker, then
 * re-arms the alarm for the next day (alarms are one-shot).
 */
class ReceiptAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<DailyReceiptWorker>().build())

        val settings = SettingsStore(context)
        DailyReceiptScheduler.schedule(
            context,
            LocalTime.of(settings.notificationHour, settings.notificationMinute),
        )
    }
}
