package com.example.util

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File

object MediaCacheManager {
    private var cache: SimpleCache? = null
    private const val MAX_CACHE_SIZE: Long = 100 * 1024 * 1024 // 100 MB

    @Synchronized
    fun getInstance(context: Context): SimpleCache {
        if (cache == null) {
            val cacheDir = File(context.cacheDir, "media_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE)
            val databaseProvider = StandaloneDatabaseProvider(context)
            cache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return cache!!
    }

    fun buildExoPlayer(context: Context, renderersFactory: DefaultRenderersFactory? = null): ExoPlayer {
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(getInstance(context))
            .setUpstreamDataSourceFactory(dataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheDataSourceFactory)

        val builder = if (renderersFactory != null) {
            ExoPlayer.Builder(context, renderersFactory)
        } else {
            ExoPlayer.Builder(context)
        }
        return builder.setMediaSourceFactory(mediaSourceFactory).build()
    }
}
