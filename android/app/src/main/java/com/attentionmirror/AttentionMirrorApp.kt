package com.attentionmirror

import android.app.Application
import com.attentionmirror.data.SettingsStore
import com.attentionmirror.notification.DailyReceiptScheduler
import com.attentionmirror.notification.DailyReceiptWorker
import java.time.LocalTime

class AttentionMirrorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DailyReceiptWorker.ensureChannel(this)
        val settings = SettingsStore(this)
        DailyReceiptScheduler.schedule(
            this,
            LocalTime.of(settings.notificationHour, settings.notificationMinute),
        )
    }
}
