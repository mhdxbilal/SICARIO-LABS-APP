package com.example.ai

import android.content.Context
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

/**
 * AI Feature B (Whisper Local Captions): Custom Media3 AudioProcessor intercepting
 * live PCM buffer and forwarding to quantized Whisper tiny via TFLite.
 */
class WhisperCaptionEngine(private val context: Context) : AudioProcessor {

    val liveCaptionsFlow = MutableStateFlow("")
    private val inferenceScope = CoroutineScope(Dispatchers.Default)

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        // We capture 16-bit PCM for Whisper inference
        return inputAudioFormat
    }

    override fun isActive(): Boolean = true

    override fun queueInput(inputBuffer: ByteBuffer) {
        // Dispatch the PCM buffer to background ML thread asynchronously so audio isn't blocked
        val pcmSize = inputBuffer.remaining()
        if (pcmSize > 0) {
            val pcmData = ByteArray(pcmSize)
            inputBuffer.get(pcmData)
            
            inferenceScope.launch {
                runWhisperInference(pcmData)
            }
        }
    }

    override fun queueEndOfStream() {}
    override fun getOutput(): ByteBuffer = AudioProcessor.EMPTY_BUFFER
    override fun isEnded(): Boolean = false
    override fun flush() {}
    override fun reset() {}

    private fun runWhisperInference(pcmData: ByteArray) {
        // Simulated execution of:
        // interpreter.runForMultipleInputsOutputs(arrayOf(pcmData), outputMap)
        // liveCaptionsFlow.value = extractedString
    }
}
