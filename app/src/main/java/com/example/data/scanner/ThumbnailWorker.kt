package com.example.data.scanner

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import com.example.data.database.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ThumbnailWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val videoDao = database.videoDao()
            val audioDao = database.audioDao()
            val repository = VideoRepository(videoDao, audioDao)

            val currentVideos = videoDao.getAllVideosSync()

            currentVideos.forEach { video ->
                if (video.thumbnailPath.isNullOrBlank()) {
                    try {
                        val path = ThumbnailGenerator.generateThumbnail(applicationContext, video)
                        if (path != null) {
                            repository.updateVideo(video.copy(thumbnailPath = path))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
