package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppDatabase
import com.example.data.database.VideoRepository
import com.example.data.scanner.MediaStoreVideoScanner
import com.example.ui.main.MediaDashboardScreen
import com.example.ui.player.VideoPlayerView
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VideoPlayerViewModel
import com.example.ui.viewmodel.VideoPlayerViewModelFactory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize DB and classes
        val database = AppDatabase.getDatabase(this)
        val repository = VideoRepository(database.videoDao(), database.audioDao())
        val scanner = MediaStoreVideoScanner(this)
        val deepScanner = com.example.data.scanner.RecursiveDirectoryScanner()
        val audioScanner = com.example.data.scanner.MediaStoreAudioScanner(this)
        
        val viewModel = ViewModelProvider(
            this,
            VideoPlayerViewModelFactory(repository, scanner, deepScanner, audioScanner)
        )[VideoPlayerViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val playingVideo by viewModel.playingVideo.collectAsStateWithLifecycle()
                val isVideoMinimized by viewModel.isVideoMinimized.collectAsStateWithLifecycle()

                // Register back press handler during video playback to close player
                BackHandler(enabled = playingVideo != null) {
                    if (!isVideoMinimized) {
                        viewModel.setVideoMinimized(true)
                    } else {
                        viewModel.closePlayer()
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (playingVideo == null || isVideoMinimized) {
                            MediaDashboardScreen(
                                viewModel = viewModel,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            )
                        }
                        
                        if (playingVideo != null) {
                            VideoPlayerView(
                                video = playingVideo!!,
                                isMinimized = isVideoMinimized,
                                onMinimize = { viewModel.setVideoMinimized(true) },
                                onMaximize = { viewModel.setVideoMinimized(false) },
                                onClose = { viewModel.closePlayer() },
                                onPlayNext = { viewModel.playNextVideo() },
                                onPlayPrevious = { viewModel.playPreviousVideo() },
                                videoRepository = repository,
                                modifier = if (isVideoMinimized) {
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(bottom = 80.dp, end = 16.dp)
                                        .width(200.dp)
                                        .aspectRatio(16f/9f)
                                        .clip(RoundedCornerShape(8.dp))
                                } else {
                                    Modifier.fillMaxSize()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
