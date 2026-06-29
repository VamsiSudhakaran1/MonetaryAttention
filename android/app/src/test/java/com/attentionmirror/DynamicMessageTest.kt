package com.attentionmirror

import com.attentionmirror.domain.AttentionReceipt
import com.attentionmirror.domain.Currencies
import com.attentionmirror.domain.DynamicMessages
import com.attentionmirror.domain.PlatformEstimate
import com.attentionmirror.domain.Tone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DynamicMessageTest {

    private fun receipt(minutes: Double, low: Int = 5, high: Int = 16, ads: Int = 20) =
        AttentionReceipt(
            totalMinutes = minutes,
            estimatedAdsSeen = ads,
            estimatedValueLowInr = low,
            estimatedValueHighInr = high,
            userReceivedInr = 0,
            perPlatform = listOf(
                PlatformEstimate("YouTube", "com.google.android.youtube", minutes, ads, low.toDouble(), high.toDouble()),
            ),
        )

    @Test
    fun `same day produces a stable message`() {
        val date = LocalDate.of(2026, 6, 17)
        val a = DynamicMessages.forDay(receipt(100.0), 80.0, "9 PM", date, Tone.GENTLE, Currencies.INR)
        val b = DynamicMessages.forDay(receipt(100.0), 80.0, "9 PM", date, Tone.GENTLE, Currencies.INR)
        assertEquals(a, b)
    }

    @Test
    fun `message changes across days`() {
        val r = receipt(100.0)
        val messages = (0..6).map {
            DynamicMessages.forDay(r, 80.0, "9 PM", LocalDate.of(2026, 6, 17).plusDays(it.toLong()), Tone.GENTLE, Currencies.INR)
        }
        assertTrue(messages.map { it.headline }.distinct().size >= 2)
    }

    @Test
    fun `no leftover placeholders in any tone`() {
        val date = LocalDate.of(2026, 6, 17)
        for (tone in Tone.values()) {
            listOf(0.0, 10.0, 100.0, 240.0).forEach { mins ->
                val m = DynamicMessages.forDay(receipt(mins), 50.0, "9 PM", date, tone, Currencies.INR)
                assertFalse(m.headline.contains("{"))
                assertFalse(m.body.contains("{"))
            }
        }
    }

    @Test
    fun `hard truth changes the body`() {
        val date = LocalDate.of(2026, 6, 17)
        val soft = DynamicMessages.forDay(receipt(100.0), 80.0, "9 PM", date, Tone.GENTLE, Currencies.INR)
        val hard = DynamicMessages.forDay(receipt(100.0), 80.0, "9 PM", date, Tone.HARD, Currencies.INR)
        assertNotEquals(soft.body, hard.body)
    }

    @Test
    fun `quirky differs from gentle`() {
        val date = LocalDate.of(2026, 6, 17)
        val gentle = DynamicMessages.forDay(receipt(100.0), 80.0, "9 PM", date, Tone.GENTLE, Currencies.INR)
        val quirky = DynamicMessages.forDay(receipt(100.0), 80.0, "9 PM", date, Tone.QUIRKY, Currencies.INR)
        assertNotEquals(gentle.headline, quirky.headline)
    }

    @Test
    fun `no usage uses the empty bucket`() {
        val m = DynamicMessages.forDay(receipt(0.0, low = 0, high = 0, ads = 0), null, null, LocalDate.of(2026, 6, 17), Tone.GENTLE, Currencies.INR)
        assertTrue(m.headline.isNotBlank())
        assertFalse(m.headline.contains("{"))
    }
}
