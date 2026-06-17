package com.attentionmirror

import android.app.Application
import com.attentionmirror.notification.DailyReceiptScheduler
import com.attentionmirror.notification.DailyReceiptWorker

class AttentionMirrorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DailyReceiptWorker.ensureChannel(this)
        DailyReceiptScheduler.schedule(this)
    }
}
