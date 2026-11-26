package com.pirateai.lab

import org.junit.Assert.*
import org.junit.Test

class UniversalGameLabTest {
    @Test fun racingSignalsChooseRacing() {
        val guess = GenreDetector.detect(GameSignals(visibleText=listOf("Lap 2", "Nitro", "Finish"), hasTimer=true))
        assertEquals(GameGenre.RACING, guess.genre)
        assertTrue(guess.confidence > 0f)
    }

    @Test fun puzzleGridIsRecognised() {
        val guess = GenreDetector.detect(GameSignals(hasGrid=true))
        assertEquals(GameGenre.PUZZLE, guess.genre)
    }

    @Test fun unknownWhenNoEvidence() {
        val guess = GenreDetector.detect(GameSignals())
        assertEquals(GameGenre.UNKNOWN, guess.genre)
        assertEquals(0f, guess.confidence)
    }

    @Test fun successRateUsesAttempts() {
        val metrics = PracticeMetrics(attempts=4, successes=3, mistakes=1)
        assertEquals(.75f, metrics.successRate, .001f)
    }

    @Test fun strategyAdvisorReturnsGenreSpecificAndFeedback() {
        val tips = StrategyAdvisor.suggest(GameGenre.PUZZLE, "match tiles", PracticeMetrics(2,1,1))
        assertTrue(tips.any { it.title == "Plan before moving" })
        assertTrue(tips.any { it.title == "Practice feedback" })
    }

    @Test fun safetyGateRequiresConfirmationAndAllowlistedTestTarget() {
        PracticeSafety.resetEmergencyStop(true)
        assertFalse(PracticeSafety.arm(false, true))
        assertFalse(PracticeSafety.arm(true, false))
        assertTrue(PracticeSafety.arm(true, true))
        assertTrue(PracticeSafety.canPerformSingleConfirmedAction(true))
        PracticeSafety.manualTakeover()
        assertFalse(PracticeSafety.canPerformSingleConfirmedAction(true))
    }

    @Test fun emergencyStopDisarmsUntilExplicitReset() {
        PracticeSafety.resetEmergencyStop(true)
        assertTrue(PracticeSafety.arm(true, true))
        PracticeSafety.emergencyStop()
        assertFalse(PracticeSafety.canPerformSingleConfirmedAction(true))
        assertFalse(PracticeSafety.arm(true, true))
        PracticeSafety.resetEmergencyStop(true)
        assertFalse(PracticeSafety.canPerformSingleConfirmedAction(true))
    }
}
