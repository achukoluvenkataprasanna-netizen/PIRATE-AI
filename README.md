# PIRATE-AI

PIRATE-AI contains a **standalone Android app** plus an optional Python development prototype. The Android app runs its screen analysis on the phone and does not require a PC or cloud AI service at runtime.

## Android app
- Kotlin + Jetpack Compose futuristic HUD.
- Android MediaProjection screen capture, started only after the Android system permission prompt.
- Foreground service with a persistent notification.
- **On-device object detection** using the bundled ML Kit object-detection model in stream mode.
- Object labels, confidence values (when available), and screen bounding-box coordinates appear in the live analysis panel.
- Frames are analyzed at a throttled rate to reduce device load.
- No network permission is declared; the app does not upload screen contents.
- No automatic taps, game controls, or anti-cheat bypass.

### Build an APK with GitHub Actions
1. Open the **Actions** tab in this repository.
2. Select **Build Android APK**.
3. Choose **Run workflow** if needed.
4. Open the completed run and download the `PIRATE-AI-debug-apk` artifact.
5. Extract the ZIP to get `app-debug.apk`, then install it on your Android device.

The workflow produces a debug APK, not a Play Store release or signed production build. Check that the workflow finishes successfully before installing.

### Use the Android app
1. Install and open PIRATE-AI.
2. Tap **START VISION**.
3. Approve Android's screen-sharing prompt.
4. Keep the app open to view the live object analysis results.
5. Tap **STOP** to end capture and analysis. You can also revoke screen sharing through Android.

Object detection is general-purpose. It may return generic labels or no labels for small, stylized, or game-specific objects. Bounding boxes are coordinates in the captured screen, not game actions.

## Android build details
- JDK 17
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- Compile SDK 35, minimum SDK 26
- ML Kit bundled object detection 17.0.2

## Optional Python prototype
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
This is an early on-device vision foundation, not a complete autonomous game AI. The detector identifies common object categories; it is not trained for a particular game, does not understand quests or game state, and does not control other apps. Device performance and detector results vary by hardware and screen content.
