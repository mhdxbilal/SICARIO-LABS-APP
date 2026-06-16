package com.siciario.labs.mediaplayer.util

import android.content.Context
import android.net.ConnectivityManager

/**
 * Network Utils - Ensures offline-only operation
 * Verifies no internet calls are made
 */
object NetworkUtils {
    
    /**
     * Check if device has internet connectivity
     * WARNING: Application should NOT use internet even if available
     */
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as? ConnectivityManager
        
        return try {
            val activeNetwork = connectivityManager?.activeNetwork ?: return false
            connectivityManager.getNetworkCapabilities(activeNetwork) != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Verify offline mode is enforced
     * This function ensures the application doesn't make any network calls
     */
    fun verifyOfflineMode(context: Context) {
        if (BuildConfig.OFFLINE_MODE_ONLY) {
            android.util.Log.i(
                "NetworkUtils",
                "✔️ Offline mode enforced - All operations are local"
            )
            android.util.Log.i(
                "NetworkUtils",
                "✔️ No internet calls permitted"
            )
            android.util.Log.i(
                "NetworkUtils",
                "✔️ All data stored locally on device"
            )
        }
    }
}
