package com.example.data.scanner

import android.content.Context
import android.os.Environment
import com.example.data.database.VideoEntity
import com.example.data.settings.PlayerSettings

class RecursiveDirectoryScanner(val context: Context) {

    fun scanVideosFromLocal(): List<VideoEntity> {
        val rootPath = Environment.getExternalStorageDirectory()
        val videoExtensions = listOf("mp4", "mkv", "avi")
        val result = mutableListOf<VideoEntity>()
        val excludedDirs = PlayerSettings.getExcludedDirectories(context)

        try {
             rootPath.walkTopDown()
                 .onEnter { dir -> 
                     !dir.name.startsWith(".") && dir.name != "Android" && !excludedDirs.contains(dir.name)
                 }
                 .filter { it.isFile && it.extension.lowercase() in videoExtensions }
                 .forEach { file ->
                     result.add(
                         VideoEntity(
                             uriString = file.absolutePath, 
                             title = file.name,
                             path = file.absolutePath,
                             duration = 0L,
                             size = file.length(),
                             addedDate = file.lastModified(),
                             folderName = file.parentFile?.name ?: "Deep Scan"
                         )
                     )
                 }
        } catch(e: Exception) {
            e.printStackTrace()
        }

        return result
    }
}
