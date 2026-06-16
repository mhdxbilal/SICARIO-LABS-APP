package com.siciario.labs.mediaplayer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siciario.labs.mediaplayer.data.model.Playlist
import com.siciario.labs.mediaplayer.data.repository.PlaylistRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Playlist ViewModel - Manages playlist state and operations
 * Offline-first approach with local database
 */
class PlaylistViewModel(private val repository: PlaylistRepository) : ViewModel() {
    
    private val _uiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()
    
    init {
        loadPlaylists()
    }
    
    fun loadPlaylists() {
        viewModelScope.launch {
            repository.getAllPlaylists()
                .catch { error ->
                    _uiState.value = PlaylistUiState.Error(error.message ?: "Unknown error")
                }
                .collect { playlists ->
                    _uiState.value = PlaylistUiState.Success(playlists)
                }
        }
    }
    
    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            try {
                repository.createPlaylist(name, description)
                loadPlaylists()
            } catch (error: Exception) {
                _uiState.value = PlaylistUiState.Error("Failed to create playlist")
            }
        }
    }
    
    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            try {
                repository.deletePlaylist(playlist)
                loadPlaylists()
            } catch (error: Exception) {
                _uiState.value = PlaylistUiState.Error("Failed to delete playlist")
            }
        }
    }
    
    fun addToPlaylist(playlistId: Long, mediaItemId: Long) {
        viewModelScope.launch {
            try {
                repository.addToPlaylist(playlistId, mediaItemId, 0)
            } catch (error: Exception) {
                _uiState.value = PlaylistUiState.Error("Failed to add to playlist")
            }
        }
    }
}

/**
 * Playlist UI State - Represents different states of playlist loading
 */
sealed class PlaylistUiState {
    object Loading : PlaylistUiState()
    data class Success(val playlists: List<Playlist>) : PlaylistUiState()
    data class Error(val message: String) : PlaylistUiState()
}
