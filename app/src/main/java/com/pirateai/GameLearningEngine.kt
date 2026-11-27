package com.pirateai

import android.content.Context
import com.pirateai.lab.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class DemoControl(
    val name: String,
    val note: String,
    val actionId: String = "",
    val stateBefore: String = "",
    val stateAfter: String = "",
    val sequenceIndex: Int = 0
)

data class GamePracticeState(
    val gameId: String = "",
    val gameName: String = "",
    val objective: String = "",
    val controlNotes: String = "",
    val genre: GameGenre = GameGenre.UNKNOWN,
    val genreConfidence: Float = 0f,
    val active: Boolean = false,
    val framesObserved: Int = 0,
    val objectsSeen: Int = 0,
    val successfulTrials: Int = 0,
    val failedTrials: Int = 0,
    val demonstrations: List<DemoControl> = emptyList(),
    val practiceSpeed: PracticeSpeed = PracticeSpeed.NORMAL,
    val lastObservation: String = "No game profile configured",
    val recommendation: String = "Add a game profile to begin.",
    val persistenceStatus: String = "Opening local database…"
)

/** Room-backed practice notebook. It records examples and never executes game actions. */
object GameLearningEngine {
    private val mutable = MutableStateFlow(GamePracticeState())
    val state = mutable.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Volatile private var dao: GameLabDao? = null
    private var startedAt: Long = 0L

    fun initialize(context: Context) {
        if (dao != null) return
        scope.launch(Dispatchers.IO) {
            runCatching {
                val database = GameLabDatabase.get(context)
                dao = database.dao()
                val profiles = database.dao().profiles()
                val selected = profiles.firstOrNull()
                val recentDemos = selected?.let { database.dao().demonstrations(it.gameId) }.orEmpty()
                withContext(Dispatchers.Main) {
                    mutable.value = mutable.value.copy(
                        persistenceStatus = "Local database ready • " + profiles.size + " saved profiles"
                    )
                    selected?.let { profile ->
                        mutable.value = mutable.value.copy(
                            gameId = profile.gameId,
                            gameName = profile.title,
                            objective = profile.objective,
                            controlNotes = profile.controlsJson,
                            genre = runCatching { GameGenre.valueOf(profile.genre) }.getOrDefault(GameGenre.UNKNOWN),
                            demonstrations = recentDemos.map {
                                DemoControl(it.controlName, it.observation, it.actionId, it.stateBefore, it.stateAfter, it.sequenceIndex)
                            },
                            recommendation = "Restored profile and " + recentDemos.size + " recorded steps."
                        )
                    }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    mutable.value = mutable.value.copy(persistenceStatus = "Database error: " + (error.message ?: "unknown"))
                }
            }
        }
    }

    fun configure(name: String, objective: String, controls: String) {
        val clean = name.trim()
        if (clean.isBlank()) {
            mutable.value = mutable.value.copy(recommendation = "Enter a game name.")
            return
        }
        val id = UUID.nameUUIDFromBytes(clean.lowercase().toByteArray()).toString()
        val current = mutable.value
        val next = current.copy(
            gameId = id, gameName = clean, objective = objective.trim(), controlNotes = controls.trim(),
            active = false, framesObserved = 0, objectsSeen = 0, successfulTrials = 0, failedTrials = 0,
            demonstrations = emptyList(), recommendation = "Profile saved locally."
        )
        mutable.value = next
        scope.launch(Dispatchers.IO) {
            runCatching {
                (dao ?: error("Local database is still initializing")).saveProfile(
                    GameProfileEntity(id, clean, next.genre.name, next.objective, next.controlNotes)
                )
                val saved = dao?.demonstrations(id).orEmpty()
                withContext(Dispatchers.Main) {
                    mutable.value = mutable.value.copy(
                        demonstrations = saved.map {
                            DemoControl(it.controlName, it.observation, it.actionId, it.stateBefore, it.stateAfter, it.sequenceIndex)
                        },
                        persistenceStatus = "Profile and demonstration history loaded from Room"
                    )
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    mutable.value = mutable.value.copy(persistenceStatus = "Database save failed: " + (it.message ?: "unknown"))
                }
            }
        }
    }

    fun startPractice() {
        val current = mutable.value
        if (current.gameName.isBlank() || current.objective.isBlank()) {
            mutable.value = current.copy(recommendation = "Enter the game name and objective first.")
            return
        }
        startedAt = System.currentTimeMillis()
        mutable.value = current.copy(
            active = true, framesObserved = 0, objectsSeen = 0, successfulTrials = 0, failedTrials = 0,
            recommendation = "Practice session active. Manual actions can now be recorded."
        )
    }

