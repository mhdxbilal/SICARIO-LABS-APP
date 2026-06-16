package com.siciario.labs.mediaplayer.feature

/**
 * Equalizer Settings - Audio EQ configuration (offline)
 * Stored locally in database
 */
data class EqualizerSettings(
    val enabled: Boolean = false,
    val preset: EqualizerPreset = EqualizerPreset.FLAT,
    val bass: Float = 0f,
    val mid: Float = 0f,
    val treble: Float = 0f
) {
    enum class EqualizerPreset {
        FLAT, BASS_BOOST, TREBLE_BOOST, ACOUSTIC, POP, CLASSICAL, JAZZ
    }
}

/**
 * Audio Settings Manager - Manages offline audio settings
 */
class AudioSettingsManager {
    
    private var eqSettings = EqualizerSettings()
    private var volume = 1.0f
    private var bassBoost = 0
    
    fun setEqualizerSettings(settings: EqualizerSettings) {
        eqSettings = settings
    }
    
    fun getEqualizerSettings(): EqualizerSettings = eqSettings
    
    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
    }
    
    fun getVolume(): Float = volume
    
    fun setBassBoost(boost: Int) {
        bassBoost = boost.coerceIn(0, 10)
    }
    
    fun getBassBoost(): Int = bassBoost
    
    fun applyPreset(preset: EqualizerSettings.EqualizerPreset) {
        eqSettings = when (preset) {
            EqualizerSettings.EqualizerPreset.FLAT -> EqualizerSettings(preset = preset)
            EqualizerSettings.EqualizerPreset.BASS_BOOST -> EqualizerSettings(
                preset = preset,
                bass = 5f,
                mid = 0f,
                treble = -2f
            )
            EqualizerSettings.EqualizerPreset.TREBLE_BOOST -> EqualizerSettings(
                preset = preset,
                bass = -2f,
                mid = 0f,
                treble = 5f
            )
            EqualizerSettings.EqualizerPreset.ACOUSTIC -> EqualizerSettings(
                preset = preset,
                bass = 3f,
                mid = 2f,
                treble = 2f
            )
            EqualizerSettings.EqualizerPreset.POP -> EqualizerSettings(
                preset = preset,
                bass = 2f,
                mid = 1f,
                treble = 3f
            )
            EqualizerSettings.EqualizerPreset.CLASSICAL -> EqualizerSettings(
                preset = preset,
                bass = 2f,
                mid = -1f,
                treble = 2f
            )
            EqualizerSettings.EqualizerPreset.JAZZ -> EqualizerSettings(
                preset = preset,
                bass = 1f,
                mid = 2f,
                treble = 1f
            )
        }
    }
}
