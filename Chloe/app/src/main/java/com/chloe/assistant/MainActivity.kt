package com.chloe.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChloeHome() }
    }
}

@Composable
private fun ChloeHome() {
    val dark = Color(0xFF08111B)
    val mint = Color(0xFF9BE7C4)
    MaterialTheme(colorScheme = darkColorScheme(
        background = dark, surface = Color(0xFF132331), primary = mint
    )) {
        Column(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF142C38), dark))
            ).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("CHLOE", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = mint)
            Text("Your personal AI companion", color = Color.LightGray)
            HumanChloePlaceholder()
            Text("Hello. I'm Chloe.", fontSize = 23.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(
                "Human-avatar, voice, screen-reading and accessibility features are being integrated. " +
                    "Phone actions will require the relevant Android permission and your confirmation.",
                color = Color(0xFFB9C8D2)
            )
            Button(onClick = { }) { Text("Voice assistant — setup pending") }
            OutlinedButton(onClick = { }) { Text("Enable screen access — setup pending") }
            Text("PIRATE-AI remains a separate app.", color = mint, fontSize = 12.sp)
        }
    }
}

/** Temporary Compose illustration; replace with a licensed 3D GLB avatar + animation runtime. */
@Composable
private fun HumanChloePlaceholder() {
    Box(
        Modifier.fillMaxWidth().height(300.dp)
            .background(Color(0x3329C99A), RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(92.dp).background(Color(0xFFE8B99B), CircleShape))
            Spacer(Modifier.height(7.dp))
            Box(Modifier.width(126.dp).height(112.dp)
                .background(Color(0xFF1B6D5A), RoundedCornerShape(38.dp, 38.dp, 18.dp, 18.dp)))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(Modifier.width(28.dp).height(80.dp).background(Color(0xFFE8B99B), RoundedCornerShape(16.dp)))
                Box(Modifier.width(28.dp).height(80.dp).background(Color(0xFFE8B99B), RoundedCornerShape(16.dp)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(Modifier.width(34.dp).height(55.dp).background(Color(0xFF243A50), RoundedCornerShape(12.dp)))
                Box(Modifier.width(34.dp).height(55.dp).background(Color(0xFF243A50), RoundedCornerShape(12.dp)))
            }
        }
    }
}
