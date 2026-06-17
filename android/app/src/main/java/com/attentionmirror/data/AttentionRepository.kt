package com.attentionmirror.data

import android.content.Context
import android.content.pm.PackageManager
import com.attentionmirror.domain.AttentionReceipt
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
    private val collector: UsageStatsCollector,
) {
    private val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun hasUsageAccess(): Boolean = collector.hasUsageAccess()

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
        return EstimateEngine.buildReceipt(DefaultPlatforms.BY_PACKAGE, seconds)
    }

    suspend fun weeklyReceipt(endDay: LocalDate = LocalDate.now()): AttentionReceipt {
        val start = endDay.minusDays(6)
        val seconds = dao.secondsBetween(start.format(isoDate), endDay.format(isoDate))
            .associate { it.packageName to it.totalSeconds }
        return EstimateEngine.buildReceipt(DefaultPlatforms.BY_PACKAGE, seconds)
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
            val db = AppDatabase.get(context)
            return AttentionRepository(
                context.applicationContext,
                db.usageDao(),
                UsageStatsCollector(context.applicationContext),
            )
        }
    }
}
