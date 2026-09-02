<div align="center">

#  Liquid Clock

**A premium Android clock app with glassmorphism UI, programmatic audio synthesis, and haptic feedback.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Room-2.7.0-3DDC84?logo=android&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Android SDK](https://img.shields.io/badge/Min%20SDK-24-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/nougat)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/16)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)



</div>


---

##  Key Features

| Feature | Description |
|---------|-------------|
| **True Black AMOLED UI** | Glassmorphism design with custom `glassCard`, `glassPill`, and `glassHigh` Compose modifiers on pure black backgrounds |
| **Analog + Digital Clock** | Large analog face with liquid glass effects, digital readout, and live date/day display |
| **Persistent Alarms** | Create, edit, delete, and toggle alarms with repeat days, custom labels, sound picker, and snooze (1–15 min) |
| **Lock-Screen Alerting** | Full-screen `AlarmRingingActivity` with heads-up notification fallback, Snooze/Dismiss actions |
| **Countdown Timer** | Hours/minutes/seconds picker, presets (1–60 min), foreground service, haptic countdown (final 5 sec) |
| **Stopwatch** | Start/pause/reset with lap tracking and millisecond precision |
| **World Clock** | Add up to 9 cities, drag-to-reorder, live UTC offset display |
| **Programmatic Audio** | 5 synthesized alarm tones via `LiquidSoundSynth` using `AudioTrack` (no bundled audio files) |
| **Graduated Haptics** | Per-feature vibration intensity scaling (1–255), double-beat waveform for final countdown second |
| **Onboarding** | First-launch wizard for clock face preference, alarm demo, and haptic settings |

---

##  Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Language** | Kotlin | 2.2.10 |
| **UI Framework** | Jetpack Compose | BOM 2024.09.00 |
| **Design System** | Material 3 | (via Compose BOM) |
| **Icons** | Material Icons Extended | (via Compose BOM) |
| **Database** | Room | 2.7.0 |
| **Architecture** | MVVM (AndroidViewModel + StateFlow) | — |
| **Async** | Kotlinx Coroutines | 1.10.2 |
| **HTTP** | Retrofit + Moshi + OkHttp | 2.12.0 / 1.15.2 / 4.10.0 |
| **AI** | Firebase AI | BOM 34.12.0 |
| **Build** | Gradle Kotlin DSL + AGP | 9.1.1 |
| **Testing** | JUnit 4 + Robolectric + Roborazzi | 4.13.2 / 4.16.1 / 1.59.0 |
| **Secrets** | Secrets Gradle Plugin (.env) | 2.0.1 |

---

##  Architecture

```
┌─────────────────────────────────────────────────────────┐
│                       UI Layer                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │ AlarmTab │ │ TimerTab │ │Stopwatch │ │WorldClock│  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘  │
│       │             │            │             │         │
│       └─────────────┴────────────┴─────────────┘         │
│                          │                              │
│                  ClockViewModel                         │
│              (StateFlow + Coroutines)                    │
│                          │                              │
├──────────────────────────┼──────────────────────────────┤
│                    Data Layer                            │
│  ┌───────────────────────┴───────────────────────┐      │
│  │              ClockRepository                  │      │
│  └───────────────────────┬───────────────────────┘      │
│                          │                              │
│  ┌──────────┐    ┌───────┴──────┐    ┌──────────────┐  │
│  │Room DB   │    │SharedPrefs   │    │AlarmManager  │  │
│  │(Alarm +  │    │(Settings +   │    │(Exact alarms │  │
│  │WorldClock│    │ Timer state) │    │ + Doze mode) │  │
│  └──────────┘    └──────────────┘    └──────────────┘  │
│                                                         │
├─────────────────────────────────────────────────────────┤
│                   Service Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ AlarmService │  │TimerService  │  │LiquidSound   │  │
│  │ (Foreground) │  │ (Foreground) │  │Synth (Audio) │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

**Data Flow:** `UI (Compose)` → `ViewModel (StateFlow)` → `Repository` → `Room DB / SharedPreferences / AlarmManager`

---

##  Project Structure

```
liquid-clock/
├── .env.example                         # Secrets template (EMPTY — no real keys)
├── .gitignore                           # Comprehensive ignore rules
├── build.gradle.kts                     # Root build (plugin declarations)
├── settings.gradle.kts                  # Module settings
├── gradle.properties                    # JVM args, Kotlin daemon, AndroidX
├── gradle/libs.versions.toml            # Version catalog
├── app/
│   ├── build.gradle.kts                 # App build (minify ON, signing)
│   ├── proguard-rules.pro               # ProGuard rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml      # Permissions, services, receiver
│       │   └── java/com/example/
│       │       ├── MainActivity.kt          # Entry point, nav host, tabs
│       │       ├── ClockApplication.kt      # App init, exception handler
│       │       ├── HapticManager.kt         # Vibration helpers
│       │       ├── data/
│       │       │   ├── ClockDatabase.kt     # Room DB + DAOs + Entities
│       │       │   └── ClockRepository.kt   # Repository pattern
│       │       ├── ui/
│       │       │   ├── ClockViewModel.kt    # Main ViewModel (alarms, timers, clock)
│       │       │   ├── AlarmReceiver.kt     # BroadcastReceiver (boot + exact)
│       │       │   ├── AlarmScheduler.kt    # AlarmManager scheduling
│       │       │   ├── AlarmService.kt      # Foreground service for alarms
│       │       │   ├── AlarmRingingActivity.kt  # Full-screen alarm UI
│       │       │   ├── TimerService.kt      # Foreground service for timers
│       │       │   ├── TimerRingingActivity.kt  # Full-screen timer UI
│       │       │   ├── LiquidSoundSynth.kt  # AudioTrack tone synthesizer
│       │       │   ├── SoundHapticHelper.kt # Preloaded UI sounds
│       │       │   ├── screens/             # Feature screens
│       │       │   └── theme/               # Colors, Typography, Glass modifiers
│       │       └── res/values/strings.xml
│       └── test/java/com/example/
│           └── ExampleRobolectricTest.kt    # Unit test
```

---

##  Build & Install

### Prerequisites

- Android Studio Ladybug (2024.2.1) or later
- JDK 17+
- Android SDK 36

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/zohaibjabbar/liquid-clock.git
cd liquid-clock

# 2. Create a .env file for signing (optional — debug builds work without it)
cat > .env << EOF
KEYSTORE_PATH=my-upload-key.jks
STORE_PASSWORD=your_store_password
KEY_PASSWORD=your_key_password
EOF

# 3. Generate a release keystore (if needed)
keytool -genkeypair -v -keystore my-upload-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias upload -storepass password -keypass password

# 4. Build debug APK
./gradlew assembleDebug

# 5. Build release APK (requires .env with signing config)
./gradlew assembleRelease

# 6. Install on connected device
./gradlew installDebug
```

### Output

```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

---

##  Running Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.example.ExampleRobolectricTest"

# Run with coverage
./gradlew testDebugUnitTest

# Generate Roborazzi screenshots
./gradlew recordRoborazziDebug
```

### Test Coverage

| Test Type | Framework | Status |
|-----------|-----------|--------|
| Unit Tests | JUnit 4 + Robolectric | ✅ Active |
| Screenshot Tests | Roborazzi | ⚙️ Configured |
| UI Tests | Compose UI Test | ⚙️ Available |

---

##  Permissions

| Permission | Purpose | Required |
|------------|---------|----------|
| `VIBRATE` | Haptic feedback for alarms, timers, UI interactions | ✅ Yes |
| `RECEIVE_BOOT_COMPLETED` | Reschedule alarms after device reboot | ✅ Yes |
| `FOREGROUND_SERVICE` | Keep alarm/timer running in background | ✅ Yes |
| `WAKE_LOCK` | Prevent CPU sleep during alarm ringing | ✅ Yes |
| `SCHEDULE_EXACT_ALARM` | Precise alarm triggering (Android 12+) | ✅ Yes |
| `USE_EXACT_ALARM` | Alternative exact alarm permission | ⚡ Fallback |
| `POST_NOTIFICATIONS` | Show alarm/timer notifications (Android 13+) | ✅ Yes |
| `USE_FULL_SCREEN_INTENT` | Display lock-screen alarm overlay | ✅ Yes |
| `SYSTEM_ALERT_WINDOW` | Draw over other apps for alarm UI | ⚡ OEM devices |

---

## ⚙️ Build Configuration

| Setting | Value |
|---------|-------|
| `applicationId` | `com.aistudio.liquidclock.amrfyp` |
| `minSdk` | 24 (Android 7.0 Nougat) |
| `targetSdk` | 36 (Android 16) |
| `compileSdk` | 36 |
| `versionCode` | 1 |
| `versionName` | 1.0 |
| `isMinifyEnabled` | `true` (release) |
| `isCrunchPngs` | `false` |
| `Java compatibility` | 11 |

---

##  Dependencies

```toml
# Core
androidx-core-ktx = "1.18.0"
androidx-lifecycle-runtime-ktx = "2.8.7"
androidx-lifecycle-viewmodel-compose = "2.8.7"
androidx-activity-compose = "1.10.1"
androidx-core-splashscreen = "1.0.1"

# Compose
compose-bom = "2024.09.00"
material3 = "(via BOM)"
material-icons-extended = "(via BOM)"

# Database
room-runtime = "2.7.0"
room-ktx = "2.7.0"
room-compiler = "2.7.0" # KSP

# Network
retrofit = "2.12.0"
converter-moshi = "2.12.0"
okhttp = "4.10.0"

# Firebase
firebase-bom = "34.12.0"
firebase-ai = "(via BOM)"

# Testing
junit = "4.13.2"
robolectric = "4.16.1"
roborazzi = "1.59.0"
```

---

## License

```
MIT License

Copyright (c) 2026 Zohaib Jabbar

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<div align="center">

**Built with by [Zohaib Jabbar](https://github.com/zohaibjabbar)**

</div>
