package com.siciario.labs.mediaplayer.data.database

import androidx.room.*
import com.siciario.labs.mediaplayer.data.model.Playlist
import com.siciario.labs.mediaplayer.data.model.PlaylistItem
import kotlinx.coroutines.flow.Flow

/**
 * Playlist DAO - Data Access Object for Playlists
 * Handles all database operations for playlists
 */
@Dao
interface PlaylistDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long
    
    @Update
    suspend fun updatePlaylist(playlist: Playlist)
    
    @Delete
    suspend fun deletePlaylist(playlist: Playlist)
    
    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): Playlist?
    
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(playlistItem: PlaylistItem)
    
    @Delete
    suspend fun deletePlaylistItem(playlistItem: PlaylistItem)
    
    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId")
    fun getPlaylistItemCount(playlistId: Long): Flow<Int>
    
    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND mediaItemId = :mediaItemId")
    suspend fun removeFromPlaylist(playlistId: Long, mediaItemId: Long)
    
    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: Long)
}
