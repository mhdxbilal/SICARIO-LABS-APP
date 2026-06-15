package com.example.ai

import android.media.audiofx.Equalizer

/**
 * AI Feature C (Dialogue Voice Booster): A DSP filter (Kotlin implementation wrapper)
 * isolating human voice frequencies for crystal clear dialogue over background noise.
 */
class DialogueVoiceBooster {

    private var dspEqualizer: Equalizer? = null

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId != 0) {
            try {
                dspEqualizer = Equalizer(0, audioSessionId).apply {
                    enabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun enableClearVoice(enabled: Boolean) {
        val eq = dspEqualizer ?: return
        if (enabled) {
            val numBands = eq.numberOfBands
            for (i in 0 until numBands) {
                val centerFreq = eq.getCenterFreq(i.toShort())
                // Boost human voice range (approx 300Hz to 3kHz)
                if (centerFreq in 300000..3000000) {
                    eq.setBandLevel(i.toShort(), 1500) // Boost by +15dB
                } else {
                    eq.setBandLevel(i.toShort(), -1000) // Reduce other ranges
                }
            }
        } else {
            val numBands = eq.numberOfBands
            for (i in 0 until numBands) {
                eq.setBandLevel(i.toShort(), 0)
            }
        }
    }

    fun release() {
        dspEqualizer?.release()
        dspEqualizer = null
    }
}
