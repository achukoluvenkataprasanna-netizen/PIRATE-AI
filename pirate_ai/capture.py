"""Capture Android screenshots through ADB."""
from __future__ import annotations
import shutil
import subprocess
import numpy as np
import cv2

class AdbError(RuntimeError):
    pass

def capture_screen(serial: str | None = None):
    """Return the current screen as a BGR OpenCV image."""
    if shutil.which("adb") is None:
        raise AdbError("adb was not found. Install Android SDK Platform Tools and add adb to PATH.")
    cmd = ["adb"]
    if serial:
        cmd += ["-s", serial]
    cmd += ["exec-out", "screencap", "-p"]
    result = subprocess.run(cmd, capture_output=True, check=False)
    if result.returncode:
        raise AdbError(result.stderr.decode(errors="replace") or "ADB screenshot failed")
    image = cv2.imdecode(np.frombuffer(result.stdout, dtype=np.uint8), cv2.IMREAD_COLOR)
    if image is None:
        raise AdbError("Could not decode screenshot. Check the connected device.")
    return image
