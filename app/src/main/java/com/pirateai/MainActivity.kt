package com.pirateai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private var analysisState by mutableStateOf("Waiting for screen capture…")
    private var coreState by mutableStateOf("SYSTEM READY")

    private val analysisReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ScreenCaptureService.ACTION_ANALYSIS) {
                analysisState = intent.getStringExtra(ScreenCaptureService.EXTRA_ANALYSIS)
                    ?: "No objects detected."
                coreState = "LIVE ANALYSIS"
            }
        }
    }

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val service = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_DATA, result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service)
            else startService(service)
            coreState = "STARTING SCREEN ANALYSIS"
        } else {
            coreState = "SCREEN CAPTURE PERMISSION DENIED"
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(ScreenCaptureService.ACTION_ANALYSIS)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(analysisReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(analysisReceiver, filter)
        }
    }

    override fun onStop() {
        unregisterReceiver(analysisReceiver)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PirateHome(
                status = coreState,
                analysis = analysisState,
                onStartCapture = {
                    val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    captureLauncher.launch(manager.createScreenCaptureIntent())
                },
                onStopCapture = {
                    startService(Intent(this, ScreenCaptureService::class.java).apply {
                        action = ScreenCaptureService.ACTION_STOP
                    })
                    coreState = "CAPTURE STOPPED"
                    analysisState = "Screen analysis stopped."
                }
            )
        }
    }
}

@Composable
private fun PirateHome(
    status: String,
    analysis: String,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit
) {
    val background = Color(0xFF080D16)
    val panel = Color(0xFF111C2B)
    val cyan = Color(0xFF39D9FF)

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("PIRATE-AI", color = cyan, fontSize = 30.sp)
                Text("ANDROID VISION SYSTEM  •  ON-DEVICE MODE", color = Color.LightGray, fontSize = 11.sp)
                Surface(color = panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CORE STATUS", color = cyan)
                        Text(status, color = Color.White, fontSize = 19.sp)
                        Text("Screen analysis runs locally after you approve Android's screen-sharing prompt.", color = Color.LightGray)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onStartCapture, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087E9C))) {
                        Text("START VISION")
                    }
                    Button(onClick = onStopCapture, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344256))) {
                        Text("STOP")
                    }
                }
                Surface(color = panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("LIVE OBJECT ANALYSIS", color = cyan)
                        Text(analysis, color = Color.White, fontSize = 14.sp)
                        Text("Detector: ML Kit bundled on-device model • Stream mode • Classification enabled", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
                Surface(color = panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("PRIVACY & CONTROLS", color = cyan)
                        Text("No network permission is used. Capture ends when you press STOP or revoke screen sharing from Android.", color = Color.LightGray)
                        Text("PIRATE-AI does not send taps or automate other apps.", color = Color(0xFFFFC27A), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text("PIRATE-AI 0.2.0  •  LOCAL-FIRST", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}
