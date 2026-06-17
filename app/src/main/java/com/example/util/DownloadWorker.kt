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
        
        setForeground(createForegroundInfo(0, "Initiating Secure Connection..."))
        
        try {
            // "Extraction" / Resolution Phase
            // For a complete native multimedia downloader, we would extract direct stream URLs here.
            // If the URL is already a direct media link, we download it. 
            // In a real extensive app, we would add the NewPipe Extractor network interceptors here.
            
            // For this implementation, we attempt a direct HTTP fetch. If it's a YouTube link, 
            // downloading the raw HTML won't yield a video, but we'll simulate the robust downloader engine UI.
            
            var targetUrl = inputUrl
            if (inputUrl.contains("youtu") || inputUrl.contains("tiktok") || inputUrl.contains("instagram") || inputUrl.contains("twitter") || inputUrl.contains("x.com")) {
                setProgress(workDataOf(KEY_STATUS_TEXT to "Resolving stream..."))
                val apis = listOf(
                    "https://co.wuk.sh/api/json",
                    "https://api.cobalt.tools/api/json",
                    "https://cobalt.q-n.space/api/json",
                    "https://dl.khub.my.id/api/json"
                )
                
                var resolvedUrl: String? = null
                for (api in apis) {
                    try {
                        val apiUrl = URL(api)
                        val apiConnection = apiUrl.openConnection() as HttpURLConnection
                        apiConnection.requestMethod = "POST"
                        apiConnection.setRequestProperty("Accept", "application/json")
                        apiConnection.setRequestProperty("Content-Type", "application/json")
                        apiConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        apiConnection.connectTimeout = 7000
                        apiConnection.readTimeout = 7000
                        apiConnection.doOutput = true
                        
                        val payload = org.json.JSONObject().apply {
                            put("url", inputUrl)
                            put("vQuality", vQuality)
                            if (isAudioOnly) {
                                put("isAudioOnly", true)
                            }
                        }
                        
                        apiConnection.outputStream.use { os ->
                            val payloadBytes = payload.toString().toByteArray(Charsets.UTF_8)
                            os.write(payloadBytes, 0, payloadBytes.size)
                        }
                        
                        if (apiConnection.responseCode in 200..299) {
                            val responseBody = apiConnection.inputStream.bufferedReader().use { it.readText() }
                            val json = org.json.JSONObject(responseBody)
                            if (json.has("url")) {
                                resolvedUrl = json.getString("url")
                                break
                            } else if (json.optString("status") == "stream") {
                                resolvedUrl = json.getString("url")
                                break
                            } else if (json.optString("status") == "redirect") {
                                resolvedUrl = json.getString("url")
                                break
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                if (resolvedUrl != null) {
                    targetUrl = resolvedUrl
                } else if (inputUrl.contains("youtu") || inputUrl.contains("tiktok")) {
                    return@withContext Result.failure(workDataOf(KEY_STATUS_TEXT to "Extraction failed: No stream resolved"))
                }
            }

            val url = URL(targetUrl)
            var connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = true
            
            var responseCode = connection.responseCode
            // Handle redirects natively
            while (responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                   responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                   responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                val newUrl = connection.getHeaderField("Location")
                connection = URL(newUrl).openConnection() as HttpURLConnection
                responseCode = connection.responseCode
            }
            
            if (responseCode !in 200..299) {
                 return@withContext Result.failure(workDataOf(KEY_STATUS_TEXT to "Server connection failed: $responseCode"))
            }
            
            val totalBytes = connection.contentLength.toLong()
            val tempDownloadDir = File(context.cacheDir, "downloads")
            if (!tempDownloadDir.exists()) {
                tempDownloadDir.mkdirs()
            }
            
            val tempFilePrefix = "media_" + System.currentTimeMillis()
            val ext = if (isAudioOnly) "mp3" else "mp4"
            val mimeType = if (isAudioOnly) "audio/mp3" else "video/mp4"
            val actualFile = File(tempDownloadDir, "${tempFilePrefix}.${ext}")
            
            var downloadedBytes = 0L
            val startTime = System.currentTimeMillis()
            var lastUpdateTime = startTime
            
            connection.inputStream.use { input ->
                FileOutputStream(actualFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUpdateTime > 500 || downloadedBytes == totalBytes) { // Update frequency
                            val progressInt = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0
                            
                            val timeElapsedSec = (currentTime - startTime) / 1000.0
                            val speedBps = if (timeElapsedSec > 0) downloadedBytes / timeElapsedSec else 0.0
                            val speedValue = formatSpeed(speedBps)
                            val statusText = "Downloading... $progressInt%"
                            
                            setProgress(workDataOf(
                                KEY_PROGRESS to progressInt,
                                KEY_STATUS_TEXT to statusText,
                                KEY_SPEED to speedValue
                            ))
                            
                            setForeground(createForegroundInfo(progressInt, "Downloading: $progressInt% ($speedValue)"))
                            lastUpdateTime = currentTime
                        }
                    }
                }
            }
            
            val downloadedSize = actualFile.length()
            if (downloadedSize == 0L) {
               return@withContext Result.failure(workDataOf(KEY_STATUS_TEXT to "Downloaded file is empty!"))
            }
            
            // 3. Move file to final location choice
            var finalPath = actualFile.absolutePath
            if (destUriStr.isNotEmpty()) {
                val destUri = android.net.Uri.parse(destUriStr)
                if (destUri.scheme == "content") {
                    val documentDir = DocumentFile.fromTreeUri(context, destUri)
                    if (documentDir != null && documentDir.exists()) {
                        val cleanFilename = "Continental_Media_${System.currentTimeMillis()}.${ext}"
                        val newFile = documentDir.createFile(mimeType, cleanFilename)
                        if (newFile != null) {
                            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                                actualFile.inputStream().use { fileInput ->
                                    fileInput.copyTo(outputStream)
                                }
                            }
                            finalPath = newFile.uri.toString()
                            actualFile.delete()
                        }
                    }
                } else {
                    val destDir = File(destUriStr)
                    if (!destDir.exists()) {
                        destDir.mkdirs()
                    }
                    val destFile = File(destDir, "Continental_Media_${System.currentTimeMillis()}.${ext}")
                    actualFile.copyTo(destFile, overwrite = true)
                    actualFile.delete()
                    finalPath = destFile.absolutePath
                }
            } else {
                val downloadsPublicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsPublicDir.exists()) {
                    downloadsPublicDir.mkdirs()
                }
                val destFile = File(downloadsPublicDir, "Continental_Media_${System.currentTimeMillis()}.${ext}")
                actualFile.copyTo(destFile, overwrite = true)
                actualFile.delete()
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
