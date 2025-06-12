


import android.Manifest
import android.os.Bundle
import android.util.Log // Import Log for debugging
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.advancedapp.ui.theme.AdvancedAppTheme // Ensure this matches your project's theme name

class MainActivity : ComponentActivity() {

    private val audioEngine = AudioEngine()

    // Create a launcher to request the RECORD_AUDIO permission
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Now, and only now, start the audio engine.
                Log.d("MainActivity", "RECORD_AUDIO permission granted. Starting AudioEngine.")
                audioEngine.start(this@MainActivity)
            } else {
                // Inform the user that the permission is required.
                // You might want to show a SnackBar or Toast message here.
                Log.w("MainActivity", "RECORD_AUDIO permission denied by user.")
                // Ensure UI reflects that recording is NOT active
                // The AudioEngine's _isRecording state will already be false,
                // and the Composable observing it will update.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdvancedAppTheme { // Make sure this matches your project's theme name
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AudioControlButton(audioEngine) {
                        // This lambda is called when the button is clicked to START recording.
                        // It initiates the permission request.
                        Log.d("MainActivity", "Button clicked to start. Requesting permission.")
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Ensure the audio engine is stopped when the app is no longer visible
        Log.d("MainActivity", "onStop called. Stopping AudioEngine.")
        audioEngine.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Good place to ensure any lingering resources are released if onStop wasn't enough.
        // For this simple case, onStop is usually sufficient as audioJob is managed by its internal loop.
        Log.d("MainActivity", "onDestroy called.")
    }
}

@Composable
fun AudioControlButton(audioEngine: AudioEngine, onPermissionRequest: () -> Unit) {
    // Observe the state directly from the AudioEngine's exposed State
    val isRecording by audioEngine.isRecording // This will recompose when audioEngine's _isRecording.value changes

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                if (isRecording) {
                    Log.d("AudioControlButton", "Button clicked: Stop recording.")
                    audioEngine.stop()
                } else {
                    Log.d("AudioControlButton", "Button clicked: Start recording. Triggering permission request.")
                    // Only request permission if we are trying to start
                    onPermissionRequest()
                }
                // Removed the manual 'isRecording = !isRecording' here, as it's now observed from AudioEngine
            },
            modifier = Modifier.size(150.dp)
        ) {
            Text(text = if (isRecording) "Stop" else "Start", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
