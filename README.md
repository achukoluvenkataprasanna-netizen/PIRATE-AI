# PIRATE-AI

A local-first Android screen-perception and UI-testing starter. It connects to an Android device or emulator through Android Debug Bridge (ADB), captures screenshots, runs basic OpenCV analysis, and supports explicitly enabled taps/swipes for apps and games you own or are authorized to test.

> This is a PC-side Python starter, not an installable Android APK. It does not bypass anti-cheat, hide automation, or automate third-party competitive games.

## Features
- ADB screenshot capture (PNG decoded with OpenCV).
- Basic image processing: HSV color-region detection and template matching.
- A small, observable state machine for demo workflows.
- ADB tap/swipe controller guarded by an explicit opt-in environment variable.
- Dry-run by default.

## Requirements
- Python 3.10+
- Android SDK Platform Tools (adb) installed and on PATH
- Android device with USB debugging enabled, or an Android emulator
- OpenCV and NumPy

## Setup
```bash
python -m venv .venv
# Windows:
.venv\\Scripts\\activate
# macOS/Linux:
source .venv/bin/activate
pip install -r requirements.txt
adb devices
```

Accept the USB debugging prompt on your device. Then run:
```bash
python -m pirate_ai
```

The default run only captures a screenshot and prints the current workflow state. It saves the image as `pirate_capture.png`.

## Input safety
The controller is disabled unless `PIRATE_ENABLE_INPUT=1` is set. Use only a dedicated test device/build that you own or are authorized to test.

Windows PowerShell:
```powershell
$env:PIRATE_ENABLE_INPUT = "1"
python -m pirate_ai --tap-demo
```

macOS/Linux:
```bash
export PIRATE_ENABLE_INPUT=1
python -m pirate_ai --tap-demo
```

The demo tap targets the center of the captured screen. Remove the opt-in variable to disable input again.

## Project layout
- `pirate_ai/capture.py` — ADB screenshot capture
- `pirate_ai/vision.py` — color-region and template detection
- `pirate_ai/controller.py` — guarded ADB input
- `pirate_ai/state_machine.py` — minimal finite-state machine
- `pirate_ai/__main__.py` — command-line entry point
- `tests/` — unit tests

## Run tests
```bash
pip install pytest
python -m pytest
```

## Limitations
This starter does not include a trained YOLO model, game-specific recognition, quest logic, or a native Android HUD. Detection thresholds and templates must be calibrated for your own app. Screen capture and input behavior vary by device and Android version.
