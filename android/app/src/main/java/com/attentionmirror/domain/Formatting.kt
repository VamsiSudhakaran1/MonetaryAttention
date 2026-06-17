package com.attentionmirror.domain

/** Display helpers shared by the UI and the daily notification. */
object Formatting {

    /** 134.0 -> "2h 14m", 48.0 -> "48m". */
    fun minutes(totalMinutes: Double): String {
        val total = totalMinutes.toInt()
        val h = total / 60
        val m = total % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    /** A value range, e.g. "₹18–₹42". Collapses to a single value if equal. */
    fun valueRange(low: Int, high: Int): String =
        if (low == high) "₹$low" else "₹$low–₹$high"
}
