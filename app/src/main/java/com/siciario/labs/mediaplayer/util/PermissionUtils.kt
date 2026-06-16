package com.siciario.labs.mediaplayer.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Permission Utils - Handle permission checks and requests
 */
object PermissionUtils {
    
    const val READ_EXTERNAL_STORAGE = android.Manifest.permission.READ_EXTERNAL_STORAGE
    const val READ_MEDIA_AUDIO = android.Manifest.permission.READ_MEDIA_AUDIO
    const val READ_MEDIA_VIDEO = android.Manifest.permission.READ_MEDIA_VIDEO
    const val READ_MEDIA_IMAGES = android.Manifest.permission.READ_MEDIA_IMAGES
    
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasRequiredPermissions(context: Context): Boolean {
        val requiredPermissions = listOf(
            READ_MEDIA_AUDIO,
            READ_MEDIA_VIDEO,
            READ_MEDIA_IMAGES
        )
        
        return requiredPermissions.all { permission ->
            hasPermission(context, permission)
        }
    }
}
