package com.attentionmirror.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Schedules the once-a-day receipt notification (default 21:30 local). */
object DailyReceiptScheduler {

    private const val WORK_NAME = "daily_receipt_work"

    fun schedule(context: Context, time: LocalTime = LocalTime.of(21, 30)) {
        val initialDelay = delayUntilNext(time)
        val request = PeriodicWorkRequestBuilder<DailyReceiptWorker>(Duration.ofDays(1))
            .setInitialDelay(initialDelay)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun delayUntilNext(time: LocalTime, zone: ZoneId = ZoneId.systemDefault()): Duration {
        val now = ZonedDateTime.now(zone)
        var next = LocalDate.now(zone).atTime(time).atZone(zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
