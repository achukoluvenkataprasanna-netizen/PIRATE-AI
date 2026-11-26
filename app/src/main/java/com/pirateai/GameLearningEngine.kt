package com.pirateai

import android.content.Context
import com.pirateai.lab.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class DemoControl(val name: String, val note: String)
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

/** Room-backed practice notebook. It never sends game actions autonomously. */
object GameLearningEngine {
    private val mutable = MutableStateFlow(GamePracticeState())
    val state = mutable.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var dao: GameLabDao? = null
    private var startedAt: Long = 0L

    fun initialize(context: Context) {
        if (dao != null) return
        scope.launch(Dispatchers.IO) {
            val database = GameLabDatabase.get(context)
            dao = database.dao()
            val profiles = database.dao().profiles()
            val recentDemos = profiles.firstOrNull()?.let { database.dao().demonstrations(it.gameId) }.orEmpty()
            withContext(Dispatchers.Main) {
                mutable.value = mutable.value.copy(
                    persistenceStatus = "Local database ready • ${profiles.size} saved profiles"
                )
                profiles.firstOrNull()?.let { p ->
                    mutable.value = mutable.value.copy(
                        gameId = p.gameId, gameName = p.title, objective = p.objective,
                        controlNotes = p.controlsJson, genre = runCatching { GameGenre.valueOf(p.genre) }.getOrDefault(GameGenre.UNKNOWN),
                        demonstrations = recentDemos.map { DemoControl(it.controlName, it.observation) }.takeLast(100),
                        recommendation = "Restored saved profile and ${recentDemos.size} demonstrations. Start a new practice session when ready."
                    )
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
        val current = mutable.value
        val id = current.gameId.ifBlank { UUID.nameUUIDFromBytes(clean.lowercase().toByteArray()).toString() }
        val next = current.copy(gameId=id, gameName=clean, objective=objective.trim(), controlNotes=controls.trim(),
            active=false, framesObserved=0, objectsSeen=0, successfulTrials=0, failedTrials=0,
            demonstrations=emptyList(), recommendation="Profile saved locally.")
        mutable.value = next
        scope.launch(Dispatchers.IO) {
            runCatching { dao?.saveProfile(GameProfileEntity(id, clean, next.genre.name, next.objective, next.controlNotes)) }
                .onSuccess { withContext(Dispatchers.Main) { mutable.value = mutable.value.copy(persistenceStatus="Profile saved in Room") } }
                .onFailure { withContext(Dispatchers.Main) { mutable.value = mutable.value.copy(persistenceStatus="Database save failed: ${it.message}") } }
        }
    }

    fun startPractice() {
        val c=mutable.value
        if (c.gameName.isBlank() || c.objective.isBlank()) {
            mutable.value=c.copy(recommendation="Enter the game name and objective first."); return
        }
        startedAt=System.currentTimeMillis()
        mutable.value=c.copy(active=true,framesObserved=0,objectsSeen=0,successfulTrials=0,failedTrials=0,
            recommendation="Practice session active. Observe gameplay and record demonstrations.")
    }

    fun stopPractice() {
        val c=mutable.value
        if (c.active && c.gameId.isNotBlank()) {
            val duration=((System.currentTimeMillis()-startedAt)/1000).coerceAtLeast(0)
            scope.launch(Dispatchers.IO) {
                runCatching { dao?.saveSession(PracticeSessionEntity(gameId=c.gameId,durationSeconds=duration,
                    attempts=c.successfulTrials+c.failedTrials,successes=c.successfulTrials,mistakes=c.failedTrials,
                    notes="Genre=${c.genre}; demos=${c.demonstrations.size}")) }
            }
        }
        mutable.value=c.copy(active=false,recommendation="Practice paused. Session results are being saved.")
    }

    fun observe(detections: List<DetectionBox>, visibleText: List<String> = emptyList()) {
        val c=mutable.value
        if (!c.active) return
        val labels=detections.map { it.label }.distinct()
        val guess=GenreDetector.detect(GameSignals(visibleText=visibleText,objectLabels=labels,
            hasTimer=visibleText.any { it.contains("time",true) || it.matches(Regex(".*\\d{1,2}:\\d{2}.*")) },
            hasScore=visibleText.any { it.contains("score",true) }, hasGrid=labels.any { it.contains("tile",true) || it.contains("grid",true) },
            hasVirtualJoystick=labels.any { it.contains("joystick",true) || it.contains("controller",true) }))
        val genre=if (guess.genre==GameGenre.UNKNOWN) c.genre else guess.genre
        val metrics=PracticeMetrics(c.successfulTrials+c.failedTrials,c.successfulTrials,c.failedTrials)
        val tips=StrategyAdvisor.suggest(genre,c.objective,metrics)
        val advice=tips.joinToString(" • ") { it.title+": "+it.explanation }
        mutable.value=c.copy(framesObserved=c.framesObserved+1,objectsSeen=c.objectsSeen+detections.size,
            genre=genre,genreConfidence=guess.confidence,
            lastObservation=if(labels.isEmpty()) "Frame observed • no object labels" else "Detected: ${labels.take(5).joinToString()}",
            recommendation=if(guess.genre==GameGenre.UNKNOWN) advice else "Genre estimate: ${guess.genre} (${(guess.confidence*100).toInt()}%). $advice")
        if (guess.genre != GameGenre.UNKNOWN && c.gameId.isNotBlank() && guess.genre != c.genre) {
            scope.launch(Dispatchers.IO) {
                runCatching { dao?.saveProfile(GameProfileEntity(c.gameId,c.gameName,guess.genre.name,c.objective,c.controlNotes)) }
            }
        }
    }

    fun recordTrial(success: Boolean) {
        val c=mutable.value; if(!c.active) return
        mutable.value=if(success)c.copy(successfulTrials=c.successfulTrials+1,
            recommendation="Success recorded. Keep the demonstration and strategy that worked.")
        else c.copy(failedTrials=c.failedTrials+1,
            recommendation="Mistake recorded. Review the state and change one practice variable.")
    }

    fun recordDemonstration(controlName: String, note: String) {
        val c=mutable.value
        if(!c.active || controlName.isBlank()) return
        val demo=DemoControl(controlName.trim(),note.trim())
        val updated=(c.demonstrations+demo).takeLast(100)
        mutable.value=c.copy(demonstrations=updated,
            recommendation="Demonstration saved: ${demo.name}. This records your example; it does not replay it automatically.")
        scope.launch(Dispatchers.IO) {
            runCatching { dao?.saveDemonstration(DemonstrationEntity(gameId=c.gameId,controlName=demo.name,observation=demo.note)) }
        }
    }

    fun setSpeed(speed: PracticeSpeed) { mutable.value=mutable.value.copy(practiceSpeed=speed) }
}
