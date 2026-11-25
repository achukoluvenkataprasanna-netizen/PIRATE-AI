package com.pirateai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GamePracticeState(
    val gameName: String = "",
    val objective: String = "",
    val controlNotes: String = "",
    val active: Boolean = false,
    val framesObserved: Int = 0,
    val objectsSeen: Int = 0,
    val successfulTrials: Int = 0,
    val failedTrials: Int = 0,
    val lastObservation: String = "No game profile configured",
    val recommendation: String = "Add the game name and its objective to begin."
)

/**
 * Generic, local practice notebook and observation engine.
 * It deliberately does not execute taps or control third-party games.
 */
object GameLearningEngine {
    private val mutable = MutableStateFlow(GamePracticeState())
    val state = mutable.asStateFlow()

    fun configure(name: String, objective: String, controls: String) {
        mutable.value = mutable.value.copy(
            gameName = name.trim(),
            objective = objective.trim(),
            controlNotes = controls.trim(),
            active = false,
            framesObserved = 0,
            objectsSeen = 0,
            successfulTrials = 0,
            failedTrials = 0,
            lastObservation = "Profile saved locally",
            recommendation = if (name.isBlank()) "Enter a game name." else "Start Vision, then begin a practice session."
        )
    }

    fun startPractice() {
        val current = mutable.value
        if (current.gameName.isBlank()) {
            mutable.value = current.copy(recommendation = "Enter the game's name first.")
            return
        }
        if (current.objective.isBlank()) {
            mutable.value = current.copy(recommendation = "Describe the game's objective first.")
            return
        }
        mutable.value = current.copy(active = true, recommendation = "Observe a few gameplay frames. No actions will be sent automatically.")
    }

    fun stopPractice() {
        mutable.value = mutable.value.copy(active = false, recommendation = "Practice paused. Your session counters remain visible.")
    }

    fun observe(detections: List<DetectionBox>) {
        val current = mutable.value
        if (!current.active) return
        val labels = detections.map { it.label }.distinct()
        val nextFrame = current.framesObserved + 1
        val objectCount = current.objectsSeen + detections.size
        val advice = when {
            detections.isEmpty() -> "No recognised objects in this frame. Try a clearer, less obstructed game screen."
            labels.any { it.contains("person", true) || it.contains("face", true) } ->
                "A person-like object is visible. Check the game's rules before deciding what action is useful."
            else -> "Observed: ${labels.take(4).joinToString()}. Compare this with your objective and record the result of a practice attempt."
        }
        mutable.value = current.copy(
            framesObserved = nextFrame,
            objectsSeen = objectCount,
            lastObservation = if (labels.isEmpty()) "Frame $nextFrame: no ML Kit labels" else "Frame $nextFrame: ${labels.joinToString()}",
            recommendation = advice
        )
    }

    fun recordTrial(success: Boolean) {
        val current = mutable.value
        if (!current.active) return
        mutable.value = if (success) current.copy(
            successfulTrials = current.successfulTrials + 1,
            recommendation = "Successful trial recorded. Note which rule or control helped."
        ) else current.copy(
            failedTrials = current.failedTrials + 1,
            recommendation = "Unsuccessful trial recorded. Review the objective and control notes before the next attempt."
        )
    }
}
