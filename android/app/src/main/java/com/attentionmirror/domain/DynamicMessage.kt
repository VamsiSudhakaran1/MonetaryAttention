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
        tone: Tone,
        currency: Currency,
    ): DynamicMessage {
        val minutes = receipt.totalMinutes
        val rng = Random(date.toEpochDay())

        if (minutes < 1.0) return choose(EMPTY, rng, receipt, peakHourLabel, tone, currency)

        // Quirky has its own personality pool, regardless of the usage bucket.
        if (tone == Tone.QUIRKY) {
            val q = QUIRKY[rng.nextInt(QUIRKY.size)]
            return DynamicMessage(
                fill(q.first, receipt, peakHourLabel, currency),
                fill(q.second, receipt, peakHourLabel, currency),
            )
        }

        val bucket = when {
            yesterdayMinutes != null && yesterdayMinutes >= 5 && minutes > yesterdayMinutes * 1.2 -> UP
            yesterdayMinutes != null && yesterdayMinutes >= 5 && minutes < yesterdayMinutes * 0.8 -> DOWN
            minutes >= 180 -> HEAVY
            minutes < 30 -> LIGHT
            date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY -> WEEKEND
            else -> DEFAULT
        }
        return choose(bucket, rng, receipt, peakHourLabel, tone, currency)
    }

    private fun choose(
        bucket: List<Pair<String, String>>,
        rng: Random,
        receipt: AttentionReceipt,
        peakHourLabel: String?,
        tone: Tone,
        currency: Currency,
    ): DynamicMessage {
        val tmpl = bucket[rng.nextInt(bucket.size)]
        val headline = fill(tmpl.first, receipt, peakHourLabel, currency)
        val body = when (tone) {
            Tone.HARD -> fill(HARD_TRUTH_TAILS[rng.nextInt(HARD_TRUTH_TAILS.size)], receipt, peakHourLabel, currency)
            Tone.QUIRKY -> fill(QUIRKY_TAILS[rng.nextInt(QUIRKY_TAILS.size)], receipt, peakHourLabel, currency)
            Tone.GENTLE -> fill(tmpl.second, receipt, peakHourLabel, currency)
        }
        return DynamicMessage(headline, body)
    }

    private fun fill(s: String, receipt: AttentionReceipt, peakHourLabel: String?, currency: Currency): String {
        val value = Formatting.valueRange(receipt.estimatedValueLowInr, receipt.estimatedValueHighInr, currency)
        val top = receipt.perPlatform.firstOrNull()?.platform ?: "apps"
        return s.replace("{value}", value)
            .replace("{time}", Formatting.minutes(receipt.totalMinutes))
            .replace("{ads}", receipt.estimatedAdsSeen.toString())
            .replace("{top}", top)
            .replace("{peak}", peakHourLabel ?: "the evening")
            .replace("{returned}", Formatting.money(receipt.userReceivedInr, currency))
    }

    // Each entry is headline → body. Placeholders: {value} {time} {ads} {top} {peak}.

    private val DEFAULT = listOf(
        "You created {value} of attention today" to "{time} of scrolling. {ads} ads. {returned} came back to you.",
        "{top} got most of your attention" to "{time} today turned into {value} for advertisers.",
        "Today's attention bill: {value}" to "Paid by you, in {time} and {ads} ads. Returned: {returned}.",
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
        "Down from yesterday" to "{time} on monetized apps, {value} of value, {returned} to you.",
    )

    private val HEAVY = listOf(
        "A heavy day: {time}" to "That created about {value}. You were paid with distraction.",
        "{time} is a lot of attention" to "{ads} ads, {value} of value created. Returned to you: {returned}.",
        "Long day on the feed" to "{top} led {time} of scrolling — {value} earned by others.",
    )

    private val LIGHT = listOf(
        "A light day: {time}" to "Even so, it created roughly {value} for advertisers.",
        "Barely scrolled today" to "{time} only — about {value} of attention value, still {returned} to you.",
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
        "Your scrolling created value. You were paid {returned}.",
        "Someone billed for your time today. It wasn't you.",
    )

    // Quirky personality (opt-in). Cheeky, never mean.
    private val QUIRKY = listOf(
        "Congrats — you were the product again 🎉" to "{time} of scrolling = {value} for advertisers. Your cut: {returned}.",
        "Your eyeballs had a busy day 👀" to "{ads} ads watched you back. {value} created. You? {returned}.",
        "Unpaid internship at {top} 🧑‍💻" to "{time} clocked in today. Salary: {value}… to them.",
        "The algorithm says thank you 🙏" to "{time} became {value}. You were paid in vibes.",
        "Main character energy 🎬" to "Featured in ~{ads} ads. Box office: {value}. Your royalties: {returned}.",
        "Doomscroll speedrun complete 🏃" to "{time} logged. {value} generated for the house.",
        "Your attention went shopping 🛍️" to "Spent {time}, made {value} for others, came back with {returned}.",
        "Free labour, premium vibes ✨" to "{ads} ads, {value} of value, all paid in dopamine.",
    )

    private val QUIRKY_TAILS = listOf(
        "You did unpaid overtime for the algorithm. 💸",
        "Tip jar for your attention: still {returned}. 🫥",
        "The feed thanks you for your service. 🫡",
    )
}
