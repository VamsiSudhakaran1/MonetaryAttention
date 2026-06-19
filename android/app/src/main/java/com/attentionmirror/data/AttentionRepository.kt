package com.attentionmirror.data

import android.content.Context
import android.content.pm.PackageManager
import com.attentionmirror.domain.AttentionReceipt
import com.attentionmirror.domain.Calibration
import com.attentionmirror.domain.Copy
import com.attentionmirror.domain.DefaultPlatforms
import com.attentionmirror.domain.DynamicMessage
import com.attentionmirror.domain.DynamicMessages
import com.attentionmirror.domain.EstimateEngine
import com.attentionmirror.domain.Formatting
import com.attentionmirror.domain.Timeline
import com.attentionmirror.domain.UsageSession
import com.attentionmirror.notification.DailyReceiptScheduler
import com.attentionmirror.tracking.UsageStatsCollector
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Everything the Home screen needs for one day. */
data class DayInsights(
    val receipt: AttentionReceipt,
    val sessions: List<UsageSession>,
    val hourlySeconds: List<Long>,
    val message: DynamicMessage,
)

/** One day's receipt, used to plot the weekly chart. */
data class DayReport(val date: LocalDate, val receipt: AttentionReceipt)

/** Everything the Reports screen needs for a 7-day window. */
data class WeekReport(
    val days: List<DayReport>,
    val total: AttentionReceipt,
    val previousTotalMinutes: Double,
)

/**
 * Single entry point for the UI/worker: refresh today's usage from the system,
 * persist it, and produce honest attention receipts.
 */
