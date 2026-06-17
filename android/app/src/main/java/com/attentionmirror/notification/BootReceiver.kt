package com.attentionmirror.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arms the daily receipt notification after a device reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DailyReceiptScheduler.schedule(context)
        }
    }
}
