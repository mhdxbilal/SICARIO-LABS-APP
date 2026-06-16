package com.siciario.labs.mediaplayer.util

import android.content.Context
import android.os.FileObserver
import kotlinx.coroutines.*

/**
 * Media Scanner - Scans device storage for media files
 * Offline-only operation - no internet calls
 */
class MediaScanner(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val supportedAudioFormats = setOf(
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma"
    )
    private val supportedVideoFormats = setOf(
        "mp4", "mkv", "webm", "avi", "3gp", "flv"
    )
    
    fun scanMediaFiles(onProgress: (Int) -> Unit = {}, onComplete: (List<String>) -> Unit = {}) {
        scope.launch {
            val mediaFiles = mutableListOf<String>()
            try {
                val dirs = arrayOf(
                    "/storage/emulated/0/Music",
                    "/storage/emulated/0/Movies",
                    "/storage/emulated/0/DCIM",
                    "/storage/emulated/0/Download"
                )
                
                for (dirPath in dirs) {
                    val dir = java.io.File(dirPath)
                    if (dir.exists() && dir.isDirectory) {
                        scanDirectory(dir, mediaFiles) { progress ->
                            onProgress(progress)
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    onComplete(mediaFiles)
                }
            } catch (e: Exception) {
                android.util.Log.e("MediaScanner", "Error scanning media", e)
            }
        }
    }
    
    private fun scanDirectory(dir: java.io.File, results: MutableList<String>, onProgress: (Int) -> Unit) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            when {
                file.isDirectory -> scanDirectory(file, results, onProgress)
                isMediaFile(file.name) -> {
                    results.add(file.absolutePath)
                    onProgress(results.size)
                }
            }
        }
    }
    
    private fun isMediaFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in supportedAudioFormats || extension in supportedVideoFormats
    }
    
    fun cancel() {
        scope.cancel()
    }
}
