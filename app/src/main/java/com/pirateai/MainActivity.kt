package com.pirateai

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.pirateai.lab.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val startIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, startIntent)
        } else VisionState.setStatus("Screen capture permission was not granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameLearningEngine.initialize(applicationContext)
        setContent { PirateDashboard(::requestCapture, ::stopCapture) }
    }

    private fun requestCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun stopCapture() {
        PracticeSafety.disarm()
        startService(Intent(this, ScreenCaptureService::class.java).apply { action = ScreenCaptureService.ACTION_STOP })
    }
}

@Composable
private fun PirateDashboard(onStart: () -> Unit, onStop: () -> Unit) {
    val vision by VisionState.state.collectAsStateWithLifecycle()
    val practice by GameLearningEngine.state.collectAsStateWithLifecycle()
    var gameName by remember { mutableStateOf("PIRATE-AI Treasure Grid") }
    var objective by remember { mutableStateOf("Reach the treasure at the bottom-right without entering blocked cells.") }
    var controls by remember { mutableStateOf("Use the directional buttons. The planner only suggests one move at a time.") }
    var demoControl by remember { mutableStateOf("") }
    var demoNote by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var miniGame by remember { mutableStateOf(TreasureGridState()) }
    var openRouterKey by remember { mutableStateOf("") }
    var remoteAdvice by remember { mutableStateOf("Optional cloud coach is off.") }
    var remoteLoading by remember { mutableStateOf(false) }
    val uiScope = rememberCoroutineScope()
    val suggested = remember(miniGame) { TreasureGridPlanner.suggest(miniGame) }
    val bg = Color(0xFF071019)
    val cyan = Color(0xFF00E6B4)

    fun movePlayer(move: GridMove, source: String = "MANUAL") {
        val before = miniGame
        val after = before.apply(move)
        if (after != before) {
            miniGame = after
            if (recording && practice.active) {
                GameLearningEngine.recordGameAction(
                    move.name, move.label, before.snapshot(), after.snapshot(), source
                )
            }
            if (after.completed) GameLearningEngine.recordTrial(true)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = cyan, background = bg, surface = Color(0xFF10212B))) {
        Column(
            Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("PIRATE-AI", style = MaterialTheme.typography.headlineMedium, color = cyan)
            Text("UNIVERSAL GAME LAB • ON-DEVICE PRACTICE", color = Color.LightGray)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onStart, enabled = !vision.running, modifier = Modifier.weight(1f)) { Text("START VISION") }
                OutlinedButton(onClick = onStop, enabled = vision.running, modifier = Modifier.weight(1f)) { Text("STOP VISION") }
            }
            Text(vision.status, color = if (vision.running) cyan else Color.LightGray)
            Text("LIVE SCREEN + DETECTION OVERLAY", color = cyan, style = MaterialTheme.typography.titleSmall)
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(220.dp).background(Color(0xFF14232D)),
                factory = { context ->
                    FrameLayout(context).apply {
                        val image = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
                        val overlay = DetectionOverlayView(context)
                        addView(image, FrameLayout.LayoutParams(-1, -1))
                        addView(overlay, FrameLayout.LayoutParams(-1, -1))
                        tag = Pair(image, overlay)
                    }
                },
                update = { frame ->
                    @Suppress("UNCHECKED_CAST")
                    val pair = frame.tag as Pair<ImageView, DetectionOverlayView>
                    pair.first.setImageBitmap(vision.bitmap)
                    if (vision.frameWidth > 0 && vision.frameHeight > 0) {
                        pair.second.setDetections(vision.detections, vision.frameWidth, vision.frameHeight)
                    } else pair.second.clear()
                }
            )
            Text("Objects: " + vision.detections.size + " • OCR lines: " + vision.visibleText.size, color = cyan)
            vision.visibleText.take(4).forEach {
                Text("OCR: " + it, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            }

            Divider(color = Color.DarkGray)
            Text("GAME PROFILE & LOCAL MEMORY", color = cyan, style = MaterialTheme.typography.titleLarge)
            Text(practice.persistenceStatus, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = gameName, onValueChange = { gameName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Game name") }, singleLine = true)
            OutlinedTextField(value = objective, onValueChange = { objective = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Objective / rules") }, minLines = 2)
            OutlinedTextField(value = controls, onValueChange = { controls = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Known controls / notes") }, minLines = 2)
            Button(onClick = { GameLearningEngine.configure(gameName, objective, controls) }, modifier = Modifier.fillMaxWidth()) {
                Text("SAVE PROFILE TO ROOM")
            }
            Text("Genre: " + practice.genre + " • confidence " + (practice.genreConfidence * 100).toInt() + "%", color = cyan)
            Text("Observed frames: " + practice.framesObserved + " • objects: " + practice.objectsSeen, color = Color.LightGray)
            Text("Success: " + practice.successfulTrials + " • mistakes: " + practice.failedTrials, color = Color.LightGray)
            Text(practice.lastObservation, color = Color.LightGray)
            Text(practice.recommendation, color = cyan)

            Divider(color = Color.DarkGray)
            Text("PRACTICE SESSION", color = cyan, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { GameLearningEngine.startPractice() },
                    enabled = !practice.active && practice.gameName.isNotBlank() && practice.objective.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("START PRACTICE") }
                OutlinedButton(
                    onClick = { recording = false; GameLearningEngine.stopPractice() },
                    enabled = practice.active, modifier = Modifier.weight(1f)
                ) { Text("PAUSE & SAVE") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PracticeSpeed.values().forEach { speed ->
                    FilterChip(
                        selected = practice.practiceSpeed == speed,
                        onClick = { GameLearningEngine.setSpeed(speed) },
                        label = { Text(speed.name) }
                    )
                }
            }
            Text("Vision sampling interval: " + practice.practiceSpeed.intervalMs + " ms", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)

            Divider(color = Color.DarkGray)
            Text("PIRATE-AI TEST GAME: TREASURE GRID", color = cyan, style = MaterialTheme.typography.titleLarge)
            Text("Move the marker to the treasure. Blocked cells cannot be entered. Every move is manual.", color = Color.LightGray)
            Text(
                "Player: (" + miniGame.player.x + "," + miniGame.player.y + ")  •  Moves: " + miniGame.moves +
                    (if (miniGame.completed) "  •  TREASURE FOUND" else ""),
                color = cyan
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (y in 0..4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (x in 0..4) {
                            val point = GridPoint(x, y)
                            val label = when {
                                point == miniGame.player -> "P"
                                point == miniGame.treasure -> "★"
                                point in miniGame.walls -> "■"
                                else -> "·"
                            }
                            Surface(
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                                color = when {
                                    point == miniGame.player -> Color(0xFF006B58)
                                    point == miniGame.treasure -> Color(0xFF755900)
                                    point in miniGame.walls -> Color(0xFF37434A)
                                    else -> Color(0xFF14232D)
                                },
                                shape = MaterialTheme.shapes.small
                            ) {
                                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { miniGame = TreasureGridState(); recording = false }, modifier = Modifier.weight(1f)) {
                    Text("RESET GAME")
                }
                OutlinedButton(
                    onClick = { recording = !recording },
                    enabled = practice.active && !miniGame.completed,
                    modifier = Modifier.weight(1f)
                ) { Text(if (recording) "STOP RECORDING" else "RECORD DEMO") }
            }
            Text(
                if (recording) "Recorder ON: manual moves are stored with before/after game states in Room."
                else "Recorder OFF: turn it on to save your manual demonstration sequence.",
                color = Color.LightGray, style = MaterialTheme.typography.bodySmall
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { movePlayer(GridMove.UP) }, enabled = !miniGame.completed, modifier = Modifier.weight(1f)) { Text("UP") }
                    Spacer(Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { movePlayer(GridMove.LEFT) }, enabled = !miniGame.completed, modifier = Modifier.weight(1f)) { Text("LEFT") }
                    Button(onClick = { movePlayer(GridMove.DOWN) }, enabled = !miniGame.completed, modifier = Modifier.weight(1f)) { Text("DOWN") }
                    Button(onClick = { movePlayer(GridMove.RIGHT) }, enabled = !miniGame.completed, modifier = Modifier.weight(1f)) { Text("RIGHT") }
                }
            }
            Text("GAME-SPECIFIC ACTION PLANNER", color = cyan, style = MaterialTheme.typography.titleMedium)
            Text(
                suggested?.let { "Suggested next action: " + it.move.label + " • " + it.remainingSteps + " steps remaining. " + it.explanation }
                    ?: if (miniGame.completed) "Objective complete." else "No route available from this state.",
                color = Color.LightGray
            )
            OutlinedButton(
                onClick = { suggested?.let { movePlayer(it.move, "PLANNER_APPROVED") } },
                enabled = suggested != null && !miniGame.completed,
                modifier = Modifier.fillMaxWidth()
            ) { Text("APPLY ONE SUGGESTED MOVE") }
            Text("The planner never taps or moves by itself. Each suggested step requires your tap.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            Text("Recorded steps: " + practice.demonstrations.size, color = cyan)
            practice.demonstrations.takeLast(5).forEach { item ->
                Text(
                    item.sequenceIndex.toString() + ". " + item.name + " [" + item.actionId + "] " +
                        item.stateBefore + " → " + item.stateAfter,
                    color = Color.LightGray, style = MaterialTheme.typography.bodySmall
                )
            }

            Divider(color = Color.DarkGray)
            Text("OPTIONAL OPENROUTER COACH", color = cyan, style = MaterialTheme.typography.titleMedium)
            Text("Cloud advice is opt-in. Send only a non-sensitive text summary; no screenshots or controls are shared.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = openRouterKey, onValueChange = { openRouterKey = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("OpenRouter API key (kept only in this screen)") },
                singleLine = true
            )
            Button(
                onClick = {
                    remoteLoading = true
                    uiScope.launch {
                        val summary = "Game: " + practice.gameName + ". Objective: " + practice.objective +
                            ". Genre: " + practice.genre + ". Recent game: Treasure Grid. Player at " +
                            miniGame.player.x + "," + miniGame.player.y + ". Suggested move: " +
                            (suggested?.move?.label ?: "none") + ". Demonstrations recorded: " + practice.demonstrations.size
                        val result = OpenRouterAdvisor().advise(openRouterKey.trim(), summary)
                        remoteAdvice = result.getOrElse { "Coach request failed: " + (it.message ?: "unknown error") }
                        remoteLoading = false
                    }
                },
                enabled = !remoteLoading && openRouterKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (remoteLoading) "ASKING COACH…" else "ASK COACH FOR TEXT ADVICE") }
            Text(remoteAdvice, color = Color.LightGray)
            Text("Never paste private information or share your API key. Remote advice is optional and cannot execute actions.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}
