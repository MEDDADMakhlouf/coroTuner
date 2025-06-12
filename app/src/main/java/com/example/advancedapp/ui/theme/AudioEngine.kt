import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log // Import Log for debugging
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch





class AudioEngine {

    // A background job to handle the audio processing
    private var audioJob: Job? = null

    // --- Audio Configuration ---
    // NOTE: These must be the same for the AudioRecord and AudioTrack
    private val sampleRate = 44100
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    // --- State Management ---
    // Expose isRecording as an observable State for Compose UI
    private val _isRecording = mutableStateOf(false)
    val isRecording: State<Boolean> = _isRecording

    fun start(context: Context) {
        if (_isRecording.value) {
            Log.d("AudioEngine", "AudioEngine already running, skipping start.")
            return // Don't start if already running
        }

        // It's good practice to re-check permission here too, though MainActivity should handle it
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w("AudioEngine", "RECORD_AUDIO permission not granted when trying to start AudioEngine.")
            // Do not proceed if permission is not granted. MainActivity should handle the request.
            return
        }

        // Launch a new coroutine on the IO dispatcher for long-running blocking I/O operations
        audioJob = CoroutineScope(Dispatchers.IO).launch {
            _isRecording.value = true // Update state to true as soon as we begin the job
            Log.d("AudioEngine", "AudioEngine job started, isRecording set to true.")

            var audioRecord: AudioRecord? = null
            var audioTrack: AudioTrack? = null

            try {
                // Get the minimum buffer size required for the audio configuration
                val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
                if (minBufferSize == AudioRecord.ERROR_BAD_VALUE || minBufferSize == AudioRecord.ERROR) {
                    Log.e("AudioEngine", "Invalid AudioRecord buffer size: $minBufferSize")
                    return@launch // Exit coroutine if buffer size is invalid
                }

                // Create the AudioRecord instance to capture audio from the microphone
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfigIn,
                    audioFormat,
                    minBufferSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("AudioEngine", "AudioRecord initialization failed. State: ${audioRecord.state}")
                    return@launch // Exit coroutine if AudioRecord failed to initialize
                }

                // Create the AudioTrack instance to play back audio
                audioTrack = AudioTrack.Builder()
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfigOut)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
                    Log.e("AudioEngine", "AudioTrack initialization failed. State: ${audioTrack.state}")
                    return@launch // Exit coroutine if AudioTrack failed to initialize
                }

                // Start recording and playing
                audioRecord.startRecording()
                audioTrack.play()
                Log.d("AudioEngine", "AudioRecord and AudioTrack started.")

                // Create a buffer to hold the audio data
                val buffer = ShortArray(minBufferSize / 2) // Using ShortArray for 16-bit PCM

                // Main audio loop - continue as long as `_isRecording.value` is true and coroutine is active
                while (_isRecording.value && isActive) { // Check isActive to respect coroutine cancellation
                    // Read audio data from the microphone into the buffer
                    val readSize = audioRecord.read(buffer, 0, buffer.size)

                    if (readSize > 0) {
                        // --- THIS IS WHERE WE WILL ADD EFFECTS LATER ---
                        // For now, we just write the buffer directly to the speaker
                        audioTrack.write(buffer, 0, readSize)
                    } else if (readSize == AudioRecord.ERROR_INVALID_OPERATION) {
                        Log.e("AudioEngine", "AudioRecord.read: Invalid operation.")
                        break // Exit loop on critical error
                    } else if (readSize == AudioRecord.ERROR_BAD_VALUE) {
                        Log.e("AudioEngine", "AudioRecord.read: Bad value.")
                        break // Exit loop on critical error
                    }
                }
                Log.d("AudioEngine", "AudioEngine loop stopped.")

            } catch (e: SecurityException) {
                Log.e("AudioEngine", "SecurityException: RECORD_AUDIO permission denied. ${e.message}")
            } catch (e: IllegalStateException) {
                Log.e("AudioEngine", "IllegalStateException during audio processing: ${e.message}")
            } catch (e: Exception) {
                Log.e("AudioEngine", "An unexpected error occurred: ${e.message}", e)
            } finally {
                // Clean up resources regardless of how the loop/coroutine exited
                try {
                    audioRecord?.apply {
                        if (recordingState == AudioRecord.RECORDSTATE_RECORDING) stop()
                        release()
                    }
                    audioTrack?.apply {
                        if (playState == AudioTrack.PLAYSTATE_PLAYING) stop()
                        release()
                    }
                    Log.d("AudioEngine", "AudioRecord and AudioTrack resources released.")
                } catch (e: Exception) {
                    Log.e("AudioEngine", "Error releasing audio resources: ${e.message}")
                }
                _isRecording.value = false // Ensure state is false after cleanup
                Log.d("AudioEngine", "AudioEngine job finished, isRecording set to false.")
            }
        }
    }

    fun stop() {
        // To stop the loop, we just set our flag to false. The coroutine will see this and exit.
        if (_isRecording.value) {
            _isRecording.value = false // Signal the coroutine to stop
            Log.d("AudioEngine", "Stop signal sent to AudioEngine.")
        }
        // No need to cancel audioJob explicitly here, as the loop condition handles it.
        // If you needed immediate cancellation and resource release, you would use audioJob?.cancel()
    }
}