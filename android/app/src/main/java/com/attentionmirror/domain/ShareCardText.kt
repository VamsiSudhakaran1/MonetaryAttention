package com.attentionmirror.domain

/**
 * The text content of the shareable "Attention Receipt" card — the viral hook.
 * Pure data so it can be unit-tested and rendered identically by the bitmap
 * renderer (`ui/ShareCardRenderer`).
 */
data class ShareCardText(
    val title: String,
    val dateLabel: String,
    val stats: List<Pair<String, String>>,
    val conclusion: String,
    val footer: String,
) {
    companion object {
        fun fromReceipt(
            receipt: AttentionReceipt,
            dateLabel: String,
            hardTruth: Boolean,
        ): ShareCardText = ShareCardText(
            title = "My Unpaid Attention",
            dateLabel = dateLabel,
            stats = listOf(
                Formatting.minutes(receipt.totalMinutes) to "scrolling",
                "${receipt.estimatedAdsSeen}" to "ads estimated",
                Formatting.valueRange(
                    receipt.estimatedValueLowInr,
                    receipt.estimatedValueHighInr,
                ) to "value created",
                "₹${receipt.userReceivedInr}" to "returned to me",
            ),
            conclusion = Copy.tagline(hardTruth),
            footer = "Attention Mirror · estimated",
        )
    }
}
