package com.attentionmirror.scanner

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.attentionmirror.data.AppDatabase
import com.attentionmirror.domain.DefaultPlatforms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * OPT-IN ad detector (only in the `full`, non-Play build). When the user turns
 * this on in Accessibility settings, it watches supported apps for on-screen ad
 * markers ("Sponsored", "Promoted", …) and records a count — automating the
 * "I saw an ad" calibration so estimates become real, per-user ad rates.
 *
 * Privacy: it reads only whether an ad marker is present, never stores screen
 * text/content, and is restricted to the tracked packages declared in
 * `res/xml/accessibility_config.xml`. Counting is on-device only.
 *
 * NOTE: this is intentionally absent from the Play build — using Accessibility
 * for analytics violates Google Play policy. See docs/DISTRIBUTION.md.
 */
class AdScannerService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Edge-detect: only count when a marker newly appears (absent -> present),
    // rate-limited per package, so one ad isn't counted on every scroll event.
    private val lastHadMarker = HashMap<String, Boolean>()
    private val lastCountAt = HashMap<String, Long>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg !in DefaultPlatforms.PACKAGES) return

        val root = rootInActiveWindow ?: return
        val hasMarker = try {
            containsAdMarker(root, 0)
        } catch (_: Exception) {
            false
        }

        val previouslyHad = lastHadMarker[pkg] ?: false
        lastHadMarker[pkg] = hasMarker
        if (!hasMarker || previouslyHad) return

        val now = System.currentTimeMillis()
        if (now - (lastCountAt[pkg] ?: 0L) < MIN_GAP_MS) return
        lastCountAt[pkg] = now
        recordAd(pkg)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun recordAd(pkg: String) {
        val dao = AppDatabase.get(applicationContext).adMarkDao()
        scope.launch { dao.increment(pkg) }
    }

    private fun containsAdMarker(node: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (node == null || depth > MAX_DEPTH) return false
        val text = buildString {
            node.text?.let { append(it) }
            append(' ')
            node.contentDescription?.let { append(it) }
        }
        if (text.isNotBlank()) {
            val lower = text.lowercase()
            if (MARKERS.any { lower.contains(it) }) return true
        }
        for (i in 0 until node.childCount) {
            if (containsAdMarker(node.getChild(i), depth + 1)) return true
        }
        return false
    }

    private companion object {
        const val MIN_GAP_MS = 2500L
        const val MAX_DEPTH = 40
        val MARKERS = listOf("sponsored", "promoted", "paid partnership")
    }
}
