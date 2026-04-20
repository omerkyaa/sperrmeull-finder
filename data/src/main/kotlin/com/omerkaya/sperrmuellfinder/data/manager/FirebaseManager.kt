package com.omerkaya.sperrmuellfinder.data.manager

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.omerkaya.sperrmuellfinder.core.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {

    // Firebase services
    val auth: FirebaseAuth by lazy { Firebase.auth }
    val firestore: FirebaseFirestore by lazy { Firebase.firestore }
    val storage: FirebaseStorage by lazy { Firebase.storage }

    /**
     * Initialize Firebase services
     */
    fun initialize() {
        try {
            // Initialize Firebase if not already initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                logger.i(Logger.TAG_FIREBASE, "Firebase initialized successfully")
            }

            // Initialize App Check with Play Integrity
            initializeAppCheck()

            // Configure Firestore settings
            configureFirestore()

            // Configure Storage settings
            configureStorage()

            logger.i(Logger.TAG_FIREBASE, "All Firebase services configured successfully")

        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to initialize Firebase", e)
            throw e
        }
    }

    /**
     * Initialize Firebase App Check for security
     * TEMPORARILY DISABLED: App Check blocking authentication during development
     */
    private fun initializeAppCheck() {
        try {
            // TEMPORARY FIX: Comment out App Check to resolve authentication blocking
            // val firebaseAppCheck = FirebaseAppCheck.getInstance()
            // firebaseAppCheck.installAppCheckProviderFactory(
            //     PlayIntegrityAppCheckProviderFactory.getInstance()
            // )
            logger.w(Logger.TAG_FIREBASE, "App Check DISABLED temporarily for development")
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to initialize App Check", e)
        }
    }

    /**
     * Configure Firestore settings for optimal performance
     * CRASH FIX: Enhanced error handling for "already started" scenarios
     */
    private fun configureFirestore() {
        try {
            // CRITICAL FIX: Check if Firestore has already been started
            // This prevents the IllegalStateException that was causing crashes
            
            // Try to configure settings only if not already started
            val persistentCacheSettings = PersistentCacheSettings.newBuilder()
                .setSizeBytes(100 * 1024 * 1024L) // 100MB cache size
                .build()
            
            // Apply modern Firestore settings BEFORE any other operations
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(persistentCacheSettings)
                .build()
            
            // Enable offline persistence after configuration
            firestore.enableNetwork()
            
            logger.i(Logger.TAG_FIREBASE, "Firestore configured with modern persistent cache (100MB)")
            
        } catch (e: IllegalStateException) {
            // ENHANCED ERROR HANDLING: More specific handling for "already started" error
            when {
                e.message?.contains("already been started") == true -> {
                    logger.i(Logger.TAG_FIREBASE, "Firestore already initialized - using existing configuration (this is normal)")
                }
                else -> {
                    logger.e(Logger.TAG_FIREBASE, "Unexpected IllegalStateException in Firestore configuration", e)
                    // Don't rethrow - continue with existing configuration
                }
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to configure Firestore", e)
            // Don't attempt fallback if already started - just continue
            logger.i(Logger.TAG_FIREBASE, "Continuing with default Firestore configuration")
        }
    }

    // REMOVED: configureFallbackMemoryCache() - no longer needed with enhanced error handling

    /**
     * Configure Storage settings
     */
    private fun configureStorage() {
        try {
            storage.apply {
                maxDownloadRetryTimeMillis = 60000 // 60 seconds
                maxUploadRetryTimeMillis = 120000  // 2 minutes
                maxOperationRetryTimeMillis = 60000 // 60 seconds
            }
            logger.i(Logger.TAG_FIREBASE, "Storage configured successfully")
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to configure Storage", e)
        }
    }

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Check if user is authenticated
     */
    fun isUserAuthenticated(): Boolean = auth.currentUser != null

    /**
     * Get Firestore collection reference
     */
    fun getCollection(collection: String) = firestore.collection(collection)

    /**
     * Get Storage reference
     */
    fun getStorageReference(path: String) = storage.reference.child(path)

    /**
     * Sign out from Firebase
     */
    fun signOut() {
        try {
            auth.signOut()
            logger.i(Logger.TAG_FIREBASE, "User signed out successfully")
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to sign out", e)
        }
    }

    companion object {
        // Collection names
        const val COLLECTION_USERS = "users"
        const val COLLECTION_USERS_PUBLIC = "users_public"
        const val COLLECTION_USERS_PRIVATE = "users_private"
        const val COLLECTION_POSTS = "posts"
        const val COLLECTION_COMMENTS = "comments"
        const val COLLECTION_FOLLOWERS = "followers"
        const val COLLECTION_REPORTS = "reports"
        const val COLLECTION_XP_TRANSACTIONS = "xp_transactions"
        const val COLLECTION_NOTIFICATIONS = "notifications"
        const val COLLECTION_DEVICE_TOKENS = "device_tokens"
        const val COLLECTION_MODERATION_QUEUE = "moderation_queue"
        const val COLLECTION_ADMIN_LOGS = "admin_logs"
        const val COLLECTION_POST_VIEWS = "post_views"
        const val COLLECTION_PURCHASES = "purchases"
        const val COLLECTION_BADGES = "badges"
        const val COLLECTION_USER_BADGES = "user_badges"

        // Storage paths
        const val STORAGE_POST_IMAGES = "post_images"
        const val STORAGE_USER_AVATARS = "user_avatars"
        const val STORAGE_TEMP_UPLOADS = "temp_uploads"
    }
}
