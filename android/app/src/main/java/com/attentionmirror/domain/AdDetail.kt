package com.attentionmirror.domain

/**
 * Per-app ad reporting derived from the opt-in scanner's sightings:
 * how many ads, how long they were on screen, and how frequently they appeared.
 * All on-device; only available in the `full` build.
 */
data class AdDetail(
    val packageName: String,
    val platform: String,
    val count: Int,
    val totalAdSeconds: Long,
    val avgAdSeconds: Double,
    val adsPerMinute: Double,
)
