package com.siciario.labs.mediaplayer.util

import com.squareup.moshi.JsonClass

/**
 * Playback State - Represents the current playback state
 * Stored locally
 */
@JsonClass(generateAdapter = true)
data class PlaybackState(
    val currentMediaId: Long? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val repeatMode: Int = 0, // 0: no repeat, 1: repeat all, 2: repeat one
    val isShuffleEnabled: Boolean = false,
    val playlistId: Long? = null
)

/**
 * Playback Controller - Manages playback operations
 */
class PlaybackController {
    
    private var currentState = PlaybackState()
    
    fun updatePlaybackState(state: PlaybackState) {
        currentState = state
    }
    
    fun getPlaybackState(): PlaybackState = currentState
    
    fun setRepeatMode(mode: Int) {
        currentState = currentState.copy(repeatMode = mode)
    }
    
    fun toggleShuffle() {
        currentState = currentState.copy(isShuffleEnabled = !currentState.isShuffleEnabled)
    }
    
    fun setCurrentMedia(mediaId: Long) {
        currentState = currentState.copy(currentMediaId = mediaId, position = 0)
    }
    
    fun updatePosition(position: Long) {
        currentState = currentState.copy(position = position)
    }
    
    fun setIsPlaying(isPlaying: Boolean) {
        currentState = currentState.copy(isPlaying = isPlaying)
    }
}
