package com.omerkaya.sperrmuellfinder.data.util

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.omerkaya.sperrmuellfinder.core.util.Logger
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🧹 CONTENT URI CLEANUP USE CASE
 * Utility to clean up problematic content:// URIs from Firestore
 * 
 * This use case:
 * 1. Scans all users for problematic content:// URIs in photoUrl field
 * 2. Removes or replaces them with safe alternatives
 * 3. Updates denormalized data in posts and comments
 */
@Singleton
class ContentUriCleanupUseCase @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val logger: Logger
) {
    
    companion object {
        private const val TAG = "ContentUriCleanup"
    }
    
    /**
     * Clean up all problematic content URIs in the users collection
     */
    suspend fun cleanupAllProblematicUris(): Result<Int> {
        return try {
            logger.i(TAG, "🧹 Starting cleanup of problematic content URIs...")
            
            // Get all users
            val usersSnapshot = firestore.collection("users").get().await()
            var cleanedCount = 0
            
            val batch = firestore.batch()
            
            for (userDoc in usersSnapshot.documents) {
                val photoUrl = userDoc.getString("photoUrl")
                
                if (!photoUrl.isNullOrBlank() && isProblematicContentUri(photoUrl)) {
                    logger.w(TAG, "🚫 Found problematic URI for user ${userDoc.id}: ${photoUrl.take(50)}...")
                    
                    // Clear the problematic URI
                    val userRef = firestore.collection("users").document(userDoc.id)
                    batch.update(userRef, "photoUrl", null)
                    
                    cleanedCount++
                }
            }
            
            if (cleanedCount > 0) {
                batch.commit().await()
                logger.i(TAG, "✅ Cleaned up $cleanedCount problematic content URIs")
            } else {
                logger.i(TAG, "✅ No problematic content URIs found")
            }
            
            Result.success(cleanedCount)
        } catch (e: Exception) {
            logger.e(TAG, "❌ Error during cleanup", e)
            Result.failure(e)
        }
    }
    
    /**
     * Clean up problematic content URIs for a specific user
     */
    suspend fun cleanupUserProblematicUri(userId: String): Result<Boolean> {
        return try {
            logger.d(TAG, "🧹 Cleaning up problematic URI for user: $userId")
            
            val userDoc = firestore.collection("users").document(userId).get().await()
            val photoUrl = userDoc.getString("photoUrl")
            
            if (!photoUrl.isNullOrBlank() && isProblematicContentUri(photoUrl)) {
                logger.w(TAG, "🚫 Found problematic URI: ${photoUrl.take(50)}...")
                
                // Clear the problematic URI
                val updates = mapOf(
                    "photoUrl" to null,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                
                firestore.collection("users").document(userId)
                    .update(updates)
                    .await()
                
                logger.i(TAG, "✅ Cleaned up problematic URI for user: $userId")
                Result.success(true)
            } else {
                logger.d(TAG, "✅ No problematic URI found for user: $userId")
                Result.success(false)
            }
        } catch (e: Exception) {
            logger.e(TAG, "❌ Error cleaning up URI for user: $userId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if a URI is a problematic content:// URI
     */
    private fun isProblematicContentUri(uri: String): Boolean {
        return uri.startsWith("content://media/picker") ||
               uri.startsWith("content://com.android.providers.media.photopicker") ||
               (uri.startsWith("content://") && 
                !uri.startsWith("https://") &&
                !uri.startsWith("http://"))
    }
}
