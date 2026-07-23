package com.attentionmirror.tracking

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import android.app.usage.UsageEvents
import com.attentionmirror.domain.DefaultPlatforms
import com.attentionmirror.domain.UsageSession
import java.time.LocalDate
import java.time.ZoneId

/** Per-package foreground seconds for one local day. */
data class DailyUsage(val packageName: String, val seconds: Long)

/**
 * Reads aggregate per-app foreground time via [UsageStatsManager]. This is the
 * only "tracking" the app does: it never reads screen content or messages.
 *
 * `PACKAGE_USAGE_STATS` is a special access that must be granted by the user in
 * Settings; use [hasUsageAccess] / [usageAccessSettingsIntent] to drive that.
 */
class UsageStatsCollector(private val context: Context) {

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Foreground seconds for [day], restricted to platforms we track. Returns
     * an empty list if usage access has not been granted.
     */
    fun collectForDay(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<DailyUsage> {
        if (!hasUsageAccess()) return emptyList()

        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val stats = manager.queryAndAggregateUsageStats(start, end)
        return stats.values
            .filter { it.packageName in DefaultPlatforms.PACKAGES && it.totalTimeInForeground > 0 }
            .map { DailyUsage(it.packageName, it.totalTimeInForeground / 1000L) }
    }

    /**
     * The single most-used app package for [day] across *all* apps (not just the
     * ones we monetize) — used only to flavour the daily message (e.g. detect a
     * gaming or shopping day). Reads aggregate time only, never content.
     */
    fun topAppForDay(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): String? {
        if (!hasUsageAccess()) return null
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return manager.queryAndAggregateUsageStats(start, end).values
            .filter { it.totalTimeInForeground > 0 && it.packageName != context.packageName }
            .maxByOrNull { it.totalTimeInForeground }
            ?.packageName
    }

    /**
     * Reconstructs per-app foreground [UsageSession]s for [day] from the system
     * event stream — this is what powers the hour-by-hour and minute-precise
     * timeline. We only read foreground/background *transitions* (timestamps),
     * never any screen content. Restricted to tracked packages.
     */
    fun collectSessionsForDay(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<UsageSession> {
        if (!hasUsageAccess()) return emptyList()

        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val queryEnd = minOf(dayEnd, System.currentTimeMillis())
        if (queryEnd <= start) return emptyList()

        val events = manager.queryEvents(start, queryEnd)
        val open = HashMap<String, Long>()
        val sessions = ArrayList<UsageSession>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (pkg !in DefaultPlatforms.PACKAGES) continue
            when (event.eventType) {
                // ACTIVITY_RESUMED / MOVE_TO_FOREGROUND share value 1.
                UsageEvents.Event.MOVE_TO_FOREGROUND -> open[pkg] = event.timeStamp
                // ACTIVITY_PAUSED / MOVE_TO_BACKGROUND share value 2.
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val began = open.remove(pkg) ?: continue
                    if (event.timeStamp > began) sessions += UsageSession(pkg, began, event.timeStamp)
                }
            }
        }
        // Close any still-foreground app at the query edge (e.g. open right now).
        for ((pkg, began) in open) {
            if (queryEnd > began) sessions += UsageSession(pkg, began, queryEnd)
        }

        return sessions
            .filter { it.durationSeconds >= 1 }
            .sortedBy { it.startMillis }
    }

    /**
     * The most recently foregrounded *tracked* package within the last few
     * minutes, or null. Used to attribute an "I saw an ad" tap to the platform
     * the user is actually on — without reading any screen content.
     */
    fun currentForegroundTrackedPackage(): String? {
        if (!hasUsageAccess()) return null

        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = manager.queryEvents(now - LOOKBACK_MS, now)

        var lastTrackedPackage: String? = null
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND &&
                event.packageName in DefaultPlatforms.PACKAGES
            ) {
                lastTrackedPackage = event.packageName
            }
        }
        return lastTrackedPackage
    }

    private companion object {
        const val LOOKBACK_MS = 5 * 60 * 1000L
    }
}
