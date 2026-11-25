"""ADB input controller. Disabled unless explicitly opted in."""
from __future__ import annotations
import os
import shutil
import subprocess

class InputDisabledError(RuntimeError):
    pass

class AdbController:
    def __init__(self, serial: str | None = None):
        self.serial = serial

    def _run_input(self, *args: str):
        if os.getenv("PIRATE_ENABLE_INPUT") != "1":
            raise InputDisabledError("Input is disabled. Set PIRATE_ENABLE_INPUT=1 only for your own authorized test app/device.")
        if shutil.which("adb") is None:
            raise RuntimeError("adb was not found on PATH")
        cmd = ["adb"]
        if self.serial:
            cmd += ["-s", self.serial]
        cmd += ["shell", "input", *args]
        subprocess.run(cmd, check=True, timeout=10)

    def tap(self, x: int, y: int):
        if x < 0 or y < 0:
            raise ValueError("coordinates must be non-negative")
        self._run_input("tap", str(int(x)), str(int(y)))

    def swipe(self, x1: int, y1: int, x2: int, y2: int, duration_ms: int = 300):
        if min(x1, y1, x2, y2) < 0 or not 50 <= duration_ms <= 5000:
            raise ValueError("coordinates must be non-negative and duration 50..5000 ms")
        self._run_input("swipe", str(int(x1)), str(int(y1)), str(int(x2)), str(int(y2)), str(int(duration_ms)))
