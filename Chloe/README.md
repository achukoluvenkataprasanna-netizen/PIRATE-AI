# Chloe Android app

Chloe is a **separate Android application** in this repository. The existing PIRATE-AI app and its source tree are unchanged.

## Current scaffold
- Independent Android application ID: `com.chloe.assistant`
- Kotlin + Jetpack Compose
- Android min SDK 26, compile/target SDK 35
- Temporary human-shaped Compose illustration

## Planned integration
1. Human Chloe: replace the temporary illustration with a properly licensed 3D GLB avatar and an Android-compatible animation renderer (idle, walk, talk, facial/lip-sync states).
2. Voice: explicit microphone permission and foreground-service lifecycle.
3. Offline wake word: Porcupine integration after the developer supplies an AccessKey through a secure local property or GitHub Actions secret. No key is committed.
4. Screen reading and navigation: user-enabled Android Accessibility Service; visible permission and capture indicators; confirmation for consequential actions.
5. Camera: use Android's camera permission and an explicit capture action. Chloe must not take photos covertly.

## Build
From repository root:
```bash
gradle --no-daemon :Chloe:app:testDebugUnitTest :Chloe:app:assembleDebug
```

The APK is written to `Chloe/app/build/outputs/apk/debug/app-debug.apk`.

## Honest status
This commit establishes the independent app module and build path. It is **not yet** the full voice/vision assistant, a finished realistic 3D avatar, or an autonomous game-playing system. The placeholder controls are intentionally not wired to phone actions yet.
