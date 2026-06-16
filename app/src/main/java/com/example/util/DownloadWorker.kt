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
import java.util.regex.Pattern

class DownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private var activeProcess: Process? = null

    companion object {
        const val KEY_URL = "url"
        const val KEY_DESTINATION_URI = "destination_uri"
        const val KEY_FORMAT = "format"
        
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS_TEXT = "status_text"
        const val KEY_SPEED = "speed"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_FILE_SIZE = "file_size"
        
        private const val NOTIFICATION_ID = 4242
        private const val CHANNEL_ID = "downloader_channel"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val destUriStr = inputData.getString(KEY_DESTINATION_URI) ?: ""
        
        setForeground(createForegroundInfo(0, "Preparing Downloader..."))
        
        try {
            // 1. Copy yt-dlp from assets to internal filesDir
            val binDir = File(context.filesDir, "bin")
            if (!binDir.exists()) {
                binDir.mkdirs()
            }
            val ytDlpFile = File(binDir, "yt-dlp")
            
            // Extract bhdlp from assets
            context.assets.open("yt-dlp").use { input ->
                FileOutputStream(ytDlpFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Add native execute permissions (Linux chmod +x equivalent)
            ytDlpFile.setExecutable(true, false)
            
            if (!ytDlpFile.exists() || !ytDlpFile.canExecute()) {
                return@withContext Result.failure(workDataOf(KEY_STATUS_TEXT to "Verification failed: Unable to prepare yt-dlp binary or lack exec rights"))
            }
            
            // 2. Resolve temporary directory to run yt-dlp
            val tempDownloadDir = File(context.cacheDir, "downloads")
            if (!tempDownloadDir.exists()) {
                tempDownloadDir.mkdirs()
            }
            
            val tempFilePrefix = "ytdl_" + System.currentTimeMillis()
            val format = inputData.getString(KEY_FORMAT)
            
            // Build ProcessBuilder command using /system/bin/sh for modern Android compatibility
            val command = mutableListOf<String>()
            command.add("/system/bin/sh")
            command.add(ytDlpFile.absolutePath)
            if (!format.isNullOrEmpty()) {
                command.add("-f")
                command.add(format)
            }
            command.add("-o")
            command.add(File(tempDownloadDir, "${tempFilePrefix}_%(title)s.%(ext)s").absolutePath)
            command.add(url)
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(binDir)
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            activeProcess = process
            
            try {
                val reader = process.inputStream.bufferedReader()
                
                val progressPattern = Pattern.compile("\\[download\\]\\s+(\\d+(\\.\\d+)?)\\%")
                val speedPattern = Pattern.compile("at\\s+([^\\s]+)")
                
                var line: String? = reader.readLine()
                while (line != null) {
                    val cleanLine = line.trim()
                    if (cleanLine.isNotEmpty()) {
                        var progressInt = 0
                        var hasProgress = false
                        val matcher = progressPattern.matcher(cleanLine)
                        if (matcher.find()) {
                            val percentageStr = matcher.group(1)
                            val progressDouble = percentageStr?.toDoubleOrNull() ?: 0.0
                            progressInt = progressDouble.toInt()
                            hasProgress = true
                        }
                        
                        var speedValue = ""
                        val speedMatcher = speedPattern.matcher(cleanLine)
                        if (speedMatcher.find()) {
                            speedValue = speedMatcher.group(1) ?: ""
                        }
                        
                        if (hasProgress) {
                            // Update real-time progress for observers
                            setProgress(workDataOf(
                                KEY_PROGRESS to progressInt,
                                KEY_STATUS_TEXT to cleanLine,
                                KEY_SPEED to speedValue
                            ))
                            
                            // Update Progress Notification
                            setForeground(createForegroundInfo(progressInt, "Downloading: $progressInt% ($speedValue)"))
                        }
                    }
                    line = reader.readLine()
                }
                
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    return@withContext Result.failure(workDataOf(KEY_STATUS_TEXT to "yt-dlp process returned non-zero exit code: $exitCode"))
                }
            } finally {
                // Ensure native process is always destroyed closed
                process.destroy()
                activeProcess = null
            }
            
            // Try matching download actual file (yt-dlp can alter extension depending on target streaming quality)
            val actualFile = tempDownloadDir.listFiles()?.find { it.name.startsWith(tempFilePrefix) } 
                ?: File(tempDownloadDir, "${tempFilePrefix}.mp4")
            
            if (!actualFile.exists() || actualFile.length() == 0L) {
                return@withContext Result.failure(workDataOf(KEY_STATUS_TEXT to "Downloaded file not found or empty!"))
            }
            
            // 3. Move file to final location choice
            var finalPath = actualFile.absolutePath
            if (destUriStr.isNotEmpty()) {
                val destUri = android.net.Uri.parse(destUriStr)
                if (destUri.scheme == "content") {
                    val documentDir = DocumentFile.fromTreeUri(context, destUri)
                    if (documentDir != null && documentDir.exists()) {
                        val cleanFilename = actualFile.name.substringAfter("_")
                        val newFile = documentDir.createFile("video/mp4", cleanFilename)
                        if (newFile != null) {
                            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                                actualFile.inputStream().use { inputStream ->
                                    inputStream.copyTo(outputStream)
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
                    val destFile = File(destDir, actualFile.name.substringAfter("_"))
                    actualFile.copyTo(destFile, overwrite = true)
                    actualFile.delete()
                    finalPath = destFile.absolutePath
                }
            } else {
                val downloadsPublicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsPublicDir.exists()) {
                    downloadsPublicDir.mkdirs()
                }
                val destFile = File(downloadsPublicDir, actualFile.name.substringAfter("_"))
                actualFile.copyTo(destFile, overwrite = true)
                actualFile.delete()
                finalPath = destFile.absolutePath
            }
            
            // Fast notify MediaScanner
            try {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(finalPath),
                    arrayOf("video/mp4"),
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            return@withContext Result.success(workDataOf(
                KEY_PROGRESS to 100,
                KEY_STATUS_TEXT to "Download complete",
                KEY_FILE_PATH to finalPath,
                KEY_FILE_SIZE to actualFile.length()
            ))
            
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(workDataOf(KEY_STATUS_TEXT to "Error: ${e.localizedMessage}"))
        }
    }

    private fun createForegroundInfo(progress: Int, statusText: String): ForegroundInfo {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Downloader", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("MhdxBilal Media Downloader")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
            
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}
