package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.data.database.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Extension function on Context to extract and cache video thumbnail from a VideoEntity using MediaMetadataRetriever.
 * Stores in a dedicated, app-specific directory "cached_video_thumbs" to avoid UI thread blockage
 * and prevent UI jank during scrolling list loading.
 */
suspend fun Context.getOrCreateVideoThumbnail(video: VideoEntity): File? = withContext(Dispatchers.IO) {
    val uriString = video.uriString
    if (uriString.isEmpty()) return@withContext null

    // Dedicated app-specific directory for video thumbnails inside files dir
    val thumbsDir = File(this@getOrCreateVideoThumbnail.filesDir, "cached_video_thumbs")
    if (!thumbsDir.exists()) {
        thumbsDir.mkdirs()
    }

    // High performance hashing for stable file caching
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(uriString.toByteArray())
    val hash = digest.joinToString("") { String.format("%02x", it) }
    val cacheFile = File(thumbsDir, "thumb_$hash.jpg")

    if (cacheFile.exists() && cacheFile.length() > 0) {
        return@withContext cacheFile
    }

    val retriever = MediaMetadataRetriever()
    try {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "content" || uri.scheme == "android.resource") {
            retriever.setDataSource(this@getOrCreateVideoThumbnail, uri)
        } else if (video.path != null) {
            retriever.setDataSource(video.path)
        } else {
            retriever.setDataSource(uriString, HashMap())
        }

        // Get a high-quality frame around 1 second mark
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toLongOrNull() ?: 0L
        val timeUs = if (durationMs > 2000L) 1000000L else 0L

        val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.frameAtTime

        if (bitmap != null) {
            // Apply scale-down algorithm for optimized scrolling performance
            val width = bitmap.width
            val height = bitmap.height
            val maxDim = 320
            val scaled = if (width > maxDim || height > maxDim) {
                val ratio = width.toFloat() / height.toFloat()
                val (newW, newH) = if (width > height) {
                    Pair(maxDim, (maxDim / ratio).toInt())
                } else {
                    Pair((maxDim * ratio).toInt(), maxDim)
                }
                Bitmap.createScaledBitmap(bitmap, newW, newH, true)
            } else {
                bitmap
            }

            FileOutputStream(cacheFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, fos)
            }

            if (scaled != bitmap) {
                scaled.recycle()
            }
            bitmap.recycle()
            Log.d("ThumbnailExtensions", "Generated high-performance thumbnail cache for: ${video.title} at ${cacheFile.absolutePath}")
            return@withContext cacheFile
        }
    } catch (e: Exception) {
        Log.e("ThumbnailExtensions", "Failed to retrieve thumbnail via MediaMetadataRetriever for ${video.title}: ${e.message}")
    } finally {
        try {
            retriever.release()
        } catch (e: Exception) {
            // Ignore release exceptions
        }
    }
    return@withContext null
}
