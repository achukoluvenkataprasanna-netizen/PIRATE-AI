package com.pirateai.lab

import org.junit.Assert.*
import org.junit.Test

class TreasureGridPlannerTest {
    @Test fun plannerFindsLegalRouteToTreasure() {
        val start = TreasureGridState()
        val route = TreasureGridPlanner.plan(start)
        assertTrue(route.isNotEmpty())
        var state = start
        route.forEach { move ->
            val next = state.apply(move)
            assertNotEquals("Planner returned a blocked or out-of-bounds move", state, next)
            state = next
        }
        assertTrue(state.completed)
    }

    @Test fun suggestedActionIsOnlyOneStep() {
        val suggestion = TreasureGridPlanner.suggest(TreasureGridState())
        assertNotNull(suggestion)
        assertTrue(suggestion!!.remainingSteps > 0)
        assertTrue(suggestion.explanation.contains("press APPLY"))
    }

    @Test fun completedGameHasNoFurtherAction() {
        val complete = TreasureGridState(player = GridPoint(4, 4))
        assertTrue(TreasureGridPlanner.plan(complete).isEmpty())
        assertNull(TreasureGridPlanner.suggest(complete))
        assertEquals(complete, complete.apply(GridMove.LEFT))
    }

    @Test fun illegalManualMoveDoesNotChangeState() {
        val start = TreasureGridState()
        assertEquals(start, start.apply(GridMove.UP))
        val afterRight = start.apply(GridMove.RIGHT)\n        assertEquals(afterRight, afterRight.apply(GridMove.DOWN))
    }

    @Test fun stateSnapshotContainsBeforeAndAfterCoordinates() {
        val before = TreasureGridState()
        val after = before.apply(GridMove.RIGHT)
        assertTrue(before.snapshot().contains("player=0,0"))
        assertTrue(after.snapshot().contains("player=1,0"))
        assertNotEquals(before.snapshot(), after.snapshot())
    }
}
