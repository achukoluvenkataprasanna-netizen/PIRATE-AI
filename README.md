# PIRATE-AI

PIRATE-AI is an Android screen-vision app built with Kotlin, Jetpack Compose, Android MediaProjection and ML Kit object detection.

## Build the APK
Open the repository's **Actions** tab. The **Build Android APK** workflow builds a debug APK after each push to `main`, or can be started manually with **Run workflow**. Download the `PIRATE-AI-debug-apk` artifact and extract `app-debug.apk`.

You can also open the repository in Android Studio with JDK 17 and Android Gradle Plugin 8.7.3, then run the app configuration.

## Use
1. Install and open PIRATE-AI.
2. Tap **START VISION**.
3. Approve Android's system screen-capture prompt.
4. Return to the app. Its preview shows the captured screen with detection boxes and labels.
5. Tap **STOP** to end the capture session.

## What it does
- Captures the display only after explicit Android consent.
- Runs ML Kit object detection on captured frames on the device.
- Draws bounding boxes over the matching preview frame.
- Shows object labels, confidence when ML Kit supplies it, and bounding-box coordinates.
- Uses a foreground service and persistent notification while capture is active.

## Important: screen viewing is not touch control
MediaProjection grants screen capture only. It cannot tap buttons or control another app. This build intentionally does not inject touch events. Touch automation would require a separate, explicitly user-enabled Android Accessibility Service and should be restricted to accessibility use cases or apps/test environments you own or are authorized to test.

## Model and privacy notes
The detector processes frames locally; no screen images are uploaded by this app. The standard ML Kit object-detection artifact can use a model provided through Google Play services, so initial model availability may depend on Play services and network availability. The detector returns broad object categories, not game-specific understanding, and may omit labels/confidence for stylized or small objects.

Screen capture is not started automatically. Android asks for permission each session, and a persistent notification indicates when capture is active.
