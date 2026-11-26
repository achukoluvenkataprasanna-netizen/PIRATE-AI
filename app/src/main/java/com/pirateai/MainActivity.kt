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
import com.pirateai.lab.PracticeSafety
import com.pirateai.lab.PracticeSpeed

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
        setContent { PirateDashboard(::requestCapture, ::stopCapture, ::openAccessibilitySettings) }
    }
    private fun requestCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(manager.createScreenCaptureIntent())
    }
    private fun stopCapture() {
        PracticeSafety.disarm()
        startService(Intent(this, ScreenCaptureService::class.java).apply { action = ScreenCaptureService.ACTION_STOP })
    }
    private fun openAccessibilitySettings() { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
}

@Composable
private fun PirateDashboard(onStart: () -> Unit, onStop: () -> Unit, onAccessibility: () -> Unit) {
    val state by VisionState.state.collectAsStateWithLifecycle()
    val practice by GameLearningEngine.state.collectAsStateWithLifecycle()
    var gameName by remember { mutableStateOf("") }
    var objective by remember { mutableStateOf("") }
    var controls by remember { mutableStateOf("") }
    var demoControl by remember { mutableStateOf("") }
    var demoNote by remember { mutableStateOf("") }
    var ownTestAppConfirmed by remember { mutableStateOf(false) }
    val bg = Color(0xFF071019)
    val cyan = Color(0xFF00E6B4)
    MaterialTheme(colorScheme = darkColorScheme(primary = cyan, background = bg, surface = Color(0xFF10212B))) {
        Column(Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("PIRATE-AI", style = MaterialTheme.typography.headlineMedium, color = cyan)
            Text("UNIVERSAL GAME LAB • ON-DEVICE PRACTICE", color = Color.LightGray)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onStart, enabled = !state.running, modifier = Modifier.weight(1f)) { Text("START VISION") }
                OutlinedButton(onClick = onStop, enabled = state.running, modifier = Modifier.weight(1f)) { Text("STOP") }
            }
            OutlinedButton(onClick = onAccessibility, modifier = Modifier.fillMaxWidth()) { Text("OPEN ACCESSIBILITY SETTINGS") }
            Text(state.status, color = if (state.running) cyan else Color.LightGray)
            Text("LIVE SCREEN + DETECTION OVERLAY", color = cyan, style = MaterialTheme.typography.titleSmall)
            AndroidView(modifier = Modifier.fillMaxWidth().height(280.dp).background(Color(0xFF14232D)),
                factory = { context -> FrameLayout(context).apply {
                    val image = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
                    val overlay = DetectionOverlayView(context)
                    addView(image, FrameLayout.LayoutParams(-1, -1)); addView(overlay, FrameLayout.LayoutParams(-1, -1))
                    tag = Pair(image, overlay)
                } },
                update = { frame ->
                    @Suppress("UNCHECKED_CAST")
                    val pair = frame.tag as Pair<ImageView, DetectionOverlayView>
                    pair.first.setImageBitmap(state.bitmap)
                    if (state.frameWidth > 0 && state.frameHeight > 0) pair.second.setDetections(state.detections, state.frameWidth, state.frameHeight)
                    else pair.second.clear()
                })
            Text("Objects: ${state.detections.size} • OCR lines: ${state.visibleText.size}", color = cyan)
            state.visibleText.take(5).forEach { Text("OCR: $it", color = Color.LightGray, style = MaterialTheme.typography.bodySmall) }

            Divider(color = Color.DarkGray)
            Text("GAME PROFILE & MEMORY", color = cyan, style = MaterialTheme.typography.titleLarge)
            Text(practice.persistenceStatus, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value=gameName,onValueChange={gameName=it},modifier=Modifier.fillMaxWidth(),label={Text("Game name")},singleLine=true)
            OutlinedTextField(value=objective,onValueChange={objective=it},modifier=Modifier.fillMaxWidth(),label={Text("Objective / rules")},minLines=2)
            OutlinedTextField(value=controls,onValueChange={controls=it},modifier=Modifier.fillMaxWidth(),label={Text("Known controls / notes")},minLines=2)
            Button(onClick={GameLearningEngine.configure(gameName,objective,controls)},modifier=Modifier.fillMaxWidth()){Text("SAVE PROFILE TO ROOM")}
            Text("Profile: ${practice.gameName.ifBlank { "Not configured" }}",color=cyan)
            Text("Detected genre: ${practice.genre} • confidence ${(practice.genreConfidence*100).toInt()}%",color=cyan)
            Text("Frames: ${practice.framesObserved} • objects: ${practice.objectsSeen}",color=Color.LightGray)
            Text("Success: ${practice.successfulTrials} • mistakes: ${practice.failedTrials}",color=Color.LightGray)
            Text(practice.lastObservation,color=Color.LightGray)
            Text(practice.recommendation,color=cyan)

            Text("DEMONSTRATION RECORDER",color=cyan,style=MaterialTheme.typography.titleMedium)
            OutlinedTextField(value=demoControl,onValueChange={demoControl=it},modifier=Modifier.fillMaxWidth(),label={Text("Control demonstrated (e.g. jump)")},singleLine=true)
            OutlinedTextField(value=demoNote,onValueChange={demoNote=it},modifier=Modifier.fillMaxWidth(),label={Text("What happened / when to use it")},minLines=2)
            OutlinedButton(onClick={GameLearningEngine.recordDemonstration(demoControl,demoNote);demoControl="";demoNote=""},
                enabled=practice.active && demoControl.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("SAVE DEMONSTRATION")}
            Text("Recorded demonstrations: ${practice.demonstrations.size}",color=Color.LightGray)
            practice.demonstrations.takeLast(4).forEach { Text("• ${it.name}: ${it.note}",color=Color.LightGray,style=MaterialTheme.typography.bodySmall) }

            Text("PRACTICE CONTROLS",color=cyan,style=MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={GameLearningEngine.startPractice()},enabled=!practice.active,modifier=Modifier.weight(1f)){Text("START")}
                OutlinedButton(onClick={GameLearningEngine.stopPractice()},enabled=practice.active,modifier=Modifier.weight(1f)){Text("PAUSE & SAVE")}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                PracticeSpeed.values().forEach { speed ->
                    FilterChip(selected=practice.practiceSpeed==speed,onClick={GameLearningEngine.setSpeed(speed)},label={Text(speed.name)})
                }
            }
            Text("Observation interval: ${practice.practiceSpeed.intervalMs} ms",color=Color.LightGray,style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={GameLearningEngine.recordTrial(true)},enabled=practice.active,modifier=Modifier.weight(1f)){Text("SUCCESS")}
                OutlinedButton(onClick={GameLearningEngine.recordTrial(false)},enabled=practice.active,modifier=Modifier.weight(1f)){Text("MISTAKE")}
            }

            Divider(color=Color.DarkGray)
            Text("PRACTICE SAFETY",color=cyan,style=MaterialTheme.typography.titleMedium)
            Text("Gestures are disabled unless you explicitly confirm a user-owned test app. This build records demonstrations and recommendations; it does not replay them automatically.",color=Color.LightGray,style=MaterialTheme.typography.bodySmall)
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked=ownTestAppConfirmed,onCheckedChange={ownTestAppConfirmed=it})
                Text("I confirm this is my own app or an authorised test environment",color=Color.LightGray)
            }
            OutlinedButton(onClick={PracticeSafety.arm(ownTestAppConfirmed,ownTestAppConfirmed)},enabled=ownTestAppConfirmed,modifier=Modifier.fillMaxWidth()){Text("ARM SINGLE CONFIRMED TEST ACTION")}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={PracticeSafety.manualTakeover();GameLearningEngine.stopPractice()},modifier=Modifier.weight(1f)){Text("MANUAL TAKEOVER")}
                Button(onClick={PracticeSafety.emergencyStop();GameLearningEngine.stopPractice()},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFB3261E))){Text("EMERGENCY STOP")}
            }
            OutlinedButton(onClick={PracticeSafety.resetEmergencyStop(ownTestAppConfirmed)},enabled=ownTestAppConfirmed,modifier=Modifier.fillMaxWidth()){Text("RESET STOP (CONFIRM)")}
            Text("Screen capture and Accessibility are separate Android permissions. Manual takeover and emergency stop disarm the gesture gate.",color=Color.Gray,style=MaterialTheme.typography.bodySmall)
        }
    }
}
