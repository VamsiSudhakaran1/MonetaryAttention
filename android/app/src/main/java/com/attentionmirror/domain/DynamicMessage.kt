package com.attentionmirror.domain

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/** A headline + supporting line for the top of the Home screen. */
data class DynamicMessage(val headline: String, val body: String)

/**
 * Generates a *different* daily message depending on the day, the amount of
 * usage, the trend vs. yesterday, and a date seed — so the user never sees the
 * same line two days running (which is what makes a static message ignorable).
 *
 * The choice is deterministic for a given date (seeded by the epoch day), so the
 * message is stable within a day but rotates across days. Tone follows the
 * user's "hard truth" preference; the underlying numbers never change.
 */
object DynamicMessages {

    fun forDay(
        receipt: AttentionReceipt,
        yesterdayMinutes: Double?,
        peakHourLabel: String?,
        date: LocalDate,
        hardTruth: Boolean,
    ): DynamicMessage {
        val minutes = receipt.totalMinutes
        val rng = Random(date.toEpochDay())

        if (minutes < 1.0) return choose(EMPTY, rng, receipt, peakHourLabel, hardTruth)

        val bucket = when {
            yesterdayMinutes != null && yesterdayMinutes >= 5 && minutes > yesterdayMinutes * 1.2 -> UP
            yesterdayMinutes != null && yesterdayMinutes >= 5 && minutes < yesterdayMinutes * 0.8 -> DOWN
            minutes >= 180 -> HEAVY
            minutes < 30 -> LIGHT
            date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY -> WEEKEND
            else -> DEFAULT
        }
        return choose(bucket, rng, receipt, peakHourLabel, hardTruth)
    }

    private fun choose(
        bucket: List<Pair<String, String>>,
        rng: Random,
        receipt: AttentionReceipt,
        peakHourLabel: String?,
        hardTruth: Boolean,
    ): DynamicMessage {
        val tmpl = bucket[rng.nextInt(bucket.size)]
        val headline = fill(tmpl.first, receipt, peakHourLabel)
        val body =
            if (hardTruth) HARD_TRUTH_TAILS[rng.nextInt(HARD_TRUTH_TAILS.size)]
            else fill(tmpl.second, receipt, peakHourLabel)
        return DynamicMessage(headline, body)
    }

    private fun fill(s: String, receipt: AttentionReceipt, peakHourLabel: String?): String {
        val value = Formatting.valueRange(receipt.estimatedValueLowInr, receipt.estimatedValueHighInr)
        val top = receipt.perPlatform.firstOrNull()?.platform ?: "apps"
        return s.replace("{value}", value)
            .replace("{time}", Formatting.minutes(receipt.totalMinutes))
            .replace("{ads}", receipt.estimatedAdsSeen.toString())
            .replace("{top}", top)
            .replace("{peak}", peakHourLabel ?: "the evening")
    }

    // Each entry is headline → body. Placeholders: {value} {time} {ads} {top} {peak}.

    private val DEFAULT = listOf(
        "You created {value} of attention today" to "{time} of scrolling. {ads} ads. ₹0 came back to you.",
        "{top} got most of your attention" to "{time} today turned into {value} for advertisers.",
        "Today's attention bill: {value}" to "Paid by you, in {time} and {ads} ads. Returned: ₹0.",
        "Your time was worth {value} today" to "You spent {time}. None of that value reached you.",
    )

    private val UP = listOf(
        "Up sharply today" to "More than yesterday — {time} created {value} of attention value.",
        "Busier than yesterday" to "{time} on screen today. Advertisers' gain: {value}.",
        "Your attention spiked today" to "{ads} ads across {time}. That's {value} you generated.",
    )

    private val DOWN = listOf(
        "Less than yesterday — nice" to "Still {time} today, worth {value} to others.",
        "You scrolled less today" to "{time} this time. Even so, {value} was created from your attention.",
        "Down from yesterday" to "{time} on monetized apps, {value} of value, ₹0 to you.",
    )

    private val HEAVY = listOf(
        "A heavy day: {time}" to "That created about {value}. You were paid with distraction.",
        "{time} is a lot of attention" to "{ads} ads, {value} of value created. Returned to you: ₹0.",
        "Long day on the feed" to "{top} led {time} of scrolling — {value} earned by others.",
    )

    private val LIGHT = listOf(
        "A light day: {time}" to "Even so, it created roughly {value} for advertisers.",
        "Barely scrolled today" to "{time} only — about {value} of attention value, still ₹0 to you.",
        "Quiet day on the apps" to "{time} on {top}. Value created: {value}.",
    )

    private val WEEKEND = listOf(
        "Weekend attention: {value}" to "{time} of scrolling today. The feed doesn't take days off.",
        "Your weekend, monetized" to "{time} today created {value}. None of it returned to you.",
        "Relaxing? So is the ad engine" to "{time} on {top} — {value} earned from your weekend.",
    )

    private val EMPTY = listOf(
        "No tracked attention yet today" to "Spend time on a monetized app and your receipt builds here.",
        "Nothing monetized so far" to "When you open the feed, we'll show what your time is worth.",
    )

    private val HARD_TRUTH_TAILS = listOf(
        "You worked for the attention economy today. Unpaid.",
        "Your scrolling created value. You were paid ₹0.",
        "Someone billed for your time today. It wasn't you.",
    )
}
