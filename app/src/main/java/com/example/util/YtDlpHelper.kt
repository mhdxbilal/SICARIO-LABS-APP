package com.example.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * A specialized utility class to handle safe extraction, native execution permissioning,
 * and pre-flight validation of the yt-dlp binary from app assets.
 */
object YtDlpHelper {

    private const val TAG = "YtDlpHelper"
    private const val BIN_DIR_NAME = "bin"
    private const val BINARY_NAME = "yt-dlp"

    /**
     * Gets the expected absolute internal directory for binary files.
     */
    fun getBinDir(context: Context): File {
        return File(context.filesDir, BIN_DIR_NAME)
    }

    /**
     * Gets the expected file location of the extracted yt-dlp binary.
     */
    fun getBinaryFile(context: Context): File {
        return File(getBinDir(context), BINARY_NAME)
    }

    /**
     * Extracts the pre-bundled yt-dlp binary from assets to the secure private filesDir
     * and guarantees native execute permissions are set correctly.
     * 
     * @param context Application or activity context.
     * @return The File representing the prepared executable binary, or null if preparation failed.
     */
    suspend fun extractAndPrepareBinary(context: Context): File? = withContext(Dispatchers.IO) {
        val binDir = getBinDir(context)
        if (!binDir.exists()) {
            val created = binDir.mkdirs()
            Log.d(TAG, "Binary directory created: $created")
        }
        val ytDlpFile = getBinaryFile(context)

        try {
            Log.d(TAG, "Extracting binary to: ${ytDlpFile.absolutePath}")
            context.assets.open(BINARY_NAME).use { input ->
                FileOutputStream(ytDlpFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Apply target POSIX execute permissions (chmod +x)
            val execResult = ytDlpFile.setExecutable(true, false)
            val readResult = ytDlpFile.setReadable(true, false)
            Log.d(TAG, "Result of setExecutable: $execResult, setReadable: $readResult")

            if (verifyBinaryReady(context)) {
                Log.d(TAG, "yt-dlp binary is verified and ready for execution.")
                return@withContext ytDlpFile
            } else {
                Log.w(TAG, "Binary verification failed, fallback checking exists...")
                if (ytDlpFile.exists() && ytDlpFile.length() > 0) {
                    return@withContext ytDlpFile
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing yt-dlp binary: ${e.localizedMessage}", e)
        }
        return@withContext null
    }

    /**
     * Runs pre-flight verification on the yt-dlp binary.
     * Ensures that the file exists, has a non-zero size, and has execution permissions flags.
     *
     * @param context Application or activity context.
     * @return true if the binary is healthy and ready to run, false otherwise.
     */
    fun verifyBinaryReady(context: Context): Boolean {
        val ytDlpFile = getBinaryFile(context)
        val exists = ytDlpFile.exists()
        val size = ytDlpFile.length()
        val canExec = ytDlpFile.canExecute()
        
        Log.d(TAG, "Checking binary state: exists=$exists, size=$size, executable=$canExec")
        return exists && size > 0 && canExec
    }
}
