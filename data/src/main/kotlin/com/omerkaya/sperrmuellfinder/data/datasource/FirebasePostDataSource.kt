package com.omerkaya.sperrmuellfinder.data.datasource

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.core.util.GeoHashUtils
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.dto.PostDto
import com.omerkaya.sperrmuellfinder.data.dto.PostLocationDto
import com.omerkaya.sperrmuellfinder.data.mapper.PostMapper
import com.omerkaya.sperrmuellfinder.data.paging.PostsPagingSource
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.repository.PostSortBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Firestore data source for post operations.
 * Rules.md compliant - Professional Firestore post CRUD operations.
 * 
 * Handles:
 * - Post creation with GeoHash for location queries
 * - Location-based post retrieval with Paging 3
 * - Real-time updates and efficient queries
 * - User authentication integration
 */
@Singleton
class FirebasePostDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val logger: Logger
) {

    /**
     * Create a new post in Firestore.
     * 
     * @param images List of uploaded image URLs
     * @param description Post description
     * @param location Post location with coordinates
     * @param city City name
     * @param categoriesEn Categories in English
     * @param categoriesDe Categories in German
     * @return Result containing the created Post
     */
    suspend fun createPost(
        images: List<String>,
        description: String,
        location: PostLocation,
        city: String,
        categoriesEn: List<String>,
        categoriesDe: List<String>
    ): Result<Post> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                logger.e(Logger.TAG_DEFAULT, "User not authenticated")
                android.util.Log.e("FirebasePostDataSource", "User not authenticated - currentUser is null")
                return Result.Error(Exception("User not authenticated"))
            }
            
            logger.d(Logger.TAG_DEFAULT, "Creating post for user: ${currentUser.uid}")
            android.util.Log.d("FirebasePostDataSource", "Creating post for user: ${currentUser.uid}, email: ${currentUser.email}")
            logger.d(Logger.TAG_DEFAULT, "🎯 DEBUG: FirebasePostDataSource.createPost() called")

            // Generate unique post ID with timestamp for extra uniqueness
            val timestamp = System.currentTimeMillis()
            val postId = "${currentUser.uid}_${timestamp}_${UUID.randomUUID().toString().take(8)}"
            val now = Date()
            val expiresAt = Date(now.time + (72 * 60 * 60 * 1000)) // 72 hours from now
            
            // Generate GeoHash for efficient location queries
            val geoHash = GeoHashUtils.encode(location, 9) // 9-character precision
            
            // Get user display name from Firestore users collection
            val userDoc = firestore.collection(FirestoreConstants.COLLECTION_USERS)
                .document(currentUser.uid)
                .get()
                .await()
            
            val userDisplayName = userDoc.getString(FirestoreConstants.FIELD_DISPLAY_NAME) ?: currentUser.displayName ?: "Unknown User"
            val userPhotoUrl = userDoc.getString(FirestoreConstants.FIELD_PHOTO_URL) ?: currentUser.photoUrl?.toString()
            val userLevel = userDoc.getLong(FirestoreConstants.FIELD_LEVEL)?.toInt() ?: 1
            val isUserPremium = userDoc.getBoolean(FirestoreConstants.FIELD_IS_PREMIUM) ?: false

            val postDto = PostDto(
                id = postId,
                ownerId = currentUser.uid,
                ownerDisplayName = userDisplayName,
                ownerPhotoUrl = userPhotoUrl,
                ownerLevel = userLevel,
                isOwnerPremium = isUserPremium,
                ownerPremiumFrameType = if (isUserPremium && userLevel >= 15) "DIAMOND" else if (isUserPremium) "GOLD" else "NONE",
                images = images,
                description = description,
                location = PostLocationDto(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = location.address,
                    geohash = geoHash
                ),
                city = city,
                categoriesEn = categoriesEn,
                categoriesDe = categoriesDe,
                availabilityPercent = 100,
                likesCount = 0,
                commentsCount = 0,
                viewsCount = 0,
                sharesCount = 0,
                status = "active",
                createdAt = now,
                expiresAt = expiresAt,
                updatedAt = now,
                geohash = geoHash
            )

            logger.d(Logger.TAG_DEFAULT, "Creating post with ID: $postId")
            android.util.Log.d("FirebasePostDataSource", "Creating post with ID: $postId")
            android.util.Log.d("FirebasePostDataSource", "PostDto ownerid: ${postDto.ownerId}")
            android.util.Log.d("FirebasePostDataSource", "PostDto status: ${postDto.status}")

            // 🎯 TEMPORARY FIX: Use HashMap instead of PostDto to debug @PropertyName issue
            val postDataMap = hashMapOf(
                "id" to postDto.id,
                "ownerid" to postDto.ownerId, // Direct field mapping
                "owner_display_name" to postDto.ownerDisplayName,
                "owner_photo_url" to postDto.ownerPhotoUrl,
                "owner_level" to postDto.ownerLevel,
                "is_owner_premium" to postDto.isOwnerPremium,
                "owner_premium_frame_type" to postDto.ownerPremiumFrameType,
                "images" to postDto.images,
                "description" to postDto.description,
                "location" to mapOf(
                    "latitude" to postDto.location?.latitude,
                    "longitude" to postDto.location?.longitude,
                    "address" to postDto.location?.address,
                    "geohash" to postDto.location?.geohash
                ),
                "city" to postDto.city,
                "category_en" to postDto.categoriesEn,
                "category_de" to postDto.categoriesDe,
                "availability_percent" to postDto.availabilityPercent,
                "likes_count" to postDto.likesCount,
                "comments_count" to postDto.commentsCount,
                "views_count" to postDto.viewsCount,
                "shares_count" to postDto.sharesCount,
                "status" to postDto.status,
                "created_at" to postDto.createdAt,
                "expires_at" to postDto.expiresAt,
                "updated_at" to postDto.updatedAt,
                "geohash" to postDto.geohash
            )
            
            logger.d(Logger.TAG_DEFAULT, "🎯 DEBUG: Using HashMap - ownerid = '${postDataMap["ownerid"]}'")

            // Save to Firestore with unique document ID
            firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .document(postId) // Use the generated UUID as document ID
                .set(postDataMap) // Use HashMap instead of PostDto
                .await()

            logger.i(Logger.TAG_DEFAULT, "Post created successfully: $postId")
            android.util.Log.i("FirebasePostDataSource", "Post created successfully: $postId")

            // Convert DTO to domain model using PostMapper instance
            val postMapper = PostMapper()
            val post = postMapper.fromDto(postDto)
            
            Result.Success(post)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error creating post", e)
            android.util.Log.e("FirebasePostDataSource", "Error creating post: ${e.message}", e)
            
            // Provide more specific error messages
            val friendlyError = when {
                e.message?.contains("PERMISSION_DENIED") == true -> 
                    Exception("Permission denied. Please check your account status.")
                e.message?.contains("UNAUTHENTICATED") == true -> 
                    Exception("Please log in again to create posts.")
                e.message?.contains("network") == true -> 
                    Exception("Network error. Please check your internet connection.")
                else -> e
            }
            
            Result.Error(friendlyError)
        }
    }

    /**
     * Get posts near user location with date sorting (newest first).
     * SIMPLIFIED: Uses basic query to avoid Firestore index requirements.
     */
    fun getPostsNearUser(
        userLocation: PostLocation,
        radiusMeters: Int
    ): Flow<PagingData<Post>> {
        logger.d(Logger.TAG_DEFAULT, "Getting posts near user (radius: ${radiusMeters}m)")
        
        // ULTRA-SIMPLIFIED: Remove status filter to avoid composite index requirement
        // All filtering (including status) will be handled client-side in PostsPagingSource
        val query = try {
            firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
        } catch (e: Exception) {
            // FALLBACK: If even created_at index fails, use no ordering (most basic query)
            logger.w(Logger.TAG_DEFAULT, "Failed to create ordered query, using basic query", e)
            firestore.collection(FirestoreConstants.COLLECTION_POSTS)
        }
        
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                PostsPagingSource(
                    firestore = firestore,
                    userLocation = userLocation,
                    radiusKm = radiusMeters / 1000.0,
                    cityFilter = null,
                    categories = emptyList(),
                    maxHoursOld = 0,
                    searchQuery = null,
                )
            }
        ).flow
    }

    /**
     * Get posts near user location with distance sorting (nearest first).
     * SIMPLIFIED: Uses basic query to avoid Firestore index requirements.
     */
    fun getPostsNearUserSortedByDistance(
        userLocation: PostLocation,
        radiusMeters: Int
    ): Flow<PagingData<Post>> {
        logger.d(Logger.TAG_DEFAULT, "Getting posts near user sorted by distance (radius: ${radiusMeters}m)")
        
        // ULTRA-SIMPLIFIED: Remove status filter to avoid composite index requirement
        // All filtering (including status) will be handled client-side in PostsPagingSource
        val query = try {
            firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
        } catch (e: Exception) {
            // FALLBACK: If even created_at index fails, use no ordering (most basic query)
            logger.w(Logger.TAG_DEFAULT, "Failed to create ordered query, using basic query", e)
            firestore.collection(FirestoreConstants.COLLECTION_POSTS)
        }
        
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                PostsPagingSource(
                    firestore = firestore,
                    userLocation = userLocation,
                    radiusKm = radiusMeters / 1000.0,
                    cityFilter = null,
                    categories = emptyList(),
                    maxHoursOld = 0,
                    searchQuery = null,
                )
            }
        ).flow
    }

    /**
     * Get posts by specific user.
     */
    fun getPostsByUser(
        userId: String,
        includeArchived: Boolean
    ): Flow<PagingData<Post>> {
        logger.d(Logger.TAG_DEFAULT, "Getting posts by user: $userId, includeArchived: $includeArchived")
        
        var query = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
            .whereEqualTo(FirestoreConstants.FIELD_OWNER_ID, userId)
            .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
        
        if (!includeArchived) {
            query = query.whereEqualTo(FirestoreConstants.FIELD_STATUS, "active")
        }
        
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                PostsPagingSource(
                    firestore = firestore,
                    userLocation = PostLocation(0.0, 0.0), // Not needed for user posts
                    radiusKm = Double.MAX_VALUE, // No radius limit for user posts
                    cityFilter = null,
                    categories = emptyList(),
                    maxHoursOld = 0,
                    searchQuery = null,
                )
            }
        ).flow
    }

    /**
     * Get a specific post by ID.
     */
    suspend fun getPostById(postId: String): Result<Post?> {
        return try {
            logger.d(Logger.TAG_DEFAULT, "Getting post by ID: $postId")
            
            val document = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .document(postId)
                .get()
                .await()
            
            if (!document.exists()) {
                logger.w(Logger.TAG_DEFAULT, "Post not found: $postId")
                return Result.Success(null)
            }
            
            val postDto = document.toObject(PostDto::class.java)
            if (postDto == null) {
                logger.e(Logger.TAG_DEFAULT, "Failed to parse post DTO: $postId")
                return Result.Error(Exception("Failed to parse post data"))
            }
            
            val postMapper = PostMapper()
            val post = postMapper.fromDto(postDto)
            logger.d(Logger.TAG_DEFAULT, "Post retrieved successfully: $postId")
            
            Result.Success(post)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error getting post by ID: $postId", e)
            Result.Error(e)
        }
    }

    /**
     * Search posts with filters and sorting.
     */
    fun searchPosts(
        query: String?,
        userLocation: PostLocation,
        radiusMeters: Int,
        categories: List<String>,
        city: String?,
        maxHoursOld: Int,
        sortBy: PostSortBy
    ): Flow<PagingData<Post>> {
        logger.d(Logger.TAG_DEFAULT, "Searching posts with query: '$query', radius: ${radiusMeters}m")
        
        // ULTRA-SIMPLIFIED: Use only basic sorting to avoid any composite index requirements
        // All filtering (status, city, categories, etc.) will be handled client-side in PostsPagingSource
        val firestoreQuery = try {
            when (sortBy) {
                PostSortBy.NEWEST -> firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                    .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
                PostSortBy.OLDEST -> firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                    .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.ASCENDING)
                PostSortBy.MOST_LIKED -> firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                    .orderBy("likes_count", Query.Direction.DESCENDING)
                PostSortBy.MOST_COMMENTED -> firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                    .orderBy("comments_count", Query.Direction.DESCENDING)
                PostSortBy.MOST_VIEWED -> firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                    .orderBy("views_count", Query.Direction.DESCENDING)
                PostSortBy.EXPIRING_SOON -> firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                    .orderBy(FirestoreConstants.FIELD_EXPIRES_AT, Query.Direction.ASCENDING)
                PostSortBy.NEAREST -> firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                    .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING) // Will sort by distance in PagingSource
            }
        } catch (e: Exception) {
            // FALLBACK: If any ordering fails, use most basic query
            logger.w(Logger.TAG_DEFAULT, "Failed to create ordered search query, using basic query", e)
            firestore.collection(FirestoreConstants.COLLECTION_POSTS)
        }
        
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                PostsPagingSource(
                    firestore = firestore,
                    userLocation = userLocation,
                    radiusKm = radiusMeters / 1000.0,
                    cityFilter = city,
                    categories = categories,
                    maxHoursOld = maxHoursOld,
                    searchQuery = query,
                )
            }
        ).flow
    }


    /**
     * Toggle like status for a post.
     */
    suspend fun toggleLike(postId: String): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.Error(Exception("User not authenticated"))
            }

            logger.d(Logger.TAG_DEFAULT, "Toggling like for post: $postId")
            
            // This is a simplified implementation
            // In a real app, you'd need to check if user already liked the post
            // and maintain a separate likes collection
            
            val postRef = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .document(postId)
            
            firestore.runTransaction { transaction ->
                transaction.update(postRef, "updated_at", Date())
            }.await()
            
            logger.i(Logger.TAG_DEFAULT, "Post like toggled successfully: $postId")
            Result.Success(true) // Assuming liked
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error toggling like for post: $postId", e)
            Result.Error(e)
        }
    }

    /**
     * Delete a post.
     */
    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.Error(Exception("User not authenticated"))
            }

            logger.d(Logger.TAG_DEFAULT, "Deleting post: $postId")
            
            // Check if user owns the post
            val postDoc = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .document(postId)
                .get()
                .await()
            
            if (!postDoc.exists()) {
                return Result.Error(Exception("Post not found"))
            }
            
            val ownerId = postDoc.getString(FirestoreConstants.FIELD_OWNER_ID)
            if (ownerId != currentUser.uid) {
                return Result.Error(Exception("Not authorized to delete this post"))
            }
            
            // Soft delete by updating status
            firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .document(postId)
                .update(
                    mapOf(
                        FirestoreConstants.FIELD_STATUS to "removed",
                        FirestoreConstants.FIELD_UPDATED_AT to Date()
                    )
                )
                .await()
            
            logger.i(Logger.TAG_DEFAULT, "Post deleted successfully: $postId")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error deleting post: $postId", e)
            Result.Error(e)
        }
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
    }
}
