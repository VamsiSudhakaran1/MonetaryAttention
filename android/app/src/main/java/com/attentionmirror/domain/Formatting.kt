package com.attentionmirror.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Display helpers shared by the UI, the daily notification and the share card. */
object Formatting {

    private val clockFmt = DateTimeFormatter.ofPattern("h:mm a")

    /** 134.0 -> "2h 14m", 48.0 -> "48m". */
    fun minutes(totalMinutes: Double): String {
        val total = totalMinutes.toInt()
        val h = total / 60
        val m = total % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    /** Seconds to a short label: "<1m", "48m", "2h 14m". */
    fun durationShort(seconds: Long): String {
        val minutes = (seconds / 60.0)
        return if (minutes < 1.0) "<1m" else minutes(minutes)
    }

    /** A single money amount (INR base) shown in [currency], e.g. "$3". */
    fun money(amountInr: Int, currency: Currency): String =
        "${currency.symbol}${(amountInr * currency.factor).roundToInt()}"

    /** A value range in [currency], e.g. "₹18–₹42" / "$1–$3". */
    fun valueRange(lowInr: Int, highInr: Int, currency: Currency): String {
        val low = money(lowInr, currency)
        val high = money(highInr, currency)
        return if (low == high) low else "$low–$high"
    }

    /** A wall-clock time, e.g. (21, 30) -> "9:30 PM". */
    fun timeOfDay(hour: Int, minute: Int): String =
        java.time.LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)).format(clockFmt)

    /** Hour-of-day (0–23) to a readable label: 0 -> "12 AM", 21 -> "9 PM". */
    fun hourLabel(hour: Int): String {
        val h = ((hour % 24) + 24) % 24
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return "$h12 ${if (h < 12) "AM" else "PM"}"
    }

    /** A session's wall-clock span, e.g. "2:32 – 3:10 PM". */
    fun clockRange(startMillis: Long, endMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val start = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalTime().format(clockFmt)
        val end = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalTime().format(clockFmt)
        return "$start – $end"
    }

    /** Signed percent change, e.g. "+24%", "-12%", or "—" when there's no base. */
    fun percentChange(current: Double, previous: Double): String {
        if (previous <= 0.0) return "—"
        val pct = ((current - previous) / previous * 100).roundToInt()
        val sign = if (pct > 0) "+" else ""
        return "$sign$pct%"
    }

    /** Single-letter weekday initial for the weekly chart. */
    fun dayInitial(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault())
}
