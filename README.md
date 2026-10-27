# Noir Music

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-black?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/AndroidX%20Media3-1.11.1-2BA86D)](https://developer.android.com/media/media3)
[![License](https://img.shields.io/badge/License-MIT-gray.svg)](LICENSE)

A personal, ad-free, offline-first local music player for Android featuring a modern minimalist dark cyber-bento Compose UI, automatic online synced lyrics from [LRCLIB](https://lrclib.net), custom playlists, sleep timer, and full lock screen / notification media controls.

No accounts, no streaming subscriptions, no tracking, and no ads.

---

## Key Features

- **Local & Offline-First Playback**
  - Instant discovery of local audio files via Android `MediaStore`.
  - Right-side **`0-9` and `A-Z` Fast-Scroll Index Rail** with tap and vertical drag scrubbing to jump directly to any song in large libraries without manual scrolling.
  - Playing a track from search results queues the full sorted library starting at that track so continuous playback is never limited to filtered search hits.
  - Supports modern granular storage permissions (`READ_MEDIA_AUDIO` on Android 13+ / API 33+, and backward-compatible `READ_EXTERNAL_STORAGE` on Android 8–12L).
  - Handles missing metadata, unknown artists, and fallback titles derived from file names.

- **Synchronized Lyrics via LRCLIB**
  - Automatic timestamped (`.lrc`) lyric retrieval with sub-second synchronization to actual Media3 playback position.
  - Smooth auto-scrolling with tactile active-line highlight and tap-to-seek support.
  - Graceful fallback to plain unsynced lyrics when timestamped lyrics are unavailable.
  - In-memory caching, offline resilience, and automatic request cancellation/deduplication when skipping tracks.

- **Custom Playlists**
  - Create, rename, and manage custom playlists stored locally in persistent JSON format.
  - Add or remove tracks easily from the library or directly from the Now Playing sheet.
  - Dedicated Bento-style playlist detail view with track count, total duration, and one-tap **"Putar Semua" (Play All)** & **Shuffle** actions.

- **Sleep Timer (Timer Tidur)**
  - Automatically stop playback after a set duration: **15, 30, 45, 60 minutes**, or custom minutes.
  - **"End of Track"** option to finish the currently playing song before stopping.
  - Interactive timer controls: extend time (+5m / +15m) or cancel directly from the Now Playing screen.

- **Notification & Lock Screen Media Controls**
  - Standard playback controls: Play/Pause, Previous, Next, and Seek Bar.
  - Integrated custom notification actions: **Toggle Shuffle** and **Cycle Repeat (Off / All / One)** accessible directly from the notification shade without unlocking or opening the app.

- **Minimalist Cyber-Bento Dark Theme (Stitch Inspired)**
  - True pitch-black (`#0A0A0C`) OLED background with subtle tactical borders (`#2A2A32`) and warm amber/gold accents (`#D4A373`).
  - Studio telemetry banner showing local storage archive status and scanned track counts.
  - Smooth transitions between Library, Playlist Details, Mini Player, and the full Now Playing sheet.

---

## Tech Stack & Architecture

- **Language:** Kotlin 2.4.21
- **UI Framework:** Jetpack Compose with Material 3 (`androidx.compose.material3`, BOM `2026.09.00`)
- **Media Engine:** AndroidX Media3 (`1.11.1`)
  - `ExoPlayer` instance owned exclusively by foreground `MediaSessionService` (`PlaybackService`).
  - UI binds asynchronously through `MediaController`.
  - Full player command delegation and custom actions for shuffle/repeat.
- **Audio Focus & Notifications:** Handled by Media3 `AudioAttributes` and `MediaNotification.Provider`.
- **Networking:** OkHttp 5.5.1 with connection pooling, custom descriptive `User-Agent`, rate-limit backoff, and JSON serialization (`kotlinx.serialization.json 1.12.0`).
- **Compatibility:** `minSdk = 26` (Android 8.0 Oreo), `targetSdk = 36` (Android 16), `compileSdk = 37`.

---

## Project Structure

```
Noir Music/
├── app/
│   ├── src/main/java/com/noirmusic/
│   │   ├── NoirApplication.kt          # Application entrypoint & dependency container
│   │   ├── MainActivity.kt             # Single Activity with Compose & permission handling
│   │   ├── data/
│   │   │   ├── lyrics/                 # LRCLIB API client, LRC parser & sync engine
│   │   │   ├── media/                  # MediaStore scanner & audio collection repository
│   │   │   ├── model/                  # Track, Lyrics, and UI models
│   │   │   └── playlist/               # Local JSON playlist repository & persistence
│   │   ├── playback/
│   │   │   ├── PlaybackService.kt      # MediaSessionService hosting ExoPlayer
│   │   │   ├── PlaybackConnection.kt   # Async MediaController connection manager
│   │   │   ├── MediaItemMapper.kt      # Track <-> MediaItem mapping & content URIs
│   │   │   ├── SleepTimerManager.kt    # Countdown timer & end-of-track sleep engine
│   │   │   └── PlayerLogic.kt          # Play/pause/repeat state resolution
│   │   └── ui/
│   │       ├── NoirApp.kt              # Root Composable & screen state coordinator
│   │       ├── library/                # Library, Search, Sort, and Playlist views
│   │       ├── player/                 # Now Playing, MiniPlayer, Lyrics pane & Sleep Timer dialog
│   │       └── theme/                  # Cyber-Bento dark color scheme, typography & shapes
│   └── src/test/java/com/noirmusic/    # Unit tests for LRC parser, lyrics, and logic
├── gradle/                             # Gradle wrapper & version catalogs
└── README.md
```

---

## Prerequisites

1. **JDK 17 or higher** (Android Studio's bundled JBR 21 or any OpenJDK 17+ on `PATH`).
2. **Android SDK Platform 37** (`platforms;android-37.0`) and **Build-Tools 36.0.0** installed.
3. Configure `local.properties` in the project root:
   ```properties
   sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk
   ```
4. *(Optional)* Set contact info in `gradle.properties` for the LRCLIB `User-Agent`:
   ```properties
   noirMusic.clientContact=https://github.com/YourUsername/Personal-Music-Player
   ```

---

## Build & Test Commands

Build the debug APK and run unit tests using PowerShell / Terminal:

```powershell
# Run all unit tests
.\gradlew.bat testDebugUnitTest

# Build debug APK
.\gradlew.bat assembleDebug

# Clean build and verify everything in one step
.\gradlew.bat clean testDebugUnitTest assembleDebug --console=plain
```

- **Debug APK Location:** `app\build\outputs\apk\debug\app-debug.apk`
- **Unit Test HTML Report:** `app\build\reports\tests\testDebugUnitTest\index.html`

---

## Installing on Your Device

### Method 1: Via ADB (USB / Wireless Debugging)
1. Enable **Developer Options** on your Android device (Settings → About Phone → tap *Build Number* 7 times).
2. Enable **USB Debugging** (Settings → System → Developer Options).
3. Connect your device via USB and run:
   ```powershell
   adb install -r "app\build\outputs\apk\debug\app-debug.apk"
   ```

### Method 2: Direct File Transfer
1. Copy `app-debug.apk` from `app\build\outputs\apk\debug\` to your phone's storage.
2. Open your phone's File Manager, tap `app-debug.apk`, and tap **Install** (allow installation from unknown sources if prompted).

---

## Device Permissions & OEM Battery Optimization

- **Audio Permission:** When first opening Noir Music, grant access to audio files when prompted (`READ_MEDIA_AUDIO` or storage permission) so the app can scan your local music library.
- **Background Playback on Aggressive OEMs:**
  Certain Android skins (Xiaomi HyperOS/MIUI, Samsung One UI, OnePlus OxygenOS, Huawei EMUI) may terminate long-running background services when the screen is off. If playback stops unexpectedly after an extended period:
  1. Open Android **Settings → Apps → Noir Music → Battery**.
  2. Select **Unrestricted** (or disable *Battery Optimization* / *MIUI Battery Saver*).

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
