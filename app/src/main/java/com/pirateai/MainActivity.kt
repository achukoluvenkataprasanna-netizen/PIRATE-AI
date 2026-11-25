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
        setContent { PirateDashboard(::requestCapture, ::stopCapture, ::openAccessibilitySettings) }
    }

    private fun requestCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun stopCapture() {
        startService(Intent(this, ScreenCaptureService::class.java).apply { action = ScreenCaptureService.ACTION_STOP })
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}

@Composable
private fun PirateDashboard(onStart: () -> Unit, onStop: () -> Unit, onAccessibility: () -> Unit) {
    val state by VisionState.state.collectAsStateWithLifecycle()
    val bg = Color(0xFF071019)
    val cyan = Color(0xFF00E6B4)
    MaterialTheme(colorScheme = darkColorScheme(primary = cyan, background = bg, surface = Color(0xFF10212B))) {
        Column(
            Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("PIRATE-AI", style = MaterialTheme.typography.headlineMedium, color = cyan)
            Text("ANDROID • LIVE VISUAL ANALYSIS & ASSISTED CONTROLS", color = Color.LightGray)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onStart, enabled = !state.running, modifier = Modifier.weight(1f)) { Text("START VISION") }
                OutlinedButton(onClick = onStop, enabled = state.running, modifier = Modifier.weight(1f)) { Text("STOP") }
            }
            OutlinedButton(onClick = onAccessibility, modifier = Modifier.fillMaxWidth()) {
                Text("OPEN ANDROID ACCESSIBILITY SETTINGS")
            }
            Text("Enable “PIRATE-AI screen controls” only if you want to test assisted screen actions. Android will ask you to approve it there.", color = Color.LightGray)
            Text(state.status, color = if (state.running) cyan else Color.LightGray)
            Text("CAPTURE PREVIEW + DETECTION BOXES", color = cyan, style = MaterialTheme.typography.titleSmall)
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(320.dp).background(Color(0xFF14232D)),
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
                    pair.first.setImageBitmap(state.bitmap)
                    if (state.frameWidth > 0 && state.frameHeight > 0) pair.second.setDetections(state.detections, state.frameWidth, state.frameHeight)
                    else pair.second.clear()
                }
            )
            Text("OBJECTS DETECTED: " + state.detections.size, color = cyan)
            state.detections.forEachIndexed { index, item ->
                val confidence = item.confidence?.let { " • " + (it * 100).toInt() + "%" } ?: ""
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10212B))) {
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Text((index + 1).toString() + ". " + item.label + confidence, color = cyan)
                        Text("Box: " + item.left.toInt() + ", " + item.top.toInt() + " — " + item.right.toInt() + ", " + item.bottom.toInt(),
                            color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("Screen capture and accessibility are separate Android permissions. Actions must be deliberate and confirmed; this app does not silently send messages, place orders, or make payments.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}