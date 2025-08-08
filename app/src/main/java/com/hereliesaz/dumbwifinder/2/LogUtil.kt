package com.hereliesaz.dumbwifinder.`2`

import android.util.Log

object LogUtil {
    private const val TAG = "DumbWiFinder"

    fun logSuccess(ssid: String, password: String) {
        Log.d(TAG, "Successfully cracked SSID: $ssid with password: $password")
        // In a real app, you would also write this to a persistent file.
    }

    fun logFailure(ssid: String, password: String) {
        Log.d(TAG, "Failed to crack SSID: $ssid with password: $password")
        // In a real app, you might want to log failures for retry logic.
    }

    fun logInfo(message: String) {
        Log.i(TAG, message)
    }

    fun logError(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
