package com.siciario.labs.mediaplayer.util

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Storage Utils - Utilities for device storage operations
 * Offline-first approach - local storage only
 */
object StorageUtils {
    
    private const val CACHE_DIR_NAME = "sicario_cache"
    private const val THUMBNAIL_DIR = "thumbnails"
    private const val METADATA_DIR = "metadata"
    
    fun getCacheDir(context: Context): File {
        return File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }
    
    fun getThumbnailCacheDir(context: Context): File {
        return File(getCacheDir(context), THUMBNAIL_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    fun getMetadataCacheDir(context: Context): File {
        return File(getCacheDir(context), METADATA_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    fun getAvailableStorageSpace(): Long {
        return try {
            val stats = android.os.StatFs(Environment.getExternalStorageDirectory().absolutePath)
            stats.availableBlocksLong * stats.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }
    
    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> "${bytes / 1_000_000_000} GB"
            bytes >= 1_000_000 -> "${bytes / 1_000_000} MB"
            bytes >= 1_000 -> "${bytes / 1_000} KB"
            else -> "$bytes B"
        }
    }
    
    fun formatDuration(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        val hours = (milliseconds / (1000 * 60 * 60))
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
}
