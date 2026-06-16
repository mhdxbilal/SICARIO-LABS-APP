package com.siciario.labs.mediaplayer.data.repository

import com.siciario.labs.mediaplayer.data.database.MediaDao
import com.siciario.labs.mediaplayer.data.database.PlaylistDao
import com.siciario.labs.mediaplayer.data.model.MediaItem
import com.siciario.labs.mediaplayer.data.model.Playlist
import com.siciario.labs.mediaplayer.data.model.PlaylistItem
import kotlinx.coroutines.flow.Flow

/**
 * Media Repository - Abstraction layer for data access
 * Handles all media-related database operations
 */
class MediaRepository(private val mediaDao: MediaDao) {
    
    fun getAllMedia(): Flow<List<MediaItem>> = mediaDao.getAllMedia()
    
    fun getFavoriteMedia(): Flow<List<MediaItem>> = mediaDao.getFavoriteMedia()
    
    fun getMostPlayedMedia(): Flow<List<MediaItem>> = mediaDao.getMostPlayedMedia()
    
    fun getRecentlyPlayedMedia(): Flow<List<MediaItem>> = mediaDao.getRecentlyPlayedMedia()
    
    fun searchMedia(query: String): Flow<List<MediaItem>> = mediaDao.searchMedia(query)
    
    suspend fun insertMedia(mediaItem: MediaItem) = mediaDao.insertMedia(mediaItem)
    
    suspend fun insertAllMedia(mediaItems: List<MediaItem>) = mediaDao.insertAllMedia(mediaItems)
    
    suspend fun updateMedia(mediaItem: MediaItem) = mediaDao.updateMedia(mediaItem)
    
    suspend fun deleteMedia(mediaItem: MediaItem) = mediaDao.deleteMedia(mediaItem)
    
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = mediaDao.toggleFavorite(id, isFavorite)
    
    suspend fun recordMediaPlay(id: Long, timestamp: Long) = mediaDao.recordMediaPlay(id, timestamp)
    
    fun getMediaCount(): Flow<Int> = mediaDao.getMediaCount()
}

/**
 * Playlist Repository - Abstraction layer for playlist data access
 * Handles all playlist-related database operations
 */
class PlaylistRepository(private val playlistDao: PlaylistDao) {
    
    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    
    suspend fun createPlaylist(name: String, description: String = ""): Long {
        val playlist = Playlist(
            name = name,
            description = description,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return playlistDao.insertPlaylist(playlist)
    }
    
    suspend fun updatePlaylist(playlist: Playlist) = playlistDao.updatePlaylist(playlist)
    
    suspend fun deletePlaylist(playlist: Playlist) = playlistDao.deletePlaylist(playlist)
    
    suspend fun addToPlaylist(playlistId: Long, mediaItemId: Long, position: Int) {
        playlistDao.insertPlaylistItem(PlaylistItem(playlistId, mediaItemId, position))
    }
    
    suspend fun removeFromPlaylist(playlistId: Long, mediaItemId: Long) {
        playlistDao.removeFromPlaylist(playlistId, mediaItemId)
    }
    
    fun getPlaylistItemCount(playlistId: Long): Flow<Int> = playlistDao.getPlaylistItemCount(playlistId)
}
