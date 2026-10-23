# Noir Music

A minimalist, dark-theme Android music player for local audio files on your phone, with automatic synced/plain lyrics from [LRCLIB](https://lrclib.net). No ads, accounts, analytics, streaming, or custom backend.

## Tech Stack
- **Kotlin 2.4.21**, **Jetpack Compose** (`compose-bom:2026.09.00`, Material 3)
- **AndroidX Media3 1.11.1** (`ExoPlayer`, `MediaSessionService`, `MediaController`)
- **MediaStore** for local audio discovery (`READ_MEDIA_AUDIO` on Android 13+, `READ_EXTERNAL_STORAGE` on Android 8–12L)
- **OkHttp 5.5.1** + **kotlinx-serialization-json 1.12.0** for LRCLIB lookups
- `minSdk = 26` (Android 8.0), `targetSdk = 36` (Android 16), `compileSdk = 37`

## Prerequisites
1. **JDK 17+** (Android Studio's bundled JBR 21 or any JDK 17+ on `JAVA_HOME` / `PATH`).
2. **Android SDK** with **Android SDK Platform 37** (`platforms;android-37.0`) and **SDK Build-Tools 36.0.0** installed.
3. `local.properties` in the project root pointing to your SDK:
   ```properties
   sdk.dir=C\:\\Users\\<You>\\AppData\\Local\\Android\\Sdk
   ```
4. *(Optional)* Set `noirMusic.clientContact=<your-github-url-or-email>` in `gradle.properties` to include a contact in the `User-Agent` header sent to LRCLIB.

## Build & Run Unit Tests (Windows PowerShell)
```powershell
cd "F:\Projects\Music Player"
.\gradlew.bat --version
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```
- **Unit test HTML report:** `app\build\reports\tests\testDebugUnitTest\index.html`
- **Debug APK output:** `app\build\outputs\apk\debug\app-debug.apk`

## Install the Debug APK on a Phone
### Option A — via USB & `adb`
1. On your phone: **Settings → About phone → tap Build number 7 times** to unlock Developer options.
2. **Settings → System → Developer options → enable USB debugging**.
3. Connect the phone via USB and accept the computer's RSA key prompt.
4. Run:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
   ```

### Option B — manual file transfer
Copy `app\build\outputs\apk\debug\app-debug.apk` to the phone and open it from your file manager (allow "Install unknown apps" if prompted).

## Opening in Android Studio
1. **File → Open…** and select `F:\Projects\Music Player`.
2. Let Gradle sync finish, select a connected device or emulator, and click **Run 'app'**.

## Notes on OEM Battery Management
Some phones (e.g. Xiaomi MIUI/HyperOS, Samsung One UI, Huawei, OnePlus) aggressively stop background services by default. If playback stops while the screen is off for a long time, set **Noir Music → Battery → Unrestricted** in Android system settings.
