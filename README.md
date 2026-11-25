# PIRATE-AI

PIRATE-AI now contains a **standalone Android app** plus an optional Python development prototype. The Android app is designed to run on the phone; it does not require a PC or cloud AI service at runtime.

## Android app (primary)
- Kotlin + Jetpack Compose HUD.
- Android MediaProjection screen capture, started only after the system permission prompt.
- Foreground service with a persistent capture notification and Stop control.
- Captured frames are written to the app's private cache.
- No network permission and no model download in this starter.
- Touch automation is not active; the UI toggle is currently a placeholder.

### Build an APK with GitHub Actions
1. Open the **Actions** tab in this repository.
2. Select **Build Android APK**.
3. Choose **Run workflow** if it has not started automatically after a push.
4. Open the completed run and download the `PIRATE-AI-debug-apk` artifact.
5. Extract the ZIP to get `app-debug.apk`, then install it on your Android device. Android may ask you to allow installation from that source.

The workflow builds a debug APK. It is not a Play Store release or a signed production build.

### Build locally
Requirements: JDK 17, Android SDK Platform Tools/SDK, and Gradle 8.9.
```bash
gradle assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Python prototype (optional)
The `pirate_ai/` folder is a PC-side ADB/OpenCV prototype, not required by the Android app.

```bash
python -m venv .venv
# Windows: .venv\\Scripts\\activate
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
adb devices
python -m pirate_ai
```

The Python controller is disabled unless `PIRATE_ENABLE_INPUT=1` is explicitly set. Use only with apps and test environments you own or are authorized to test.

## Current limitations
This is an early foundation, not a complete autonomous game AI. The Android version currently provides the HUD and permission-based screen capture. On-device object detection, a configurable visual pipeline, game-specific state logic, and a real overlay controller remain future work. It does not bypass anti-cheat or disguise automation.
