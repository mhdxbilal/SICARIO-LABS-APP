package com.example.viewmodel

sealed class DownloadUiState {
    object Idle : DownloadUiState()
    object Loading : DownloadUiState()
    data class Downloading(val progress: Int, val speed: String) : DownloadUiState()
    data class Completed(val filePath: String) : DownloadUiState()
    data class Error(val message: String) : DownloadUiState()
}
