package com.example.advancedapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class AudioEngine {
    var volumeMultiplier: Float by mutableStateOf(1.0f)
    var echoDelayInSeconds: Float by mutableStateOf(0.5f)
    var echoDecay: Float by mutableStateOf(0.4f)

    private val _isRecording = mutableStateOf(false)

    val isRecording: State<Boolean> = _isRecording

    private var audioJob: Job? = null
    private val sampleRate = 44100
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT


    private lateinit var delayBuffer: ShortArray
    private var delayBufferIndex: Int = 0


    fun start( ) {
        if (_isRecording.value) {
            Log.d("AudioEngine", "Already recording, skipping start.")
            return
        }

        audioJob = CoroutineScope(Dispatchers.IO).launch {
            _isRecording.value = true
            Log.d("AudioEngine", "Audio job started.")

            var audioRecord: AudioRecord? = null
            var audioTrack: AudioTrack? = null
            var aec: AcousticEchoCanceler? = null

            try {
                val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)

                val maxDelayInSamples = (sampleRate * 2.0).roundToInt()
                delayBuffer = ShortArray(maxDelayInSamples)
                delayBufferIndex = 0

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfigIn,
                    audioFormat,
                    minBufferSize
                )

                if (AcousticEchoCanceler.isAvailable()) {
                    aec = AcousticEchoCanceler.create(audioRecord.audioSessionId)
                    aec?.enabled = true
                    Log.d("AudioEngine", "AcousticEchoCanceler enabled.")
                }

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

                audioRecord.startRecording()
                audioTrack.play()

                val buffer = ShortArray(minBufferSize / 2)

                while (_isRecording.value && isActive) {
                    val readSize = audioRecord.read(buffer, 0, buffer.size)
                    if (readSize > 0) {

                        val currentEchoDelayInSamples = (sampleRate * echoDelayInSeconds).roundToInt()
                        val currentVolumeMultiplier = volumeMultiplier
                        val currentEchoDecay = echoDecay

                        for (i in 0 until readSize) {
                            val pastSampleIndex = (delayBufferIndex - currentEchoDelayInSamples + delayBuffer.size) % delayBuffer.size
                            val delayedSample = delayBuffer[pastSampleIndex]
                            val currentSample = buffer[i]
                            val mixedSample = (currentSample + (delayedSample * currentEchoDecay)).toInt()
                            delayBuffer[delayBufferIndex] = mixedSample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                            val finalSample = (mixedSample * currentVolumeMultiplier)
                                .coerceIn(Short.MIN_VALUE.toInt().toFloat(),
                                    Short.MAX_VALUE.toInt().toFloat()
                                )
                                .toInt().toShort()
                            buffer[i] = finalSample
                            delayBufferIndex = (delayBufferIndex + 1) % delayBuffer.size
                        }
                        audioTrack.write(buffer, 0, readSize)
                    }
                }

            } catch (e: Exception) {
                Log.e("AudioEngine", "An error occurred: ${e.message}", e)
            } finally {
                Log.d("AudioEngine", "Stopping and releasing resources.")
                audioRecord?.stop(); audioRecord?.release()
                audioTrack?.stop(); audioTrack?.release()
                aec?.release()
                _isRecording.value = false
            }
        }
    }

    fun stop() {
        if (_isRecording.value) {
            Log.d("AudioEngine", "Stop signal received.")
            _isRecording.value = false
        }
    }
}
