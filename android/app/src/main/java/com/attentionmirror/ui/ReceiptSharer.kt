package com.attentionmirror.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.attentionmirror.domain.ShareCardText
import java.io.File
import java.io.FileOutputStream

/** Renders the share card to a PNG in cache and fires a system share sheet. */
object ReceiptSharer {

    fun share(context: Context, card: ShareCardText) {
        val bitmap = ShareCardRenderer.render(card)
        val dir = File(context.cacheDir, "shares").apply { mkdirs() }
        val file = File(dir, "attention_receipt.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "${card.conclusion}\n— Attention Mirror")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            Intent.createChooser(send, "Share your Attention Receipt")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
