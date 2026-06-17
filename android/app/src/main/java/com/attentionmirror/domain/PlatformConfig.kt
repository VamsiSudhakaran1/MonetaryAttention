package com.attentionmirror.domain

/**
 * Tunable per-platform ad assumptions. Kept in sync with the backend
 * (`/platforms`) and with `docs/ESTIMATE_SPEC.md`.
 *
 * @param monetized when false (e.g. WhatsApp) time is tracked but no attention
 *   value is ever attributed.
 */
data class PlatformConfig(
    val platform: String,
    val packageName: String,
    val adsPerMinute: Double,
    val lowCpmInr: Double,
    val highCpmInr: Double,
    val monetized: Boolean = true,
)

/**
 * Seeded defaults shipped with the app so it works fully offline on first run.
 * Mirrors `backend/app/seed.py`. The backend can override these at runtime.
 */
object DefaultPlatforms {
    val ALL: List<PlatformConfig> = listOf(
        PlatformConfig("YouTube", "com.google.android.youtube", 0.20, 250.0, 800.0),
        PlatformConfig("Facebook", "com.facebook.katana", 0.35, 200.0, 600.0),
        PlatformConfig("Instagram", "com.instagram.android", 0.45, 220.0, 650.0),
        PlatformConfig("X", "com.twitter.android", 0.30, 150.0, 450.0),
        PlatformConfig("Reddit", "com.reddit.frontpage", 0.25, 120.0, 400.0),
        PlatformConfig("Snapchat", "com.snapchat.android", 0.30, 150.0, 450.0),
        PlatformConfig("ShareChat", "in.mohalla.sharechat", 0.40, 80.0, 300.0),
        PlatformConfig("Moj", "in.mohalla.video", 0.50, 80.0, 300.0),
        PlatformConfig("Josh", "com.eterno.shortvideos", 0.50, 80.0, 300.0),
        PlatformConfig("Chrome", "com.android.chrome", 0.10, 100.0, 350.0),
        // Tracked for time only; never monetized.
        PlatformConfig("WhatsApp", "com.whatsapp", 0.0, 0.0, 0.0, monetized = false),
    )

    val BY_PACKAGE: Map<String, PlatformConfig> = ALL.associateBy { it.packageName }

    /** Packages we track at all (everything else is ignored). */
    val PACKAGES: Set<String> = BY_PACKAGE.keys
}
