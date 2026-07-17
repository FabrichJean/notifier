package com.notifier.client.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notifier.client.AppPrefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (AppPrefs(context).isConfigured()) {
            NotifierWebSocketService.start(context)
        }
    }
}
