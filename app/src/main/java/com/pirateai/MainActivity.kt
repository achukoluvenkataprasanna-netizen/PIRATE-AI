package com.pirateai

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PirateHome(
                onStartCapture = {
                    val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    captureLauncher.launch(manager.createScreenCaptureIntent())
                },
                onStopCapture = {
                    startService(Intent(this, ScreenCaptureService::class.java).apply {
                        action = ScreenCaptureService.ACTION_STOP
                    })
                }
            )
        }
    }
}

@Composable
private fun PirateHome(onStartCapture: () -> Unit, onStopCapture: () -> Unit) {
    var inputEnabled by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("SYSTEM READY") }
    val background = Color(0xFF080D16)
    val panel = Color(0xFF111C2B)
    val cyan = Color(0xFF39D9FF)

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("PIRATE-AI", color = cyan, fontSize = 30.sp)
                Text("ANDROID VISION SYSTEM  •  LOCAL MODE", color = Color.LightGray, fontSize = 11.sp)
                Surface(color = panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CORE STATUS", color = cyan)
                        Text(status, color = Color.White, fontSize = 20.sp)
                        Text("Processing stays on this device. Screen capture starts only after Android permission.", color = Color.LightGray)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        status = "WAITING FOR ANDROID PERMISSION"
                        onStartCapture()
                        status = "CAPTURE REQUEST SENT"
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087E9C))) {
                        Text("START VISION")
                    }
                    Button(onClick = {
                        onStopCapture()
                        status = "CAPTURE STOPPED"
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344256))) {
                        Text("STOP")
                    }
                }
                Surface(color = panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("INPUT CONTROL", color = cyan)
                        Text("Touch automation is reserved for your own apps and authorized test environments.", color = Color.LightGray)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { inputEnabled = !inputEnabled },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (inputEnabled) Color(0xFF9B5B22) else Color(0xFF344256)
                                )
                            ) { Text(if (inputEnabled) "INPUT ENABLED" else "INPUT DISABLED") }
                        }
                        if (inputEnabled) {
                            Text("Demo UI toggle only — no automatic taps are implemented in this starter.", color = Color(0xFFFFC27A), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Surface(color = panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("ON-DEVICE MODULES", color = cyan)
                        Text("• Screen capture: MediaProjection\n• Vision: Android bitmap processing (next module)\n• Network: not required\n• Model: none bundled in this starter", color = Color.LightGray)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("PIRATE-AI 0.1.0  •  LOCAL-FIRST", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}
