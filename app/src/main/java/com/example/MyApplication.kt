package com.example

import android.app.Application
import com.example.util.MediaCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize pre-warming service asynchronously to keep UI thread responsive
        CoroutineScope(Dispatchers.IO).launch {
            // Pre-warm the cache and playback surface requirements
            MediaCacheManager.getInstance(this@MyApplication)
        }
    }
}
