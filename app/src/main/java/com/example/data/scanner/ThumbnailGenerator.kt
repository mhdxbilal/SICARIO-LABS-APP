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

import com.example.util.getOrCreateVideoThumbnail

object ThumbnailGenerator {
    private const val TAG = "ThumbnailGenerator"

    /**
     * Generates a thumbnail for a single video entity and returns the cache file path,
     * or null if generation fails or is not applicable.
     */
    suspend fun generateThumbnail(context: Context, video: VideoEntity): String? {
        val file = context.getOrCreateVideoThumbnail(video)
        return file?.absolutePath
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
