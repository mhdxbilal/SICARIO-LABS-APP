package com.siciario.labs.mediaplayer.util

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.siciario.labs.mediaplayer.data.database.SicarioMediaDatabase
import com.siciario.labs.mediaplayer.data.repository.MediaRepository
import com.siciario.labs.mediaplayer.data.repository.PlaylistRepository
import com.siciario.labs.mediaplayer.ui.viewmodel.MediaViewModel
import com.siciario.labs.mediaplayer.ui.viewmodel.PlaylistViewModel

/**
 * Dependency Injection Factory - Creates and provides dependencies
 * Offline-first dependency graph
 */
class DIFactory {
    
    companion object {
        private var mediaDatabase: SicarioMediaDatabase? = null
        private var mediaRepository: MediaRepository? = null
        private var playlistRepository: PlaylistRepository? = null
        private var mediaViewModel: MediaViewModel? = null
        private var playlistViewModel: PlaylistViewModel? = null
        
        fun provideMediaDatabase(databaseInstance: SicarioMediaDatabase): SicarioMediaDatabase {
            return mediaDatabase ?: databaseInstance.also { mediaDatabase = it }
        }
        
        fun provideMediaRepository(context: android.content.Context): MediaRepository {
            return mediaRepository ?: MediaRepository(
                provideMediaDatabase(
                    SicarioMediaDatabase.getInstance(context)
                ).mediaDao()
            ).also { mediaRepository = it }
        }
        
        fun providePlaylistRepository(context: android.content.Context): PlaylistRepository {
            return playlistRepository ?: PlaylistRepository(
                provideMediaDatabase(
                    SicarioMediaDatabase.getInstance(context)
                ).playlistDao()
            ).also { playlistRepository = it }
        }
        
        fun provideMediaViewModel(context: android.content.Context): MediaViewModel {
            return mediaViewModel ?: MediaViewModel(
                provideMediaRepository(context)
            ).also { mediaViewModel = it }
        }
        
        fun providePlaylistViewModel(context: android.content.Context): PlaylistViewModel {
            return playlistViewModel ?: PlaylistViewModel(
                providePlaylistRepository(context)
            ).also { playlistViewModel = it }
        }
    }
}
