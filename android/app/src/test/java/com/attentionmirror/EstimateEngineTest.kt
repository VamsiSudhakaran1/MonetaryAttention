package com.attentionmirror

import com.attentionmirror.domain.DefaultPlatforms
import com.attentionmirror.domain.EstimateEngine
import com.attentionmirror.domain.PlatformConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EstimateEngineTest {

    private val youtube = PlatformConfig("YouTube", "com.google.android.youtube", 0.20, 250.0, 800.0)

    @Test
    fun singlePlatformMatchesSpec() {
        val est = EstimateEngine.estimatePlatform(youtube, 72 * 60)
        assertEquals(14, est.estimatedAdsSeen)
        assertEquals(14 * 250 / 1000.0, est.valueLowInr, 1e-9)
        assertEquals(14 * 800 / 1000.0, est.valueHighInr, 1e-9)
    }

    @Test
    fun roundsHalfUp() {
        val insta = PlatformConfig("Instagram", "com.instagram.android", 0.45, 220.0, 650.0)
        // 50 min * 0.45 = 22.5 -> 23
        assertEquals(23, EstimateEngine.estimatePlatform(insta, 50 * 60).estimatedAdsSeen)
    }

    @Test
    fun nonMonetizedHasNoValue() {
        val wa = PlatformConfig("WhatsApp", "com.whatsapp", 0.0, 0.0, 0.0, monetized = false)
        val est = EstimateEngine.estimatePlatform(wa, 60 * 60)
        assertEquals(0, est.estimatedAdsSeen)
        assertEquals(0.0, est.valueLowInr, 1e-9)
        assertEquals(60.0, est.minutes, 1e-9)
    }

    @Test
    fun negativeDurationClamped() {
        val est = EstimateEngine.estimatePlatform(youtube, -100)
        assertEquals(0, est.estimatedAdsSeen)
        assertEquals(0.0, est.minutes, 1e-9)
    }

    @Test
    fun buildReceiptAggregatesAndSorts() {
        val usage = mapOf(
            "com.google.android.youtube" to 72L * 60,
            "com.facebook.katana" to 48L * 60,
            "com.instagram.android" to 34L * 60,
            "com.unknown.app" to 99L * 60, // ignored
        )
        val receipt = EstimateEngine.buildReceipt(DefaultPlatforms.BY_PACKAGE, usage)
        assertEquals(46, receipt.estimatedAdsSeen)
        assertEquals(10, receipt.estimatedValueLowInr)
        assertEquals(31, receipt.estimatedValueHighInr)
        assertEquals(0, receipt.userReceivedInr)
        assertEquals(
            listOf("YouTube", "Facebook", "Instagram"),
            receipt.perPlatform.map { it.platform },
        )
        assertTrue(receipt.perPlatform.none { it.packageName == "com.unknown.app" })
    }
}
