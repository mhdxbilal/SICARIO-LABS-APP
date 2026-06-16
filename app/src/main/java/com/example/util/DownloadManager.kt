package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern

class DownloadManager(private val context: Context) {

    private var activeProcess: Process? = null

    /**
     * Copies the pre-bundled yt-dlp binary from assets to the secure filesDir
     * and sets native execution permissions (chmod +x equivalent).
     */
    suspend fun copyBinaryFromAssets(): File = withContext(Dispatchers.IO) {
        val file = YtDlpHelper.extractAndPrepareBinary(context)
            ?: throw IllegalStateException("Could not extract and prepare yt-dlp binary.")
        file
    }

    /**
     * Converts a modern DocumentTree Uri into a raw file path recognizable by ProcessBuilder.
     * Incorporates primary storage decoding and DocumentFile resolution fallbacks.
     */
    fun getPhysicalPathFromUri(uri: Uri): String {
        try {
            if ("content" == uri.scheme) {
                val docId = if (DocumentsContract.isTreeUri(uri)) {
                    DocumentsContract.getTreeDocumentId(uri)
                } else {
                    DocumentsContract.getDocumentId(uri)
                }
                if (docId != null) {
                    val parts = docId.split(":")
                    val type = parts[0]
                    val relativePath = if (parts.size > 1) parts[1] else ""
                    if ("primary" == type.lowercase()) {
                        return File(Environment.getExternalStorageDirectory(), relativePath).absolutePath
                    } else {
                        // Scan secondary storage partitions for folder mapping
                        val extDirs = context.getExternalFilesDirs(null)
                        for (extDir in extDirs) {
                            if (extDir != null) {
                                val absolutePath = extDir.absolutePath
                                val index = absolutePath.indexOf("/Android/data")
                                if (index >= 0) {
                                    val rootPath = absolutePath.substring(0, index)
                                    val candidate = File(rootPath, relativePath)
                                    if (candidate.exists()) {
                                        return candidate.absolutePath
                                    }
                                }
                            }
                        }
                    }
                }
            } else if ("file" == uri.scheme) {
                val path = uri.path
                if (path != null) return path
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback check using DocumentFile representation
        try {
            val docFile = DocumentFile.fromTreeUri(context, uri)
            if (docFile != null && docFile.exists()) {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                return downloadsDir.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return context.filesDir.absolutePath
    }

    /**
     * Initiates the download via ProcessBuilder on Dispatchers.IO.
     * Processes stdout stream in real-time to report speed and progress.
     */
    suspend fun startDownload(
        url: String,
        quality: String,
        folderUri: Uri?,
        onProgress: (Int, String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val ytDlpFile = copyBinaryFromAssets()
            val targetDirString = if (folderUri != null) getPhysicalPathFromUri(folderUri) else context.filesDir.absolutePath
            
            val targetDir = File(targetDirString)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            // Build parameters conforming exactly to requested output template
            val command = mutableListOf<String>()
            command.add(ytDlpFile.absolutePath)
            if (quality.trim().isNotEmpty() && quality != "best") {
                command.add("-f")
                command.add(quality)
            }
            command.add("-o")
            command.add(File(targetDir, "%(title)s.%(ext)s").absolutePath)
            command.add(url)

            val pb = ProcessBuilder(command)
            pb.directory(ytDlpFile.parentFile)
            pb.redirectErrorStream(true)

            val process = pb.start()
            activeProcess = process

            try {
                val reader = process.inputStream.bufferedReader()
                val progressPattern = Pattern.compile("\\[download\\]\\s+(\\d+(\\.\\d+)?)\\%")
                val speedPattern = Pattern.compile("at\\s+([^\\s]+)")

                var line: String? = reader.readLine()
                while (line != null) {
                    val cleanLine = line.trim()
                    if (cleanLine.isNotEmpty()) {
                        var progressPercent = 0
                        var hasProgress = false
                        val pm = progressPattern.matcher(cleanLine)
                        if (pm.find()) {
                            val percentStr = pm.group(1)
                            val dVal = percentStr?.toDoubleOrNull() ?: 0.0
                            progressPercent = dVal.toInt()
                            hasProgress = true
                        }

                        var speedVal = "Calculating"
                        val sm = speedPattern.matcher(cleanLine)
                        if (sm.find()) {
                            speedVal = sm.group(1) ?: "Calculating"
                        }

                        if (hasProgress) {
                            onProgress(progressPercent, speedVal)
                        }
                    }
                    line = reader.readLine()
                }

                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    onComplete(targetDirString)
                } else {
                    onError("yt-dlp exited with non-zero status code: $exitCode")
                }
            } finally {
                process.destroy()
                activeProcess = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e.localizedMessage ?: "Unknown compilation execution error")
        }
    }

    /**
     * Explicitly terminates any active execution process (cancel download cleanly).
     */
    fun cancelDownload() {
        try {
            activeProcess?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            activeProcess = null
        }
    }
}