    fun stopPractice() {
        val current = mutable.value
        if (current.active && current.gameId.isNotBlank()) {
            val duration = ((System.currentTimeMillis() - startedAt) / 1000).coerceAtLeast(0)
            scope.launch(Dispatchers.IO) {
                runCatching {
                    (dao ?: error("Local database is still initializing")).saveSession(
                        PracticeSessionEntity(
                            gameId = current.gameId, durationSeconds = duration,
                            attempts = current.successfulTrials + current.failedTrials,
                            successes = current.successfulTrials, mistakes = current.failedTrials,
                            notes = "Genre=" + current.genre + "; demonstrations=" + current.demonstrations.size
                        )
                    )
                }
            }
        }
        mutable.value = current.copy(active = false, recommendation = "Practice paused. Session results are being saved.")
    }

    fun observe(detections: List<DetectionBox>, visibleText: List<String> = emptyList()) {
        val current = mutable.value
        if (!current.active) return
        val labels = detections.map { it.label }.distinct()
        val guess = GenreDetector.detect(
            GameSignals(
                visibleText = visibleText, objectLabels = labels,
                hasTimer = visibleText.any { it.contains("time", true) || Regex(".*\\d{1,2}:\\d{2}.*").matches(it) },
                hasScore = visibleText.any { it.contains("score", true) },
                hasGrid = labels.any { it.contains("tile", true) || it.contains("grid", true) },
                hasVirtualJoystick = labels.any { it.contains("joystick", true) || it.contains("controller", true) }
            )
        )
        val genre = if (guess.genre == GameGenre.UNKNOWN) current.genre else guess.genre
        val metrics = PracticeMetrics(
            current.successfulTrials + current.failedTrials,
            current.successfulTrials, current.failedTrials
        )
        val scoreText = visibleText.firstOrNull { it.contains("score", true) }
        val score = scoreText?.filter { it.isDigit() }?.toIntOrNull()
        val advice = StateAwareStrategyAdvisor.recommend(
            GameStateModel(genre, current.objective, visibleEntities = labels, score = score, playerNotes = current.controlNotes),
            metrics
        ).joinToString(" • ") { it.title + ": " + it.explanation }
        mutable.value = current.copy(
            framesObserved = current.framesObserved + 1,
            objectsSeen = current.objectsSeen + detections.size,
            genre = genre, genreConfidence = guess.confidence,
            lastObservation = if (labels.isEmpty()) "Frame observed • no object labels" else "Detected: " + labels.take(5).joinToString(),
            recommendation = if (guess.genre == GameGenre.UNKNOWN) advice else
                "Genre estimate: " + guess.genre + " (" + (guess.confidence * 100).toInt() + "%). " + advice
        )
    }

    fun recordTrial(success: Boolean) {
        val current = mutable.value
        if (!current.active) return
        mutable.value = if (success) current.copy(
            successfulTrials = current.successfulTrials + 1,
            recommendation = "Success recorded. Keep the approach that worked."
        ) else current.copy(
            failedTrials = current.failedTrials + 1,
            recommendation = "Mistake recorded. Review the state and change one practice variable."
        )
    }

    /** Records a real state transition from the built-in test game. */
    fun recordGameAction(
        actionId: String,
        controlName: String,
        stateBefore: String,
        stateAfter: String,
        source: String = "MANUAL"
    ) {
        val current = mutable.value
        if (!current.active || current.gameId.isBlank() || stateBefore == stateAfter) return
        val sequence = current.demonstrations.size + 1
        val demo = DemoControl(
            controlName, "Recorded " + source.lowercase() + " action " + actionId,
            actionId, stateBefore, stateAfter, sequence
        )
        mutable.value = current.copy(
            demonstrations = current.demonstrations + demo,
            recommendation = "Recorded step " + sequence + ": " + controlName + ". No automatic replay is enabled."
        )
        scope.launch(Dispatchers.IO) {
            runCatching {
                (dao ?: error("Local database is still initializing")).saveDemonstration(
                    DemonstrationEntity(
                        gameId = current.gameId, controlName = controlName,
                        observation = demo.note, sequenceIndex = sequence, actionId = actionId,
                        stateBefore = stateBefore, stateAfter = stateAfter, source = source
                    )
                )
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    mutable.value = mutable.value.copy(persistenceStatus = "Recorder save failed: " + (error.message ?: "unknown"))
                }
            }
        }
    }

    fun recordDemonstration(controlName: String, note: String) {
        val current = mutable.value
        if (!current.active || current.gameId.isBlank() || controlName.isBlank()) return
        val sequence = current.demonstrations.size + 1
        val demo = DemoControl(controlName.trim(), note.trim(), "NOTE", "", "", sequence)
        mutable.value = current.copy(demonstrations = current.demonstrations + demo)
        scope.launch(Dispatchers.IO) {
            runCatching {
                (dao ?: error("Local database is still initializing")).saveDemonstration(
                    DemonstrationEntity(
                        gameId = current.gameId, controlName = demo.name, observation = demo.note,
                        sequenceIndex = sequence, actionId = "NOTE"
                    )
                )
            }
        }
    }

    fun setSpeed(speed: PracticeSpeed) {
        mutable.value = mutable.value.copy(practiceSpeed = speed)
    }
}
