package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.DownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadManager = DownloadManager(application)
    
    private val _uiState = MutableStateFlow<DownloadUiState>(DownloadUiState.Idle)
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    /**
     * Executes the download process safely inside viewModelScope on Dispatchers.IO.
     * Continuously converts binary callbacks to StateFlow emissions on the main thread dispatcher.
     */
    fun initiateDownload(url: String, quality: String, folderUri: Uri?) {
        viewModelScope.launch {
            // Update uiState to Loading on Main Thread before launching process
            _uiState.value = DownloadUiState.Loading

            withContext(Dispatchers.IO) {
                downloadManager.startDownload(
                    url = url,
                    quality = quality,
                    folderUri = folderUri,
                    onProgress = { progress, speed ->
                        // Safely emit to StateFlow
                        _uiState.value = DownloadUiState.Downloading(progress, speed)
                    },
                    onComplete = { finalPath ->
                        _uiState.value = DownloadUiState.Completed(finalPath)
                    },
                    onError = { errorMessage ->
                        _uiState.value = DownloadUiState.Error(errorMessage)
                    }
                )
            }
        }
    }

    /**
     * Cleanly cancels the download by destroying the active subprocess.
     */
    fun cancelDownload() {
        viewModelScope.launch(Dispatchers.IO) {
            downloadManager.cancelDownload()
            _uiState.value = DownloadUiState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Ensure process is destroyed when ViewModel is finished or activity recreated
        downloadManager.cancelDownload()
    }
}
