package com.attentionmirror.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.attentionmirror.domain.ShareCardText

/**
 * Draws a [ShareCardText] to a square bitmap for sharing — the viral hook. Pure
 * `android.graphics` (no Compose capture) so it renders identically off the UI
 * thread, e.g. from the daily worker.
 */
object ShareCardRenderer {

    private const val SIZE = 1080
    private const val MARGIN = 96f

    private val textPrimary = Color.parseColor("#F5F6FA")
    private val textMuted = Color.parseColor("#9AA3B2")
    private val accent = Color.parseColor("#FF5A5F")
    private val valueColor = Color.parseColor("#FFC857")

    fun render(card: ShareCardText): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Brand vertical gradient background.
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, SIZE.toFloat(),
                Color.parseColor("#1B2230"), Color.parseColor("#0B0E14"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), bgPaint)

        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val regular = Typeface.DEFAULT

        val titlePaint = paint(textPrimary, 64f, bold)
        val datePaint = paint(textMuted, 36f, regular)
        val numberPaint = paint(textPrimary, 96f, bold)
        val captionPaint = paint(textMuted, 36f, regular)
        val concPaint = paint(accent, 44f, bold)
        val footerPaint = paint(textMuted, 30f, regular)

        // Accent bar at the top — the brand mark.
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        canvas.drawRoundRect(RectF(MARGIN, MARGIN, MARGIN + 96f, MARGIN + 14f), 7f, 7f, barPaint)

        var y = MARGIN + 96f
        canvas.drawText(card.title, MARGIN, y, titlePaint)
        y += 56f
        canvas.drawText(card.dateLabel, MARGIN, y, datePaint)

        y += 96f
        // "value created" (3rd stat) is tinted amber so the money pops.
        card.stats.forEachIndexed { i, (statValue, caption) ->
            numberPaint.color = if (i == 2) valueColor else textPrimary
            canvas.drawText(statValue, MARGIN, y, numberPaint)
            y += 46f
            canvas.drawText(caption, MARGIN, y, captionPaint)
            y += 110f
        }

        // Conclusion, wrapped to the card width.
        y += 8f
        for (line in wrap(card.conclusion, concPaint, SIZE - 2 * MARGIN)) {
            canvas.drawText(line, MARGIN, y, concPaint)
            y += 56f
        }

        canvas.drawText(card.footer, MARGIN, SIZE - MARGIN, footerPaint)
        return bmp
    }

    private fun paint(color: Int, size: Float, typeface: Typeface) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        this.typeface = typeface
    }

    /** Greedy word-wrap to a pixel width. */
    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in text.split(' ')) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}
