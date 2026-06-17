package com.attentionmirror

import com.attentionmirror.domain.Timeline
import com.attentionmirror.domain.UsageSession
import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineTest {

    private val dayStart = 0L // local midnight at epoch for simple maths
    private val hour = 3_600_000L

    @Test
    fun `session within one hour lands in that bucket`() {
        // 09:00–09:30 -> 1800s in hour 9
        val sessions = listOf(UsageSession("p", 9 * hour, 9 * hour + 30 * 60_000L))
        val buckets = Timeline.hourlySeconds(sessions, dayStart)
        assertEquals(1800L, buckets[9])
        assertEquals(0L, buckets[8])
        assertEquals(9, Timeline.peakHour(buckets))
    }

    @Test
    fun `session spanning an hour boundary is split`() {
        // 09:30–10:30 -> 1800s in hour 9 and 1800s in hour 10
        val sessions = listOf(UsageSession("p", 9 * hour + 30 * 60_000L, 10 * hour + 30 * 60_000L))
        val buckets = Timeline.hourlySeconds(sessions, dayStart)
        assertEquals(1800L, buckets[9])
        assertEquals(1800L, buckets[10])
    }

    @Test
    fun `perApp groups and sorts by total seconds desc`() {
        val sessions = listOf(
            UsageSession("a", 0, 60_000L),               // 60s
            UsageSession("b", 0, 300_000L),              // 300s
            UsageSession("a", hour, hour + 120_000L),    // 120s
        )
        val perApp = Timeline.perApp(sessions) { it.uppercase() }
        assertEquals("b", perApp[0].packageName)
        assertEquals(300L, perApp[0].seconds)
        assertEquals("a", perApp[1].packageName)
        assertEquals(180L, perApp[1].seconds)
        assertEquals(2, perApp[1].sessionCount)
        assertEquals("A", perApp[1].platform)
    }
}
