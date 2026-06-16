package com.siciario.labs.mediaplayer.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Media Playback Service - Handles audio/video playback
 * Foreground service for uninterrupted offline playback
 */
class MediaPlaybackService : Service() {
    
    private var player: ExoPlayer? = null
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }
    
    override fun onCreate() {
        super.onCreate()
        initializePlayer()
    }
    
    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }
    
    override fun onDestroy() {
        player?.release()
        super.onDestroy()
    }
    
    fun getPlayer(): ExoPlayer? = player
}
