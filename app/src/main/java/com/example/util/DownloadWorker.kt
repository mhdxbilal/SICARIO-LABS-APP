package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

class DownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_URL = "url"
        const val KEY_DESTINATION_URI = "destination_uri"
        const val KEY_FORMAT = "format"
        
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS_TEXT = "status_text"
        const val KEY_SPEED = "speed"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_FILE_SIZE = "file_size"
        
        private const val NOTIFICATION_ID = 4243
        private const val CHANNEL_ID = "native_downloader_channel"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val inputUrl = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val destUriStr = inputData.getString(KEY_DESTINATION_URI) ?: ""
        val qualityArg = inputData.getString(KEY_FORMAT) ?: "max"
        val isAudioOnly = qualityArg == "audio"
        val vQuality = if (isAudioOnly) "max" else qualityArg
        
        setForeground(createForegroundInfo(0, "Initializing God-Tier Downloader..."))
        
        try {
            val tempDownloadDir = File(context.cacheDir, "downloads")
            if (!tempDownloadDir.exists()) {
                tempDownloadDir.mkdirs()
            }
            
            val tempFilePrefix = "media_" + System.currentTimeMillis()
            val baseExt = if (isAudioOnly) "mp3" else "mp4"
            val actualFile = File(tempDownloadDir, "${tempFilePrefix}.${baseExt}")
            
            val request = com.yausername.youtubedl_android.YoutubeDLRequest(inputUrl)
            // Use ext wildcard so yt-dlp doesn't fail on format conflicts
            request.addOption("-o", File(tempDownloadDir, "${tempFilePrefix}.%(ext)s").absolutePath)
            
            if (isAudioOnly) {
                request.addOption("-f", "bestaudio")
                request.addOption("--extract-audio")
                request.addOption("--audio-format", "mp3")
            } else {
                val query = when (vQuality) {
                    "max" -> "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"
                    "2160" -> "bestvideo[height<=2160][ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"
                    "1440" -> "bestvideo[height<=1440][ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"
                    "1080" -> "bestvideo[height<=1080][ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"
                    "720" -> "bestvideo[height<=720][ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"
                    "480" -> "bestvideo[height<=480][ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"
                    "360" -> "bestvideo[height<=360][ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"
                    else -> "bestvideo[height<=720][ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"
                }
                request.addOption("-f", query)
                request.addOption("--merge-output-format", "mp4")
            }
            
            var lastUpdateTime = System.currentTimeMillis()
            
            com.yausername.youtubedl_android.YoutubeDL.getInstance().execute(request, tempFilePrefix) { progress: Float, etaInSeconds: Long, line: String ->
                val currentTime = System.currentTimeMillis()
                if ((currentTime - lastUpdateTime > 500) || (progress > 99.0f)) {
                    val progressInt = progress.toInt()
                    val speedStr = if (line.contains("at ")) line.substringAfter("at ").substringBefore(" ETA") else "calculating..."
                    val statusText = "Downloading... $progressInt%"
                    
                    setProgressAsync(workDataOf(
                        KEY_PROGRESS to progressInt,
                        KEY_STATUS_TEXT to statusText,
                        KEY_SPEED to speedStr
                    ))
                    lastUpdateTime = currentTime
                }
                kotlin.Unit
            }
            
            // Find the actual generated file
            val downloadedFile = tempDownloadDir.listFiles { _, name -> name.startsWith(tempFilePrefix) }?.firstOrNull()
            
            if (downloadedFile == null || downloadedFile.length() == 0L) {
               return@withContext Result.failure(workDataOf(KEY_STATUS_TEXT to "Downloaded file is empty!"))
            }

            val downloadedSize = downloadedFile.length()
            val finalExt = downloadedFile.extension
            val mimeType = if (isAudioOnly) "audio/${finalExt}" else "video/${finalExt}"
            
            // 3. Move file to final location choice
            var finalPath = downloadedFile.absolutePath
            if (destUriStr.isNotEmpty()) {
                val destUri = android.net.Uri.parse(destUriStr)
                if (destUri.scheme == "content") {
                    val documentDir = DocumentFile.fromTreeUri(context, destUri)
                    if (documentDir != null && documentDir.exists()) {
                        val cleanFilename = "Continental_Media_${System.currentTimeMillis()}.${finalExt}"
                        val newFile = documentDir.createFile(mimeType, cleanFilename)
                        if (newFile != null) {
                            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                                downloadedFile.inputStream().use { fileInput ->
                                    fileInput.copyTo(outputStream)
                                }
                            }
                            finalPath = newFile.uri.toString()
                            downloadedFile.delete()
                        }
                    }
                } else {
                    val destDir = File(destUriStr)
                    if (!destDir.exists()) {
                        destDir.mkdirs()
                    }
                    val destFile = File(destDir, "Continental_Media_${System.currentTimeMillis()}.${finalExt}")
                    downloadedFile.copyTo(destFile, overwrite = true)
                    downloadedFile.delete()
                    finalPath = destFile.absolutePath
                }
            } else {
                val downloadsPublicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsPublicDir.exists()) {
                    downloadsPublicDir.mkdirs()
                }
                val destFile = File(downloadsPublicDir, "Continental_Media_${System.currentTimeMillis()}.${finalExt}")
                downloadedFile.copyTo(destFile, overwrite = true)
                downloadedFile.delete()
                finalPath = destFile.absolutePath
            }
            
            // Fast notify MediaScanner
            try {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(finalPath),
                    arrayOf(mimeType),
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            setProgress(workDataOf(
                KEY_PROGRESS to 100,
                KEY_STATUS_TEXT to "Download complete",
                KEY_SPEED to "0 B/s"
            ))
            
            return@withContext Result.success(workDataOf(
                KEY_PROGRESS to 100,
                KEY_STATUS_TEXT to "Download complete",
                KEY_FILE_PATH to finalPath,
                KEY_FILE_SIZE to downloadedSize
            ))
            
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(workDataOf(KEY_STATUS_TEXT to "Error: ${e.localizedMessage}"))
        }
    }
    
    private fun formatSpeed(bytesPerSec: Double): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format("%.2f MB/s", bytesPerSec / (1024 * 1024))
            bytesPerSec >= 1024 -> String.format("%.2f KB/s", bytesPerSec / 1024)
            else -> "${bytesPerSec.roundToInt()} B/s"
        }
    }

    private fun createForegroundInfo(progress: Int, statusText: String): ForegroundInfo {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Native Downloader", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("MhdxBilal Native Downloader")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
            
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
}
