package com.omerkaya.sperrmuellfinder.core.util

import android.util.Log

/**
 * Utility for detecting and handling problematic content:// URIs
 * 
 * This helps identify URIs that cause SecurityException:
 * - content://media/picker_get_content/... (Photo Picker URIs)
 * - Other temporary content:// URIs that lose permission
 */
object ContentUriCleaner {
    
    /**
     * Check if a URI string is a problematic content:// URI
     */
    fun isProblematicContentUri(uri: String?): Boolean {
        if (uri.isNullOrBlank()) return false
        
        // Allow our own FileProvider URIs - these are safe
        if (uri.contains(".fileprovider/")) {
            return false
        }
        
        // Block known problematic URIs
        return uri.startsWith("content://media/picker_get_content") ||
               uri.startsWith("content://com.android.providers.media.photopicker") ||
               uri.startsWith("content://media/picker/")
    }
    
    /**
     * Check if a URI string is a valid Firebase Storage URL
     */
    fun isFirebaseStorageUrl(uri: String?): Boolean {
        if (uri.isNullOrBlank()) return false
        
        return uri.startsWith("https://firebasestorage.googleapis.com/") ||
               uri.startsWith("https://storage.googleapis.com/")
    }
    
    /**
     * Get a safe image URL for Glide loading
     * Returns null if the URI is problematic to prevent crashes
     */
    fun getSafeImageUrl(uri: String?): String? {
        if (uri.isNullOrBlank()) return null
        
        return when {
            isFirebaseStorageUrl(uri) -> {
                Log.d("ContentUriCleaner", "✅ Safe Firebase Storage URL: ${uri.take(50)}...")
                uri // Safe Firebase URL
            }
            uri.contains(".fileprovider/") -> {
                Log.d("ContentUriCleaner", "✅ Safe FileProvider URI: ${uri.take(50)}...")
                uri // Safe FileProvider URI
            }
            isProblematicContentUri(uri) -> {
                Log.w("ContentUriCleaner", "🚫 Blocked problematic content URI: ${uri.take(50)}...")
                null // Block problematic URIs
            }
            uri.startsWith("http://") || uri.startsWith("https://") -> {
                Log.d("ContentUriCleaner", "✅ Safe HTTP URL: ${uri.take(50)}...")
                uri // Other HTTP URLs
            }
            uri.startsWith("content://media/external") -> {
                Log.d("ContentUriCleaner", "✅ Safe MediaStore URI: ${uri.take(50)}...")
                uri // Safe MediaStore URIs
            }
            else -> {
                Log.w("ContentUriCleaner", "⚠️ Unknown URI format, blocking for safety: ${uri.take(50)}...")
                null // Block unknown formats for safety
            }
        }
    }
    
    /**
     * Log statistics about URI types in a collection
     */
    fun logUriStatistics(uris: List<String?>, collectionName: String) {
        val total = uris.size
        val firebaseUrls = uris.count { isFirebaseStorageUrl(it) }
        val problematicUris = uris.count { isProblematicContentUri(it) }
        val nullOrEmpty = uris.count { it.isNullOrBlank() }
        val other = total - firebaseUrls - problematicUris - nullOrEmpty
        
        Log.i("ContentUriCleaner", """
            📊 URI Statistics for $collectionName:
            Total: $total
            ✅ Firebase Storage URLs: $firebaseUrls
            🚫 Problematic content:// URIs: $problematicUris
            ⚪ Null/Empty: $nullOrEmpty
            ❓ Other: $other
        """.trimIndent())
        
        if (problematicUris > 0) {
            Log.w("ContentUriCleaner", "⚠️ Found $problematicUris problematic URIs in $collectionName that may cause crashes!")
        }
    }
}
