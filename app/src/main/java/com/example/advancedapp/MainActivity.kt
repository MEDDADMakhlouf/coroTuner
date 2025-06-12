package com.example.advancedapp

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.advancedapp.ui.theme.AdvancedAppTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val audioEngine = AudioEngine()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("MainActivity", "Permission granted. Starting AudioEngine.")

                audioEngine.start()
            } else {
                Log.w("MainActivity", "Permission denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdvancedAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AudioControlScreen(audioEngine, onPermissionRequest = {
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    })
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        audioEngine.stop()
    }
}

@Composable
fun AudioControlScreen(audioEngine: AudioEngine, onPermissionRequest: () -> Unit) {
    val isRecording by audioEngine.isRecording

    var volume by remember { mutableStateOf(1.0f) }
    var echoDelay by remember { mutableStateOf(0.5f) }
    var echoDecay by remember { mutableStateOf(0.4f) }

    LaunchedEffect(volume, echoDelay, echoDecay) {
        audioEngine.volumeMultiplier = volume
        audioEngine.echoDelayInSeconds = echoDelay
        audioEngine.echoDecay = echoDecay
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Voice Morpher", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(bottom = 32.dp))

        Button(
            onClick = {
                if (isRecording) {
                    audioEngine.stop()
                } else {
                    onPermissionRequest()
                }
            },
            modifier = Modifier.size(150.dp)
        ) {
            Text(text = if (isRecording) "Stop" else "Start", style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("Volume: ${(volume * 100).roundToInt()}%", style = MaterialTheme.typography.bodyLarge)
                    Slider(value = volume, onValueChange = { volume = it }, valueRange = 0f..2f)
                }

                Column {
                    Text("Echo Delay: ${String.format("%.2f", echoDelay)}s", style = MaterialTheme.typography.bodyLarge)
                    Slider(value = echoDelay, onValueChange = { echoDelay = it }, valueRange = 0.01f..1.0f)
                }

                Column {
                    Text("Echo Decay: ${String.format("%.2f", echoDecay)}", style = MaterialTheme.typography.bodyLarge)
                    Slider(value = echoDecay, onValueChange = { echoDecay = it }, valueRange = 0f..0.7f)
                }
            }
        }
    }
}