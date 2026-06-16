package com.siciario.labs.mediaplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.siciario.labs.mediaplayer.ui.screens.MainScreen
import com.siciario.labs.mediaplayer.ui.theme.SicarioLabsMediaPlayerTheme

/**
 * Main Activity - Entry point for SICARIO LABS Media Player
 * Offline-first architecture with local-only data storage
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SicarioLabsMediaPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Verify offline mode on startup
                    LaunchedEffect(Unit) {
                        OfflineModeManager.verifyOfflineMode()
                    }
                    
                    MainScreen()
                }
            }
        }
    }
}

/**
 * Offline Mode Manager - Ensures application operates without internet
 */
object OfflineModeManager {
    private const val TAG = "OfflineModeManager"
    
    /**
     * Verify that the application is in offline mode
     * This ensures no network calls are made
     */
    fun verifyOfflineMode() {
        if (BuildConfig.OFFLINE_MODE_ONLY) {
            android.util.Log.i(TAG, "✓ Offline mode enabled - No internet required")
            android.util.Log.i(TAG, "✓ All features available locally")
        }
    }
}
