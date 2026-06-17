package com.attentionmirror.domain

/**
 * User-assisted ad counting. Turns the ads a user actually marked ("I saw an
 * ad") into a personal ads/minute rate that overrides the platform default.
 * Mirrors `effective_ads_per_minute` in `backend/app/estimate.py`. See
 * `docs/ESTIMATE_SPEC.md`. No screen content is ever read.
 */
object Calibration {

    /**
     * Minimum observed minutes before we trust a personal rate. Below this we
     * keep the conservative seeded default — a handful of taps shouldn't swing
     * the estimate wildly.
     */
    const val MIN_CALIBRATION_MINUTES = 15.0

    /**
     * Personal ads/minute from the user's own marks, else the platform default.
     * A sufficiently-sampled 0 is honoured (genuinely saw no ads -> rate 0).
     */
    fun effectiveAdsPerMinute(
        config: PlatformConfig,
        observedAds: Int,
        observedMinutes: Double,
    ): Double =
        if (observedMinutes >= MIN_CALIBRATION_MINUTES && observedAds >= 0) {
            observedAds / observedMinutes
        } else {
            config.adsPerMinute
        }
}
