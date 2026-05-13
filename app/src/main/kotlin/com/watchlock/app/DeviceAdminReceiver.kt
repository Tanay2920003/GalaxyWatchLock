package com.watchlock.app

import android.app.admin.DeviceAdminReceiver as BaseDeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Device Admin Receiver — required to call DevicePolicyManager.lockNow().
 * The user must activate this once via Settings → Security → Device Admin.
 * The app will guide them automatically on first launch.
 */
class DeviceAdminReceiver : BaseDeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Watch Lock activated ✓", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Watch Lock deactivated", Toast.LENGTH_SHORT).show()
    }
}
