package com.attentionmirror.domain

/**
 * A single continuous foreground session of one tracked app. Times are epoch
 * millis. Reconstructed from [android.app.usage.UsageStatsManager] events — we
 * only ever see *when* an app was in the foreground, never its content.
 */
data class UsageSession(
    val packageName: String,
    val startMillis: Long,
    val endMillis: Long,
) {
    val durationSeconds: Long
        get() = ((endMillis - startMillis) / 1000L).coerceAtLeast(0L)
}

/** Per-app rollup for a day, derived from [UsageSession]s. */
data class AppUsageDetail(
    val packageName: String,
    val platform: String,
    val seconds: Long,
    val sessionCount: Int,
)

/**
 * Pure helpers that turn raw sessions into the hour-by-hour / minute-by-minute
 * views the UI shows. No Android dependencies, so it's unit-testable.
 */
object Timeline {
    const val HOURS = 24
    private const val HOUR_MS = 3_600_000L

    /**
     * Foreground seconds bucketed into the 24 hours of a local day. Sessions
     * that straddle an hour boundary are split proportionally across hours.
     */
    fun hourlySeconds(sessions: List<UsageSession>, dayStartMillis: Long): LongArray {
        val buckets = LongArray(HOURS)
        val dayEnd = dayStartMillis + HOURS * HOUR_MS
        for (s in sessions) {
            var start = maxOf(s.startMillis, dayStartMillis)
            val end = minOf(s.endMillis, dayEnd)
            while (start < end) {
                val idx = ((start - dayStartMillis) / HOUR_MS).toInt().coerceIn(0, HOURS - 1)
                val sliceEnd = minOf(end, dayStartMillis + (idx + 1) * HOUR_MS)
                buckets[idx] += (sliceEnd - start) / 1000L
                start = sliceEnd
            }
        }
        return buckets
    }

    /** Index of the busiest hour (0–23); 0 when there is no usage. */
    fun peakHour(hourly: LongArray): Int =
        hourly.indices.maxByOrNull { hourly[it] } ?: 0

    /** Group sessions into per-app rollups, busiest first. */
    fun perApp(
        sessions: List<UsageSession>,
        platformOf: (String) -> String,
    ): List<AppUsageDetail> =
        sessions.groupBy { it.packageName }
            .map { (pkg, list) ->
                AppUsageDetail(
                    packageName = pkg,
                    platform = platformOf(pkg),
                    seconds = list.sumOf { it.durationSeconds },
                    sessionCount = list.size,
                )
            }
            .sortedByDescending { it.seconds }
}
