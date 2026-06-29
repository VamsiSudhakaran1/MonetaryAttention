package com.attentionmirror.domain

/** Message tone. The math never changes — only the wording. */
enum class Tone { GENTLE, HARD, QUIRKY }

/**
 * Tone-aware copy. The default is respectful (works for senior citizens,
 * homemakers, students). "Hard truth" is stronger; "Quirky" is cheeky/funny.
 * Both stronger tones are opt-in (Settings). See `docs/ESTIMATE_SPEC.md`.
 */
object Copy {

    /** Resolve the active tone from the two opt-in toggles (quirky wins). */
    fun toneOf(hardTruth: Boolean, quirky: Boolean): Tone = when {
        quirky -> Tone.QUIRKY
        hardTruth -> Tone.HARD
        else -> Tone.GENTLE
    }

    /** The receipt's closing punch line. */
    fun conclusion(tone: Tone): String = when (tone) {
        Tone.HARD -> "You worked for the attention economy today. Unpaid."
        Tone.QUIRKY -> "Plot twist: today, you were the product. 🫠"
        Tone.GENTLE -> "Your time created monetizable attention. You were paid with distraction."
    }

    /** Short line for the daily notification body / share card. */
    fun tagline(tone: Tone): String = when (tone) {
        Tone.HARD -> "Your scrolling created value. You were paid nothing."
        Tone.QUIRKY -> "You did unpaid overtime for the algorithm. 💸"
        Tone.GENTLE -> "Your attention had value. Today it was monetized by others."
    }
}
