package com.siciario.labs.mediaplayer.feature

import com.siciario.labs.mediaplayer.data.model.MediaItem
import kotlin.random.Random

/**
 * Shuffle Manager - Handles shuffle and queue management
 * Offline-only playback operations
 */
class ShuffleManager {
    
    private var playbackQueue: List<MediaItem> = emptyList()
    private var shuffledQueue: List<MediaItem> = emptyList()
    private var currentIndex: Int = 0
    
    fun setQueue(mediaItems: List<MediaItem>) {
        playbackQueue = mediaItems
        currentIndex = 0
    }
    
    fun enableShuffle() {
        shuffledQueue = playbackQueue.shuffled(Random(System.currentTimeMillis()))
        currentIndex = 0
    }
    
    fun disableShuffle() {
        shuffledQueue = emptyList()
        currentIndex = 0
    }
    
    fun getCurrentQueue(): List<MediaItem> {
        return if (shuffledQueue.isNotEmpty()) shuffledQueue else playbackQueue
    }
    
    fun getCurrentItem(): MediaItem? {
        val queue = getCurrentQueue()
        return if (currentIndex < queue.size) queue[currentIndex] else null
    }
    
    fun getNextItem(): MediaItem? {
        val queue = getCurrentQueue()
        val nextIndex = currentIndex + 1
        return if (nextIndex < queue.size) queue[nextIndex] else null
    }
    
    fun getPreviousItem(): MediaItem? {
        return if (currentIndex > 0) {
            val queue = getCurrentQueue()
            queue[currentIndex - 1]
        } else {
            null
        }
    }
    
    fun next() {
        val queue = getCurrentQueue()
        if (currentIndex < queue.size - 1) {
            currentIndex++
        }
    }
    
    fun previous() {
        if (currentIndex > 0) {
            currentIndex--
        }
    }
    
    fun seekTo(index: Int) {
        val queue = getCurrentQueue()
        if (index in queue.indices) {
            currentIndex = index
        }
    }
    
    fun getCurrentIndex(): Int = currentIndex
    
    fun getQueueSize(): Int = getCurrentQueue().size
}
