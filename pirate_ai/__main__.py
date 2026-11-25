"""PIRATE-AI command line entry point."""
import argparse
import cv2
from .capture import capture_screen
from .controller import AdbController
from .state_machine import StateMachine

def main():
    parser = argparse.ArgumentParser(description="PIRATE-AI Android UI testing starter")
    parser.add_argument("--serial", help="ADB device serial (optional)")
    parser.add_argument("--tap-demo", action="store_true", help="Tap screen center; requires PIRATE_ENABLE_INPUT=1")
    args = parser.parse_args()
    frame = capture_screen(args.serial)
    h, w = frame.shape[:2]
    print(f"Connected screen: {w}x{h}")
    cv2.imwrite("pirate_capture.png", frame)
    print("Saved screenshot to pirate_capture.png")
    machine = StateMachine()
    machine.step("start")
    print(f"Workflow state: {machine.state.name}")
    if args.tap_demo:
        print("Sending one center-screen test tap...")
        AdbController(args.serial).tap(w // 2, h // 2)
        print("Tap sent.")
    else:
        print("Dry run: no device input sent. Use --tap-demo only on your own authorized test app.")

if __name__ == "__main__":
    main()
