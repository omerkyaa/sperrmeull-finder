package com.omerkaya.sperrmuellfinder.core.util

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around Android logging to provide consistent tagging and formatting
 * Rules.md compliant - Core utility class
 */
@Singleton
class Logger @Inject constructor() {
    companion object {
        const val TAG_DEFAULT = "SperrmullDefault"
        const val TAG_FIREBASE = "SperrmullFirebase"
        const val TAG_AUTH = "SperrmullAuth"
        const val TAG_USER = "SperrmullUser"
        const val TAG_PREMIUM = "SperrmullPremium"
        const val TAG_POSTS = "SperrmullPosts"
        const val TAG_CAMERA = "SperrmullCamera"
        const val TAG_MAP = "SperrmullMap"
        const val TAG_NOTIFICATIONS = "SperrmullNotifications"
        const val TAG_NETWORK = "SperrmullNetwork"
        const val TAG_LOCATION = "SperrmullLocation"
    }

    fun d(tag: String, message: String) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message)
        }
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    @JvmOverloads
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }

    @JvmOverloads
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}