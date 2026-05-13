package com.watchlock.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.wear.compose.material.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current

            // All state refreshed on every resume so it picks up any grants made outside the app
            var hasPermissions      by remember { mutableStateOf(checkAllPermissions(context)) }
            var isAdminActive       by remember { mutableStateOf(LockHelper.isAdminActive(context)) }
            var isA11yEnabled       by remember { mutableStateOf(LockHelper.isAccessibilityServiceEnabled()) }
            val canLock = isAdminActive || isA11yEnabled

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        hasPermissions = checkAllPermissions(context)
                        isAdminActive  = LockHelper.isAdminActive(context)
                        isA11yEnabled  = LockHelper.isAccessibilityServiceEnabled()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { _ ->
                hasPermissions = checkAllPermissions(context)
            }

            WatchLockApp(
                onLockNow            = { LockHelper.lockNow(context) },
                onSleepToggle        = { enabled -> toggleSleepLock(context, enabled) },
                onRequestAdmin       = { LockHelper.requestAdminActivation(context) },
                onRequestA11y        = { openAccessibilitySettings(context) },
                onRequestPermissions = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.BODY_SENSORS,
                            Manifest.permission.ACTIVITY_RECOGNITION
                        )
                    )
                },
                isSleepLockEnabled   = SleepLockService.isRunning,
                isAdminActive        = isAdminActive,
                isA11yEnabled        = isA11yEnabled,
                hasPermissions       = hasPermissions,
                canLock              = canLock
            )
        }
    }

    private fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun checkAllPermissions(context: Context): Boolean =
        checkPermission(context, Manifest.permission.BODY_SENSORS) &&
        checkPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)

    private fun checkPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun toggleSleepLock(context: Context, enable: Boolean) {
        context.getSharedPreferences("watchlock", Context.MODE_PRIVATE)
            .edit().putBoolean("sleep_lock_enabled", enable).apply()
        val intent = Intent(context, SleepLockService::class.java)
        if (enable) startForegroundService(intent) else stopService(intent)
    }
}

@Composable
fun WatchLockApp(
    onLockNow: () -> Unit,
    onSleepToggle: (Boolean) -> Unit,
    onRequestAdmin: () -> Unit,
    onRequestA11y: () -> Unit,
    onRequestPermissions: () -> Unit,
    isSleepLockEnabled: Boolean,
    isAdminActive: Boolean,
    isA11yEnabled: Boolean,
    hasPermissions: Boolean,
    canLock: Boolean
) {
    var sleepEnabled by remember { mutableStateOf(isSleepLockEnabled) }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 32.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title
        item {
            Text(
                text = "🔒 Watch Lock",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // ── Lock method status ──────────────────────────────────────────────

        // Admin active — green tick
        if (isAdminActive) {
            item {
                Chip(
                    onClick = { },
                    label = { Text("✓ Admin active", fontSize = 11.sp, color = Color(0xFF4ADE80)) },
                    secondaryLabel = { Text("Lock Now ready", fontSize = 9.sp, color = Color.Gray) },
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF002A10)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Admin not active — show tap-to-enable chip
            item {
                Chip(
                    onClick = onRequestAdmin,
                    label = {
                        Text("⚠ Tap: Enable Device Admin", fontSize = 11.sp, color = Color(0xFFFFCC00))
                    },
                    secondaryLabel = { Text("Primary lock method", fontSize = 9.sp, color = Color.Gray) },
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF2A2000)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Accessibility Service status
        if (!isAdminActive) {
            if (isA11yEnabled) {
                item {
                    Chip(
                        onClick = { },
                        label = { Text("✓ Accessibility active", fontSize = 11.sp, color = Color(0xFF4ADE80)) },
                        secondaryLabel = { Text("Fallback lock ready", fontSize = 9.sp, color = Color.Gray) },
                        colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF002A10)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                item {
                    Chip(
                        onClick = onRequestA11y,
                        label = {
                            Text("⚠ Tap: Enable Accessibility", fontSize = 11.sp, color = Color(0xFFFFAA00))
                        },
                        secondaryLabel = { Text("Fallback if Admin fails", fontSize = 9.sp, color = Color.Gray) },
                        colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF2A1500)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Health permissions
        if (!hasPermissions) {
            item {
                Chip(
                    onClick = onRequestPermissions,
                    label = {
                        Text("⚠ Tap: Grant Permissions", fontSize = 11.sp, color = Color(0xFFFFCC00))
                    },
                    secondaryLabel = { Text("Required for sleep detection", fontSize = 9.sp) },
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF2A2000)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Lock Now button
        item {
            Button(
                onClick = onLockNow,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (canLock) Color(0xFF1A6EFF) else Color(0xFF333333)
                )
            ) {
                Text(
                    text = "🔒  Lock Now",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Sleep Auto-Lock toggle
        item {
            ToggleChip(
                checked = sleepEnabled,
                onCheckedChange = { enabled ->
                    sleepEnabled = enabled
                    onSleepToggle(enabled)
                },
                enabled = hasPermissions && canLock,
                label = { Text("Sleep Auto-Lock", color = Color.White, fontSize = 13.sp) },
                secondaryLabel = {
                    Text(
                        if (sleepEnabled) "Locks when sleeping" else "Tap to enable",
                        color = Color.Gray, fontSize = 11.sp
                    )
                },
                toggleControl = { Switch(checked = sleepEnabled, onCheckedChange = null) },
                modifier = Modifier.fillMaxWidth(),
                colors = ToggleChipDefaults.toggleChipColors(
                    checkedStartBackgroundColor = Color(0xFF003A1E),
                    checkedEndBackgroundColor   = Color(0xFF004D27)
                )
            )
        }

        // Tile tip
        item {
            Text(
                text = "Add tile: Swipe left → + → Lock Now",
                color = Color(0xFF555555),
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
