# Pre-Release Fixes Applied

## Changes Summary

### 1. `.gitignore` — Added missing entries
Added Gradle caches, secrets/signing files, screenshot test outputs, and OS files to `.gitignore` to prevent accidental commits of sensitive or generated content.

### 2. `app/build.gradle.kts` — Enabled minification in release build
Changed `isMinifyEnabled = false` to `true` in the release build type. This enables ProGuard/R8 code shrinking, obfuscation, and optimization for production builds, reducing APK size and improving security.

### 3. `app/src/main/AndroidManifest.xml` — Removed unused `READ_EXTERNAL_STORAGE` permission
Removed the `READ_EXTERNAL_STORAGE` permission declaration. The app does not read external storage and this permission is unnecessary.

### 4. `app/src/main/AndroidManifest.xml` — Added permission protection to `AlarmReceiver`
Added `android:permission="android.permission.RECEIVE_BOOT_COMPLETED"` to the `AlarmReceiver` declaration. This ensures only the system can send boot-completed broadcasts to this receiver, preventing third-party apps from triggering alarms.

### 5. `app/src/main/java/com/example/ui/AlarmReceiver.kt` — WakeLock leak fix
Added a `finally` block to release the WakeLock after starting the alarm service, and an `else` branch to release it when `alarmId == -1L`. This prevents the WakeLock from being held indefinitely if an exception occurs or if the alarm ID is invalid. **Already applied from prior session.**

### 6. `app/src/main/java/com/example/MainActivity.kt` — Replaced deprecated `isServiceRunning`
Replaced the deprecated `ActivityManager.getRunningServices()` implementation with a `SharedPreferences` check that reads the `timer_is_running` flag written by `TimerService`. The old API is unreliable on Android 8+ and logs warnings.

### 7. `app/src/test/java/com/example/GreetingScreenshotTest.kt` — Removed broken test
Removed the `GreetingScreenshotTest` class that referenced a non-existent `Greeting()` composable. The file now contains a placeholder comment for future screenshot tests.

### 8. `app/src/test/java/com/example/ExampleRobolectricTest.kt` — Updated expected app name
Changed the expected app name from `"My Application"` to `"Liquid Clock"` to match the current `strings.xml` value.

### 9. `metadata.json` — Removed misleading capability declaration
Removed `"MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API"` from `majorCapabilities` array. This capability is not implemented in the current app.