class AttentionRepository(
    private val context: Context,
    private val dao: UsageDao,
    private val adMarkDao: AdMarkDao,
    private val collector: UsageStatsCollector,
    private val settings: SettingsStore,
) {
    private val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val zone: ZoneId = ZoneId.systemDefault()

    fun hasUsageAccess(): Boolean = collector.hasUsageAccess()

    var hardTruthMode: Boolean
        get() = settings.hardTruthMode
        set(value) {
            settings.hardTruthMode = value
        }

    var quirkyMode: Boolean
        get() = settings.quirkyMode
        set(value) {
            settings.quirkyMode = value
        }

    val notificationHour: Int get() = settings.notificationHour
    val notificationMinute: Int get() = settings.notificationMinute

    /** Persist a new daily notification time and re-arm the scheduled work. */
    fun setNotificationTime(hour: Int, minute: Int) {
        settings.notificationHour = hour
        settings.notificationMinute = minute
        DailyReceiptScheduler.schedule(context, LocalTime.of(hour, minute))
    }

    /** Record one user-marked ad, attributed to a tracked package. */
    suspend fun markAd(packageName: String) = adMarkDao.increment(packageName)

    /**
     * The currently-foreground tracked package, if any — used to attribute an
     * "I saw an ad" tap to the right platform without reading screen content.
     */
    fun currentTrackedPackage(): String? = collector.currentForegroundTrackedPackage()

    /** Pull the given day's usage from the system and store it locally. */
    suspend fun refresh(day: LocalDate = LocalDate.now()) {
        val usage = collector.collectForDay(day)
        if (usage.isEmpty()) return
        val records = usage.map {
            UsageRecord(
                packageName = it.packageName,
                localDate = day.format(isoDate),
                appName = appLabel(it.packageName),
                durationSeconds = it.seconds,
            )
        }
        dao.upsertAll(records)
    }

    suspend fun dailyReceipt(day: LocalDate = LocalDate.now()): AttentionReceipt {
        val seconds = dao.secondsForDay(day.format(isoDate))
            .associate { it.packageName to it.totalSeconds }
        return EstimateEngine.buildReceipt(DefaultPlatforms.BY_PACKAGE, seconds, personalRates())
    }

    suspend fun weeklyReceipt(endDay: LocalDate = LocalDate.now()): AttentionReceipt {
        val start = endDay.minusDays(6)
        val seconds = dao.secondsBetween(start.format(isoDate), endDay.format(isoDate))
            .associate { it.packageName to it.totalSeconds }
        return EstimateEngine.buildReceipt(DefaultPlatforms.BY_PACKAGE, seconds, personalRates())
    }

    /** Today's receipt plus the timeline + a fresh, day-specific message. */
    suspend fun dayInsights(day: LocalDate = LocalDate.now()): DayInsights {
        val receipt = dailyReceipt(day)
        val sessions = collector.collectSessionsForDay(day, zone)
        val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val hourly = Timeline.hourlySeconds(sessions, dayStart)
        val yesterdayMinutes = dao.secondsForDay(day.minusDays(1).format(isoDate))
            .sumOf { it.totalSeconds } / 60.0
        val peakLabel = if (hourly.sum() > 0) Formatting.hourLabel(Timeline.peakHour(hourly)) else null
        val message = DynamicMessages.forDay(
            receipt = receipt,
            yesterdayMinutes = yesterdayMinutes.takeIf { it > 0 },
            peakHourLabel = peakLabel,
            date = day,
            tone = Copy.toneOf(settings.hardTruthMode, settings.quirkyMode),
        )
        return DayInsights(receipt, sessions, hourly.toList(), message)
    }

    /** A 7-day report ending on [endDay], with the prior week for trend. */
    suspend fun weekReport(endDay: LocalDate = LocalDate.now()): WeekReport {
        val start = endDay.minusDays(6)
        val rates = personalRates()
        val byDate = dao.secondsPerDayPackage(start.format(isoDate), endDay.format(isoDate))
            .groupBy { it.localDate }
        val days = (0..6).map { offset ->
            val date = start.plusDays(offset.toLong())
            val seconds = byDate[date.format(isoDate)].orEmpty()
                .associate { it.packageName to it.totalSeconds }
            DayReport(date, EstimateEngine.buildReceipt(DefaultPlatforms.BY_PACKAGE, seconds, rates))
        }
        val total = weeklyReceipt(endDay)
        val prevStart = start.minusDays(7)
        val prevEnd = endDay.minusDays(7)
        val previousMinutes = dao.secondsBetween(prevStart.format(isoDate), prevEnd.format(isoDate))
            .sumOf { it.totalSeconds } / 60.0
        return WeekReport(days, total, previousMinutes)
    }

    fun adFreePackages(): Set<String> = settings.adFreePackages

    fun setAdFree(packageName: String, adFree: Boolean) = settings.setAdFree(packageName, adFree)

    /**
     * Per-package ads/minute overrides applied on top of platform defaults:
     *  - calibration from the user's "I saw an ad" marks vs tracked time, and
     *  - a hard 0 for apps the user marked ad-free (Premium), so no ad value is
     *    attributed even though their time is still shown.
     */
    private suspend fun personalRates(): Map<String, Double> {
        val rates = mutableMapOf<String, Double>()
        val marks = adMarkDao.all()
        if (marks.isNotEmpty()) {
            val minutesByPackage = dao.secondsPerPackageAllTime()
                .associate { it.packageName to it.totalSeconds / 60.0 }
            for (mark in marks) {
                val config = DefaultPlatforms.BY_PACKAGE[mark.packageName] ?: continue
                rates[mark.packageName] = Calibration.effectiveAdsPerMinute(
                    config = config,
                    observedAds = mark.count,
                    observedMinutes = minutesByPackage[mark.packageName] ?: 0.0,
                )
            }
        }
        // Ad-free (Premium) wins over any calibration: zero ads → zero value.
        for (pkg in settings.adFreePackages) rates[pkg] = 0.0
        return rates
    }

    private fun appLabel(pkg: String): String {
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            DefaultPlatforms.BY_PACKAGE[pkg]?.platform ?: pkg
        }
    }

    companion object {
        fun create(context: Context): AttentionRepository {
            val app = context.applicationContext
            val db = AppDatabase.get(app)
            return AttentionRepository(
                app,
                db.usageDao(),
                db.adMarkDao(),
                UsageStatsCollector(app),
                SettingsStore(app),
            )
        }
    }
}
