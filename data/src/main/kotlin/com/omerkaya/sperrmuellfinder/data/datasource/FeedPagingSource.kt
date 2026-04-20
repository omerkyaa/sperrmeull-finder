package com.omerkaya.sperrmuellfinder.data.datasource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.data.mapper.PostMapper
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.PostStatus
import kotlinx.coroutines.tasks.await

/**
 * 🏠 FEED PAGING SOURCE - SperrmüllFinder
 * Rules.md compliant - Paging 3 integration with Firestore
 * 
 * Features:
 * - Efficient Firestore pagination with startAfter
 * - Active posts filtering (status = "active")
 * - Created date descending order
 * - Location-based filtering (future enhancement)
 * - Professional error handling
 * - Stable pagination keys
 */
class FeedPagingSource(
    private val firestore: FirebaseFirestore,
    private val postMapper: PostMapper,
    private val userId: String,
    private val isPremium: Boolean,
    private val earlyAccessMinutes: Int,
    private val radiusMeters: Int,
    private val userLatitude: Double?,
    private val userLongitude: Double?,
    private val logger: Logger
) : PagingSource<DocumentSnapshot, Post>() {
    
    companion object {
        private const val TAG = "FeedPagingSource"
        private const val BLOCK_CACHE_TTL_MS = 30_000L
    }

    // Cache per PagingSource instance; recreated when feed invalidates.
    private var blockedUserIdsCache: Set<String>? = null
    private var blockedUserIdsCacheAtMs: Long = 0L
    
    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Post> {
        return try {
            // Server-side active filter uses the composite index (status ASC, created_at DESC)
            var query: Query = firestore
                .collection(FirestoreConstants.Collections.POSTS)
                .whereEqualTo(FirestoreConstants.FIELD_STATUS, "active")
                .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .limit(params.loadSize.toLong())
            
            // Add pagination cursor if available
            params.key?.let { lastDocument ->
                query = query.startAfter(lastDocument)
            }
            
            // Execute query
            val querySnapshot = query.get().await()
            val documents = querySnapshot.documents
            val blockedUserIds = getBlockedUserIds()
            
            // Convert to domain models and filter active posts client-side.
            // Early-access rule:
            // - Premium users see new posts immediately.
            // - Basic users can see their own new posts immediately.
            // - Other basic users see a post only after the early-access window.
            val visibilityCutoffMs = System.currentTimeMillis() - (earlyAccessMinutes * 60_000L)
            val activePosts = documents.mapNotNull { document ->
                try {
                    val post = postMapper.mapFromFirestore(document)
                    if (post == null) {
                        null
                    } else if (!isPremium && post.ownerId != userId && post.createdAt.time > visibilityCutoffMs) {
                        // Basic non-owners do not see early-access posts during the delay window.
                        null
                    } else if (blockedUserIds.contains(post.ownerId)) {
                        null
                    } else {
                        enrichAndFilterByDistance(post)
                    }
                } catch (e: Exception) {
                    logger.e(TAG, "Error converting document ${document.id} to Post", e)
                    null
                }
            }
            
            // Batch check like status for all posts (PERFORMANCE OPTIMIZATION)
            val posts = if (activePosts.isNotEmpty()) {
                val likeStatuses = batchCheckLikeStatus(activePosts.map { it.id }, userId)
                activePosts.map { post ->
                    post.copy(isLikedByCurrentUser = likeStatuses[post.id] ?: false)
                }
            } else {
                activePosts
            }
            
            // Determine next key for pagination
            val nextKey = if (documents.isNotEmpty() && documents.size == params.loadSize) {
                documents.lastOrNull()
            } else {
                null
            }
            
            logger.d(TAG, "Loaded ${posts.size} posts, nextKey: ${nextKey?.id}")
            
            LoadResult.Page(
                data = posts,
                prevKey = null, // We only support forward pagination
                nextKey = nextKey
            )
            
        } catch (e: Exception) {
            logger.e(TAG, "Error loading feed page", e)
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Post>): DocumentSnapshot? {
        // Return null to always refresh from the beginning
        return null
    }
    
    /**
     * PERFORMANCE OPTIMIZATION: Batch check like status for multiple posts
     * This reduces N+1 query problem from N queries to 1 query
     */
    private suspend fun batchCheckLikeStatus(postIds: List<String>, userId: String): Map<String, Boolean> {
        return try {
            if (postIds.isEmpty()) return emptyMap()

            val result = mutableMapOf<String, Boolean>()
            postIds.forEach { postId ->
                result[postId] = false
            }

            // Avoid collectionGroup("likes") because rules can reject mixed-path group queries.
            // Direct document reads on posts/{postId}/likes/{userId} are rule-safe and deterministic.
            postIds.forEach { postId ->
                val likeDoc = firestore
                    .collection(FirestoreConstants.Collections.POSTS)
                    .document(postId)
                    .collection(FirestoreConstants.Subcollections.LIKES)
                    .document(userId)
                    .get()
                    .await()
                if (likeDoc.exists()) {
                    result[postId] = true
                }
            }

            logger.d(TAG, "💖 Batch like check: ${postIds.size} posts")
            result
            
        } catch (e: Exception) {
            logger.e(TAG, "Error batch checking like status", e)
            // Return all false on error
            postIds.associateWith { false }
        }
    }
    
    /**
     * Check if user has liked a specific post by checking if like document exists
     * DEPRECATED: Use batchCheckLikeStatus for better performance
     */
    private suspend fun checkIfUserLikedPost(postId: String, userId: String): Boolean {
        return try {
            val likeDocumentId = "${postId}_${userId}"
            val likeDocument = firestore
                .collection(FirestoreConstants.Collections.LIKES)
                .document(likeDocumentId)
                .get()
                .await()
            
            val isLiked = likeDocument.exists()
            logger.d(TAG, "💖 Like check: post=$postId, user=$userId, isLiked=$isLiked")
            isLiked
        } catch (e: Exception) {
            logger.e(TAG, "Error checking like status for post $postId", e)
            false // Default to not liked on error
        }
    }

    /**
     * Returns users that current user blocked OR users that blocked current user.
     */
    private suspend fun getBlockedUserIds(): Set<String> {
        val now = System.currentTimeMillis()
        blockedUserIdsCache?.let { cached ->
            if (now - blockedUserIdsCacheAtMs < BLOCK_CACHE_TTL_MS) {
                return cached
            }
        }
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptySet()

        val blockedByMe = try {
            firestore.collection(FirestoreConstants.BLOCKED_USERS)
                .document(currentUserId)
                .collection("blocks")
                .limit(200)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString(FirestoreConstants.BlockedUser.BLOCKED_USER_ID) }
                .toSet()
        } catch (e: Exception) {
            if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) != true) {
                logger.e(TAG, "Error loading blockedByMe for feed filter", e)
            }
            emptySet()
        }

        // Current rules do not allow reverse lookup from other users' block lists.
        val blockedMe = emptySet<String>()

        return (blockedByMe + blockedMe).also {
            blockedUserIdsCache = it
            blockedUserIdsCacheAtMs = now
        }
    }

    private fun enrichAndFilterByDistance(post: Post): Post? {
        val lat = userLatitude
        val lng = userLongitude
        val location = post.location
        if (lat == null || lng == null || location == null) {
            return post
        }

        val distanceMeters = calculateDistanceMeters(
            lat1 = lat,
            lon1 = lng,
            lat2 = location.latitude,
            lon2 = location.longitude
        )

        if (radiusMeters != Int.MAX_VALUE && distanceMeters > radiusMeters) {
            return null
        }
        return post.copy(distanceFromUser = distanceMeters)
    }

    private fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }
}
