from pirate_ai.state_machine import State, StateMachine

def test_basic_flow():
    machine = StateMachine()
    assert machine.step("start") is State.OBSERVE
    assert machine.step("done") is State.COMPLETE
    assert machine.step("reset") is State.IDLE
