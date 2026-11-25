"""A tiny explicit state machine for test/demo workflows."""
from enum import Enum, auto

class State(Enum):
    IDLE = auto()
    OBSERVE = auto()
    COMPLETE = auto()
    ERROR = auto()

class StateMachine:
    def __init__(self):
        self.state = State.IDLE

    def step(self, event: str):
        if event == "start" and self.state is State.IDLE:
            self.state = State.OBSERVE
        elif event == "done" and self.state is State.OBSERVE:
            self.state = State.COMPLETE
        elif event == "error":
            self.state = State.ERROR
        elif event == "reset":
            self.state = State.IDLE
        return self.state
