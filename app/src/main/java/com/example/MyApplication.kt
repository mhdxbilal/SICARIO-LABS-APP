package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.util.MediaCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.ffmpeg.FFmpeg
import android.util.Log

class MyApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize pre-warming service asynchronously to keep UI thread responsive
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(this@MyApplication)
                FFmpeg.getInstance().init(this@MyApplication)
                // Download latest yt-dlp binary if needed (optional but recommended)
                // YoutubeDL.getInstance().updateYoutubeDL(this@MyApplication, YoutubeDL.UpdateChannel.STABLE)
            } catch (e: Exception) {
                Log.e("MyApplication", "failed to init youtubedl", e)
            }
            
            // Pre-warm the cache and playback surface requirements
            MediaCacheManager.getInstance(this@MyApplication)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Allocate 25% of memory for imaging
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512 * 1024 * 1024) // 512 MB disk cache
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
