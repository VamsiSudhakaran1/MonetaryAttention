package com.attentionmirror.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** Estimate for a single platform over a usage window. */
data class PlatformEstimate(
    val platform: String,
    val packageName: String,
    val minutes: Double,
    val estimatedAdsSeen: Int,
    val valueLowInr: Double,
    val valueHighInr: Double,
)

/** Aggregated, honest estimate across all monetized platforms. */
data class AttentionReceipt(
    val totalMinutes: Double,
    val estimatedAdsSeen: Int,
    val estimatedValueLowInr: Int,
    val estimatedValueHighInr: Int,
    val userReceivedInr: Int,
    val perPlatform: List<PlatformEstimate>,
)

/**
 * Canonical attention-value math. Mirrors `backend/app/estimate.py`. Everything
 * here is a transparent estimate — never an exact claim. See
 * `docs/ESTIMATE_SPEC.md`.
 */
object EstimateEngine {

    private fun roundHalfUp(value: Double): Int =
        BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toInt()

    /**
     * @param personalAdsPerMinute when non-null, overrides the platform default
     *   (used for user-assisted calibration; see [Calibration]).
     */
    fun estimatePlatform(
        config: PlatformConfig,
        durationSeconds: Long,
        personalAdsPerMinute: Double? = null,
    ): PlatformEstimate {
        val seconds = durationSeconds.coerceAtLeast(0L)
        val minutes = seconds / 60.0

        if (!config.monetized) {
            return PlatformEstimate(
                platform = config.platform,
                packageName = config.packageName,
                minutes = minutes,
                estimatedAdsSeen = 0,
                valueLowInr = 0.0,
                valueHighInr = 0.0,
            )
        }

        val rate = personalAdsPerMinute ?: config.adsPerMinute
        val ads = roundHalfUp(minutes * rate)
        return PlatformEstimate(
            platform = config.platform,
            packageName = config.packageName,
            minutes = minutes,
            estimatedAdsSeen = ads,
            valueLowInr = ads * config.lowCpmInr / 1000.0,
            valueHighInr = ads * config.highCpmInr / 1000.0,
        )
    }

    /**
     * Combine per-package usage (seconds) into a daily/weekly receipt. Packages
     * without a known config are ignored entirely.
     */
    fun buildReceipt(
        configsByPackage: Map<String, PlatformConfig>,
        usageSecondsByPackage: Map<String, Long>,
        personalRatesByPackage: Map<String, Double> = emptyMap(),
    ): AttentionReceipt {
        var totalMinutes = 0.0
        var totalAds = 0
        var totalLow = 0.0
        var totalHigh = 0.0
        val perPlatform = mutableListOf<PlatformEstimate>()

        for ((pkg, seconds) in usageSecondsByPackage) {
            val config = configsByPackage[pkg] ?: continue
            val est = estimatePlatform(config, seconds, personalRatesByPackage[pkg])
            perPlatform += est
            totalMinutes += est.minutes
            totalAds += est.estimatedAdsSeen
            totalLow += est.valueLowInr
            totalHigh += est.valueHighInr
        }

        perPlatform.sortByDescending { it.minutes }

        return AttentionReceipt(
            totalMinutes = totalMinutes,
            estimatedAdsSeen = totalAds,
            estimatedValueLowInr = roundHalfUp(totalLow),
            estimatedValueHighInr = roundHalfUp(totalHigh),
            userReceivedInr = 0,
            perPlatform = perPlatform,
        )
    }
}
