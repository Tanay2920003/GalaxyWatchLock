package com.watchlock.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts SleepLockService after the watch reboots,
 * if the user had it enabled.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("watchlock", Context.MODE_PRIVATE)
            val sleepEnabled = prefs.getBoolean("sleep_lock_enabled", false)

            if (sleepEnabled) {
                val serviceIntent = Intent(context, SleepLockService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
