package com.siciario.labs.mediaplayer.data.database

import androidx.room.*
import com.siciario.labs.mediaplayer.data.model.MediaItem
import kotlinx.coroutines.flow.Flow

/**
 * Media DAO - Data Access Object for MediaItem
 * Handles all database operations for media items
 */
@Dao
interface MediaDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(mediaItem: MediaItem)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMedia(mediaItems: List<MediaItem>)
    
    @Update
    suspend fun updateMedia(mediaItem: MediaItem)
    
    @Delete
    suspend fun deleteMedia(mediaItem: MediaItem)
    
    @Query("DELETE FROM media_items WHERE filePath = :filePath")
    suspend fun deleteByFilePath(filePath: String)
    
    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaById(id: Long): MediaItem?
    
    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    fun getAllMedia(): Flow<List<MediaItem>>
    
    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoriteMedia(): Flow<List<MediaItem>>
    
    @Query("SELECT * FROM media_items ORDER BY playCount DESC LIMIT 10")
    fun getMostPlayedMedia(): Flow<List<MediaItem>>
    
    @Query("SELECT * FROM media_items WHERE lastPlayedTime > 0 ORDER BY lastPlayedTime DESC")
    fun getRecentlyPlayedMedia(): Flow<List<MediaItem>>
    
    @Query("SELECT * FROM media_items WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    fun searchMedia(query: String): Flow<List<MediaItem>>
    
    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    
    @Query("UPDATE media_items SET playCount = playCount + 1, lastPlayedTime = :timestamp WHERE id = :id")
    suspend fun recordMediaPlay(id: Long, timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM media_items")
    fun getMediaCount(): Flow<Int>
}
