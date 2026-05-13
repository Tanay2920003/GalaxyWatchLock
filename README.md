# 🔒 AutoLock — Galaxy Watch Ultra

> **Automatically lock your Galaxy Watch Ultra when you fall asleep** — protecting OTPs and sensitive notifications from unauthorized access while you sleep.

Samsung's Galaxy Watch already locks when taken off your wrist. But **it stays unlocked while you're wearing it and sleeping** — leaving your OTPs, payment apps, and notifications exposed if someone picks up your hand. AutoLock solves this.

---

## 🛡️ The Problem This Solves

Samsung Galaxy Watch Ultra has a built-in **Wrist Detection** feature that locks the watch when removed from the wrist. However:

- ❌ While you're **sleeping and wearing it**, the watch remains **fully unlocked**
- ❌ Anyone nearby can swipe through your notifications, view OTPs, trigger payments
- ❌ No native Samsung option to auto-lock based on sleep state

**AutoLock detects when you fall asleep using Wear OS Health Services and immediately locks the watch**, adding a critical layer of security that Samsung doesn't provide out of the box.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🔒 **Lock Now** | Instantly lock the watch screen with one tap |
| 😴 **Sleep Auto-Lock** | Automatically locks when Wear OS detects you're asleep |
| 🔐 **OTP Protection** | Prevents access to OTPs and sensitive notifications while sleeping |
| ⌚ **Quick Tile** | Add a "Lock Now" tile to the tile carousel (swipe right from watch face) |
| 🔄 **Boot Persistence** | Sleep Auto-Lock restarts automatically after the watch reboots |
| 🛡️ **Dual Lock Methods** | Device Admin + Accessibility Service fallback for maximum compatibility |

---

## 📋 Requirements

- Samsung Galaxy Watch Ultra (or any Galaxy Watch running **One UI Watch 8** / Wear OS 4+)
- Android Studio (to build) **or** a pre-built APK
- ADB enabled on the watch (for one-time Device Admin grant)
- Watch PIN/Password set (required for lock screen to activate)

---

## 🚀 Setup Guide

### Step 1 — Enable Developer Options on the Watch

1. On your watch: **Settings → About watch → Software → tap Build number 7 times**
2. Go to **Settings → Developer options → ADB debugging → ON**
3. Enable **Wireless debugging** and note the IP address and port

### Step 2 — Connect ADB over Wi-Fi

On your PC (with [platform-tools](https://developer.android.com/tools/releases/platform-tools) installed):

```bash
adb connect <watch-ip>:<port>
# Example:
adb connect 192.168.1.42:41234
```

Verify connection:
```bash
adb devices
# Should show: <device-id>   device
```

### Step 3 — Build & Install the App

Clone this repo and open in Android Studio, then:

```bash
# Build debug APK
./gradlew assembleDebug

# Install to connected watch
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or install directly from Android Studio using **Run → Run 'app'**.

### Step 4 — Grant Device Admin (Required for Lock Now)

> ⚠️ This is the critical step. Samsung One UI 8 blocks the normal Device Admin UI flow for sideloaded apps. Run this once after every fresh install:

```bash
adb shell dpm set-active-admin com.watchlock.app/.DeviceAdminReceiver
```

Expected output:
```
Success: Active admin set to component com.watchlock.app/.DeviceAdminReceiver
```

> 🔁 **You must re-run this command after every reinstall** (Device Admin is cleared on uninstall). The app now shows a green "✓ Admin active" chip when it's working.

### Step 5 — Grant Health Permissions (Required for Sleep Auto-Lock)

On the watch, open the AutoLock app. If you see **"⚠ Tap: Grant Permissions"**, tap it and allow:
- **Body sensors**
- **Physical activity**

### Step 6 — Enable Sleep Auto-Lock

In the app, tap the **Sleep Auto-Lock** toggle. The watch will now automatically lock whenever Wear OS detects you've fallen asleep.

### Step 7 (Optional) — Add the Lock Tile

For quick one-tap locking from anywhere:
1. From the watch face, **swipe right** until you reach the end
2. Tap **+**
3. Select **Lock Now**

---

## 🔧 Fallback: Accessibility Service

If Device Admin isn't active (e.g., waiting for ADB connection), you can enable the Accessibility Service fallback directly on the watch:

1. **Settings → Accessibility → Downloaded apps → Watch Lock → Enable**
2. The app will show **"✓ Accessibility active"** — Lock Now will work immediately

This method survives reinstalls without needing ADB.

---

## 🏗️ Architecture

```
AutoLock
├── MainActivity.kt          — Compose UI, permission/admin state management
├── LockHelper.kt            — Lock strategy: Device Admin → Accessibility Service
├── LockAccessibilityService.kt — GLOBAL_ACTION_LOCK_SCREEN fallback
├── SleepLockService.kt      — Foreground service, Health Services sleep monitor
├── LockTileService.kt       — Wear OS Tile (swipe-right carousel)
├── DeviceAdminReceiver.kt   — Device Policy Manager receiver
└── BootReceiver.kt          — Restarts SleepLockService after reboot
```

### Lock Method Priority

```
Lock Now pressed
       │
       ▼
DevicePolicyManager.lockNow()  ←── Active if ADB grant was run
       │ (fails if admin not active)
       ▼
AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN  ←── Active if enabled in Settings
```

### Sleep Detection Flow

```
SleepLockService (foreground)
       │
       ▼
Wear OS Health Services PassiveListenerCallback
       │
       ▼
UserActivityState == USER_ACTIVITY_ASLEEP
       │
       ▼ (with 5-min cooldown to avoid repeated locks)
LockHelper.lockNow()
```

---

## ❓ Troubleshooting

### "Lock Now" does nothing
→ Run: `adb shell dpm set-active-admin com.watchlock.app/.DeviceAdminReceiver`  
→ Or enable the Accessibility Service fallback in Settings

### Admin chip still shows after running ADB command
→ Close and reopen the app (state refreshes on resume)

### Sleep Auto-Lock toggle is greyed out
→ Grant Body Sensors and Physical Activity permissions first (tap the yellow chip)

### Lock activates but no PIN screen appears
→ Make sure you have a **PIN or Password** set on the watch: **Settings → Security and privacy → Screen lock → PIN**

### ADB can't find the watch
→ Make sure watch and PC are on the same Wi-Fi network  
→ Re-enable Wireless Debugging on the watch  
→ Try: `adb disconnect && adb connect <ip>:<port>`

---

## 🔒 Privacy & Security

- **No internet permissions** — the app never phones home
- **No data collection** — sleep detection runs entirely on-device via Wear OS Health Services
- **Open source** — audit the code yourself

---

## 📄 License

MIT License — free to use, modify, and distribute.

---

## 🙏 How It's Different from Samsung's Built-in Feature

| Feature | Samsung Wrist Detection | AutoLock |
|---|---|---|
| Locks when watch removed | ✅ Yes | ✅ Yes (via Device Admin) |
| Locks while sleeping & wearing | ❌ No | ✅ **Yes** |
| OTP protection during sleep | ❌ No | ✅ **Yes** |
| Requires hardware action | ✅ Remove watch | ❌ Automatic |
| One UI 8 compatible | ✅ System feature | ✅ Via ADB admin grant |
