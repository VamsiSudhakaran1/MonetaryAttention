package com.attentionmirror.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Schedules the once-a-day receipt notification at a specific wall-clock time
 * using [AlarmManager], which anchors to the chosen time of day (unlike a
 * periodic worker, whose schedule drifts from the clock). The alarm fires a
 * broadcast ([ReceiptAlarmReceiver]) that runs the worker and re-arms tomorrow.
 */
object DailyReceiptScheduler {

    private const val REQUEST_CODE = 2001
    const val ACTION = "com.attentionmirror.DAILY_RECEIPT"

    fun schedule(context: Context, time: LocalTime = LocalTime.of(21, 30)) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = nextTrigger(time)
        val pending = pendingIntent(context)

        // Allow-while-idle so Doze doesn't silently drop it; use exact only when
        // the OS permits it (no special permission required for the fallback).
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        try {
            if (canExact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun nextTrigger(time: LocalTime, zone: ZoneId = ZoneId.systemDefault()): Long {
        val now = ZonedDateTime.now(zone)
        var next = LocalDate.now(zone).atTime(time).atZone(zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.toInstant().toEpochMilli()
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReceiptAlarmReceiver::class.java).setAction(ACTION)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
