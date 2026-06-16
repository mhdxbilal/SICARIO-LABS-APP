package com.siciario.labs.mediaplayer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siciario.labs.mediaplayer.data.model.MediaItem
import com.siciario.labs.mediaplayer.data.repository.MediaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Media ViewModel - Manages media library state and operations
 * Offline-first approach with local database
 */
class MediaViewModel(private val repository: MediaRepository) : ViewModel() {
    
    private val _uiState = MutableStateFlow<MediaUiState>(MediaUiState.Loading)
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    init {
        loadAllMedia()
    }
    
    fun loadAllMedia() {
        viewModelScope.launch {
            repository.getAllMedia()
                .catch { error ->
                    _uiState.value = MediaUiState.Error(error.message ?: "Unknown error")
                }
                .collect { mediaItems ->
                    _uiState.value = MediaUiState.Success(mediaItems)
                }
        }
    }
    
    fun searchMedia(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            loadAllMedia()
        } else {
            viewModelScope.launch {
                repository.searchMedia(query)
                    .catch { error ->
                        _uiState.value = MediaUiState.Error(error.message ?: "Search failed")
                    }
                    .collect { mediaItems ->
                        _uiState.value = MediaUiState.Success(mediaItems)
                    }
            }
        }
    }
    
    fun toggleFavorite(mediaItem: MediaItem) {
        viewModelScope.launch {
            repository.toggleFavorite(mediaItem.id, !mediaItem.isFavorite)
        }
    }
    
    fun recordMediaPlay(mediaItem: MediaItem) {
        viewModelScope.launch {
            repository.recordMediaPlay(mediaItem.id, System.currentTimeMillis())
        }
    }
    
    fun loadFavoriteMedia() {
        viewModelScope.launch {
            repository.getFavoriteMedia()
                .catch { error ->
                    _uiState.value = MediaUiState.Error(error.message ?: "Unknown error")
                }
                .collect { mediaItems ->
                    _uiState.value = MediaUiState.Success(mediaItems)
                }
        }
    }
    
    fun loadRecentlyPlayedMedia() {
        viewModelScope.launch {
            repository.getRecentlyPlayedMedia()
                .catch { error ->
                    _uiState.value = MediaUiState.Error(error.message ?: "Unknown error")
                }
                .collect { mediaItems ->
                    _uiState.value = MediaUiState.Success(mediaItems)
                }
        }
    }
}

/**
 * Media UI State - Represents different states of media loading
 */
sealed class MediaUiState {
    object Loading : MediaUiState()
    data class Success(val mediaItems: List<MediaItem>) : MediaUiState()
    data class Error(val message: String) : MediaUiState()
}
