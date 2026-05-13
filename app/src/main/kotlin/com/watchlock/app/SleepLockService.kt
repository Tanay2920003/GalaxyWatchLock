package com.watchlock.app

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.await

/**
 * Foreground service that monitors sleep state via Wear OS Health Services.
 *
 * When the user enters ASLEEP state, the watch locks automatically.
 */
class SleepLockService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isRegistered = false

    companion object {
        var isRunning = false
        private const val TAG = "SleepLockService"
        private const val NOTIF_ID = 1001
        private const val CHANNEL_ID = "sleep_lock_channel"

        // Minimum time between auto-locks to avoid repeated locks (5 minutes)
        private const val LOCK_COOLDOWN_MS = 5 * 60 * 1000L
        private var lastLockTime = 0L
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Activity listener callback
    // ──────────────────────────────────────────────────────────────────────────
    private val activityCallback = object : PassiveListenerCallback {

        override fun onUserActivityInfoReceived(info: UserActivityInfo) {
            val state = info.userActivityState
            Log.d(TAG, "User activity state: ${state.name}")

            if (state == UserActivityState.USER_ACTIVITY_ASLEEP) {
                val now = System.currentTimeMillis()
                if (now - lastLockTime > LOCK_COOLDOWN_MS) {
                    lastLockTime = now
                    Log.d(TAG, "Sleep detected (ASLEEP state) — locking watch")
                    LockHelper.lockNow(this@SleepLockService)
                }
            }
        }

        override fun onPermissionLost() {
            Log.w(TAG, "Permission lost — stopping sleep monitoring")
            stopSelf()
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Service lifecycle
    // ──────────────────────────────────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        startForeground(NOTIF_ID, buildNotification())
        registerSleepListener()
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        unregisterSleepListener()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ──────────────────────────────────────────────────────────────────────────
    // Health Services registration
    // ──────────────────────────────────────────────────────────────────────────
    private fun registerSleepListener() {
        serviceScope.launch {
            try {
                val healthClient = HealthServices.getClient(this@SleepLockService)
                val passiveClient = healthClient.passiveMonitoringClient

                // Check capability first
                val capabilities = passiveClient.getCapabilitiesAsync().await()
                val supportsUserActivity = capabilities.supportedUserActivityStates
                    .contains(UserActivityState.USER_ACTIVITY_ASLEEP)

                if (!supportsUserActivity) {
                    Log.w(TAG, "Sleep state not supported on this device")
                    return@launch
                }

                val config = PassiveListenerConfig.Builder()
                    .setShouldUserActivityInfoBeRequested(true)
                    .build()

                passiveClient.setPassiveListenerCallback(config, activityCallback)
                isRegistered = true
                Log.d(TAG, "Sleep state listener registered")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to register sleep listener: ${e.message}")
            }
        }
    }

    private fun unregisterSleepListener() {
        if (isRegistered) {
            serviceScope.launch {
                try {
                    val healthClient = HealthServices.getClient(this@SleepLockService)
                    healthClient.passiveMonitoringClient.clearPassiveListenerCallbackAsync().await()
                    isRegistered = false
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to unregister: ${e.message}")
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Notification (required for foreground service)
    // ──────────────────────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Sleep Lock",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors sleep to auto-lock the watch"
            setShowBadge(false)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Sleep Lock Active")
            .setContentText("Will lock when you fall asleep")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
