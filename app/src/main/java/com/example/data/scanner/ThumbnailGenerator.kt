package com.example.data.scanner

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

object ThumbnailGenerator {
    private const val TAG = "ThumbnailGenerator"
    private const val THUMBNAILS_DIR = "video_thumbnails"

    /**
     * Generates a thumbnail for a single video entity and returns the cache file path,
     * or null if generation fails or is not applicable.
     */
    suspend fun generateThumbnail(context: Context, video: VideoEntity): String? = withContext(Dispatchers.IO) {
        val uriString = video.uriString
        if (uriString.isEmpty()) return@withContext null

        val cacheDir = File(context.cacheDir, THUMBNAILS_DIR)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        // Generate a stable unique filename based on the URI string
        val safeFileName = md5(uriString) + ".jpg"
        val outputFile = File(cacheDir, safeFileName)

        // If the thumbnail already exists, return it immediately to avoid re-generating
        if (outputFile.exists() && outputFile.length() > 0) {
            return@withContext outputFile.absolutePath
        }

        val retriever = MediaMetadataRetriever()
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "content" || uri.scheme == "android.resource") {
                retriever.setDataSource(context, uri)
            } else if (video.path != null) {
                retriever.setDataSource(video.path)
            } else {
                retriever.setDataSource(uriString, HashMap())
            }

            // Extract a frame at 1 second mark (or 0 if video is short)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val targetTimeUs = if (durationMs > 2000L) 1000000L else 0L

            val bitmap = retriever.getFrameAtTime(targetTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime

            if (bitmap != null) {
                // Scale down bitmap to a reasonable size (e.g., width 320px) to conserve memory & disk
                val scaledBitmap = scaleBitmap(bitmap, 320)
                FileOutputStream(outputFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
                }
                if (scaledBitmap != bitmap) {
                    scaledBitmap.recycle()
                }
                bitmap.recycle()
                
                return@withContext outputFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail for ${video.title}: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
        return@withContext null
    }

    private fun scaleBitmap(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) return source

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { String.format("%02x", it) }
    }
}
