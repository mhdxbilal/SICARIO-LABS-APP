package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.data.database.AudioEntity

class MediaStoreAudioScanner(private val context: Context) {

    fun scanAudioOnDevice(): List<AudioEntity> {
        val audioList = mutableListOf<AudioEntity>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.DURATION} > 1000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        val excludedDirs = com.example.data.settings.PlayerSettings.getExcludedDirectories(context)

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Track_$id"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val path = cursor.getString(dataColumn)
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)

                    val folderNameValue = path?.substringBeforeLast("/")?.substringAfterLast("/") ?: "Internal Storage"
                    if (excludedDirs.contains(folderNameValue)) continue

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val extension = path?.substringAfterLast('.', "mp3")?.lowercase() ?: "mp3"
                    
                    // Mark FLAC/WAV as lossless, other files with standard formats
                    val sampleRate = if (extension in listOf("flac", "wav")) 48000 else 44100
                    val bitDepth = if (extension in listOf("flac", "wav")) 24 else 16

                    audioList.add(
                        AudioEntity(
                            uriString = contentUri.toString(),
                            title = title,
                            artist = artist,
                            album = album,
                            path = path,
                            duration = duration,
                            size = size,
                            addedDate = dateAdded * 1000L,
                            format = extension,
                            sampleRate = sampleRate,
                            bitDepth = bitDepth
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return audioList
    }
}
