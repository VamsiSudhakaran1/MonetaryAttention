package com.attentionmirror.data

import android.content.Context
import android.content.pm.PackageManager
import com.attentionmirror.domain.AttentionReceipt
import com.attentionmirror.domain.Calibration
import com.attentionmirror.domain.DefaultPlatforms
import com.attentionmirror.domain.EstimateEngine
import com.attentionmirror.tracking.UsageStatsCollector
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    fun hasUsageAccess(): Boolean = collector.hasUsageAccess()

    var hardTruthMode: Boolean
        get() = settings.hardTruthMode
        set(value) {
            settings.hardTruthMode = value
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

    /**
     * Per-package calibrated ads/minute from the user's marks vs all tracked
     * time. Packages without enough sampled time keep their default (no-op).
     */
    private suspend fun personalRates(): Map<String, Double> {
        val marks = adMarkDao.all()
        if (marks.isEmpty()) return emptyMap()
        val minutesByPackage = dao.secondsPerPackageAllTime()
            .associate { it.packageName to it.totalSeconds / 60.0 }
        val rates = mutableMapOf<String, Double>()
        for (mark in marks) {
            val config = DefaultPlatforms.BY_PACKAGE[mark.packageName] ?: continue
            rates[mark.packageName] = Calibration.effectiveAdsPerMinute(
                config = config,
                observedAds = mark.count,
                observedMinutes = minutesByPackage[mark.packageName] ?: 0.0,
            )
        }
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
