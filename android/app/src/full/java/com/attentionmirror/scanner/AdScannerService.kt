package com.attentionmirror.scanner

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.attentionmirror.data.AdSighting
import com.attentionmirror.data.AppDatabase
import com.attentionmirror.domain.DefaultPlatforms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * OPT-IN ad detector (only in the `full`, non-Play build). When the user turns
 * this on in Accessibility settings, it watches supported apps for on-screen ad
 * markers ("Sponsored", "Promoted", …) and records, per ad: when it appeared,
 * when it left the screen (→ how long it was shown), and which marker matched.
 * Each ad also bumps the calibration count so estimates become real ad rates.
 *
 * Privacy: it reads only whether an ad marker is present (and which keyword),
 * never other screen content, and is restricted to the tracked packages in
 * `res/xml/accessibility_config.xml`. Everything is on-device.
 *
 * NOTE: intentionally absent from the Play build — Accessibility-for-analytics
 * violates Google Play policy. See docs/DISTRIBUTION.md.
 */
class AdScannerService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** An ad currently visible in a package, awaiting its disappearance. */
    private data class OpenAd(val start: Long, val marker: String)

    private val open = HashMap<String, OpenAd>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg !in DefaultPlatforms.PACKAGES) return

        val root = rootInActiveWindow ?: return
        val marker = try {
            firstAdMarker(root, 0)
        } catch (_: Exception) {
            null
        }
        val now = System.currentTimeMillis()
        val current = open[pkg]

        if (marker != null) {
            when {
                current == null -> open[pkg] = OpenAd(now, marker) // ad appeared
                now - current.start > MAX_AD_MS -> {                // cap a stuck/looping ad
                    finalize(pkg, current.start, current.start + MAX_AD_MS, current.marker)
                    open[pkg] = OpenAd(now, marker)
                }
                // else: same ad still showing — keep accumulating duration.
            }
        } else if (current != null) {
            open.remove(pkg)                                        // ad left the screen
            finalize(pkg, current.start, now, current.marker)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        // Best-effort: close anything still open at the current time.
        val now = System.currentTimeMillis()
        open.forEach { (pkg, ad) -> finalize(pkg, ad.start, now, ad.marker) }
        open.clear()
        scope.cancel()
    }

    private fun finalize(pkg: String, start: Long, end: Long, marker: String) {
        if (end <= start) return
        val db = AppDatabase.get(applicationContext)
        val day = LocalDate.now().toString()
        scope.launch {
            db.adSightingDao().insert(
                AdSighting(
                    packageName = pkg,
                    localDate = day,
                    startMillis = start,
                    endMillis = end,
                    marker = marker,
                ),
            )
            // Keep feeding the existing calibration (one count per ad).
            db.adMarkDao().increment(pkg)
        }
    }

    /** First matching ad-marker keyword in the tree, or null. */
    private fun firstAdMarker(node: AccessibilityNodeInfo?, depth: Int): String? {
        if (node == null || depth > MAX_DEPTH) return null
        val text = buildString {
            node.text?.let { append(it) }
            append(' ')
            node.contentDescription?.let { append(it) }
        }
        if (text.isNotBlank()) {
            val lower = text.lowercase()
            MARKERS.firstOrNull { lower.contains(it) }?.let { return it }
        }
        for (i in 0 until node.childCount) {
            firstAdMarker(node.getChild(i), depth + 1)?.let { return it }
        }
        return null
    }

    private companion object {
        const val MAX_AD_MS = 90_000L
        const val MAX_DEPTH = 40
        val MARKERS = listOf("sponsored", "promoted", "paid partnership")
    }
}
