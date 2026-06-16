package com.siciario.labs.mediaplayer.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.siciario.labs.mediaplayer.data.model.MediaItem
import com.siciario.labs.mediaplayer.data.model.Playlist
import com.siciario.labs.mediaplayer.data.model.PlaylistItem

/**
 * Sicario Media Player Database
 * Offline-first local storage using Room ORM
 */
@Database(
    entities = [MediaItem::class, Playlist::class, PlaylistItem::class],
    version = 1,
    exportSchema = false
)
abstract class SicarioMediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun playlistDao(): PlaylistDao
    
    companion object {
        private const val DATABASE_NAME = "sicario_media_player.db"
        
        @Volatile
        private var instance: SicarioMediaDatabase? = null
        
        fun getInstance(context: Context): SicarioMediaDatabase {
            return instance ?: synchronized(this) {
                instance ?: createDatabase(context).also { instance = it }
            }
        }
        
        private fun createDatabase(context: Context): SicarioMediaDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SicarioMediaDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
