package com.siciario.labs.mediaplayer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Media Item - Represents a single media file (audio or video)
 * Stored locally in Room database
 */
@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey
    val id: Long,
    val filePath: String,
    val fileName: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val genre: String?,
    val duration: Long,
    val fileSize: Long,
    val mimeType: String,
    val dateAdded: Long,
    val dateModified: Long,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedTime: Long = 0
)

/**
 * Playlist - Local playlist container
 */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val coverUri: String? = null
)

/**
 * PlaylistItem - Junction entity for Playlist and MediaItem
 */
@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "mediaItemId"]
)
data class PlaylistItem(
    val playlistId: Long,
    val mediaItemId: Long,
    val position: Int
)
