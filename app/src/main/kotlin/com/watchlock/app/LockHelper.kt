package com.watchlock.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Central utility for locking the Galaxy Watch screen.
 *
 * Primary method: DevicePolicyManager.lockNow()
 *   - Device Admin is granted once via ADB (survives reboots, lost on uninstall):
 *       adb shell dpm set-active-admin com.watchlock.app/.DeviceAdminReceiver
 *   - Or the user can tap the chip in the app to activate via Settings UI.
 *
 * Fallback: AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
 *   - Works once the user enables Watch Lock in Settings → Accessibility.
 */
object LockHelper {

    private const val TAG = "LockHelper"

    fun lockNow(context: Context): Boolean {
        // Primary: Device Admin lockNow()
        if (tryAdminLock(context)) return true

        // Fallback: Accessibility Service global action
        if (LockAccessibilityService.lockScreen()) return true

        Log.w(TAG, "All lock methods failed. Enable Device Admin or Accessibility Service.")
        return false
    }

    private fun tryAdminLock(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, DeviceAdminReceiver::class.java)
            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
                Log.d(TAG, "lockNow() succeeded")
                true
            } else {
                Log.d(TAG, "Device Admin not active")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "lockNow() failed: ${e.message}")
            false
        }
    }

    fun isAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, DeviceAdminReceiver::class.java)
        return dpm.isAdminActive(adminComponent)
    }

    fun isAccessibilityServiceEnabled(): Boolean =
        LockAccessibilityService.isEnabled()

    fun requestAdminActivation(context: Context) {
        val adminComponent = ComponentName(context, DeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Watch Lock needs admin access to lock your screen."
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
