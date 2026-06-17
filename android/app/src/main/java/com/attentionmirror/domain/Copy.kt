package com.attentionmirror.domain

/**
 * Tone-aware copy. The math never changes; only the wording does. The default
 * is respectful (works for senior citizens, homemakers, students). The stronger
 * "hard truth" wording is shown only after the user opts in (Settings). See the
 * "Tone" section in `docs/ESTIMATE_SPEC.md`.
 */
object Copy {

    /** The receipt's closing punch line. */
    fun conclusion(hardTruth: Boolean): String =
        if (hardTruth) {
            "You worked for the attention economy today. Unpaid."
        } else {
            "Your time created monetizable attention. You were paid with distraction."
        }

    /** Short line for the daily notification body / share card. */
    fun tagline(hardTruth: Boolean): String =
        if (hardTruth) {
            "Your scrolling created value. You were paid ₹0."
        } else {
            "Your attention had value. Today it was monetized by others."
        }
}
