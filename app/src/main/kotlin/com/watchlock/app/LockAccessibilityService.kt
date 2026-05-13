package com.watchlock.app

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility Service fallback for locking the screen.
 *
 * Used when Device Admin is not active (e.g., after a fresh reinstall).
 * User enables it once: Settings → Accessibility → Watch Lock → Enable
 *
 * GLOBAL_ACTION_LOCK_SCREEN is supported on Android 9+ (API 28+).
 */
class LockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "LockA11yService"

        @Volatile
        private var instance: LockAccessibilityService? = null

        /** Called by LockHelper — returns true if the service is running and locked. */
        fun lockScreen(): Boolean {
            val svc = instance ?: return false
            val result = svc.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            Log.d(TAG, "GLOBAL_ACTION_LOCK_SCREEN result: $result")
            return result
        }

        /** Returns true if this service is currently connected/running. */
        fun isEnabled(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        instance = this
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not needed for lock-only use case
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
