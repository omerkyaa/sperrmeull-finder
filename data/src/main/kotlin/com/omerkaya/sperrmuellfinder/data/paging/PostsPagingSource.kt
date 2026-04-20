package com.omerkaya.sperrmuellfinder.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.data.dto.PostDto
import com.omerkaya.sperrmuellfinder.data.mapper.PostMapper
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.Post
import kotlinx.coroutines.tasks.await
import kotlin.math.*

/**
 * PagingSource for loading posts from Firestore with client-side filtering
 */
class PostsPagingSource(
    private val firestore: FirebaseFirestore,
    private val userLocation: PostLocation,
    private val radiusKm: Double,
    private val cityFilter: String? = null,
    private val categories: List<String> = emptyList(),
    private val maxHoursOld: Int = 0,
    private val searchQuery: String? = null
) : PagingSource<QuerySnapshot, Post>() {

    override val keyReuseSupported: Boolean = true
    private var blockedUserIdsCache: Set<String>? = null

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Post> {
        return try {
            // Server-side active filter uses the composite index (status ASC, created_at DESC)
            val query = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .whereEqualTo(FirestoreConstants.FIELD_STATUS, "active")
                .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .limit(params.loadSize.toLong())

            // Apply cursor for pagination
            val snapshot = if (params.key != null) {
                query.startAfter(params.key!!.documents.lastOrNull()).get().await()
            } else {
                query.get().await()
            }

            val radiusMeters = radiusKm * 1000
            val blockedUserIds = getBlockedUserIds()

            val posts = snapshot.documents.mapNotNull { document ->
                try {
                    val postDto = document.toObject(PostDto::class.java) ?: return@mapNotNull null
                    
                    // Fix empty ID issue
                    val finalPostDto = if (postDto.id.isBlank()) {
                        postDto.copy(id = document.id)
                    } else {
                        postDto
                    }

                    val postMapper = PostMapper()
                    val post = postMapper.fromDto(finalPostDto)

                    // Hide posts from users current user blocked OR users who blocked current user
                    if (blockedUserIds.contains(post.ownerId)) {
                        return@mapNotNull null
                    }
                    
                    // Calculate distance (only for posts with valid location)
                    val distance = post.location?.let { location: PostLocation ->
                        calculateDistance(
                            userLocation.latitude, userLocation.longitude,
                            location.latitude, location.longitude
                        )
                    } ?: Double.MAX_VALUE // Posts without location get max distance

                    // 1. Filter by radius
                    if (distance > radiusMeters) {
                        return@mapNotNull null
                    }

                    // 2. Filter by city (if specified)
                    if (!cityFilter.isNullOrBlank() && !post.city.equals(cityFilter, ignoreCase = true)) {
                        return@mapNotNull null
                    }

                    // 3. Filter by categories (if specified)
                    if (categories.isNotEmpty()) {
                        val hasMatchingCategory = post.categoriesEn.any { category: String ->
                            categories.contains(category)
                        }
                        if (!hasMatchingCategory) {
                            return@mapNotNull null
                        }
                    }

                    // 4. Filter by time (if specified)
                    if (maxHoursOld > 0) {
                        val cutoffTime = System.currentTimeMillis() - (maxHoursOld * 60 * 60 * 1000)
                        if (post.createdAt.time < cutoffTime) {
                            return@mapNotNull null
                        }
                    }

                    // 5. Filter by search query (if specified)
                    if (!searchQuery.isNullOrBlank()) {
                        val queryLower = searchQuery.lowercase()
                        val matchesDescription = post.description.lowercase().contains(queryLower)
                        val matchesCity = post.city.lowercase().contains(queryLower)
                        val matchesCategories = post.categoriesDe.any { category: String -> 
                            category.lowercase().contains(queryLower) 
                        }

                        if (!matchesDescription && !matchesCity && !matchesCategories) {
                            return@mapNotNull null
                        }
                    }

                    // Return post with distance
                    post.copy(distanceFromUser = distance)
                } catch (e: Exception) {
                    null
                }
            }

            LoadResult.Page(
                data = posts,
                prevKey = null, // Only forward pagination
                nextKey = if (snapshot.documents.size >= params.loadSize) snapshot else null
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<QuerySnapshot, Post>): QuerySnapshot? {
        return null // Always start from the beginning on refresh
    }

    /**
     * Calculate distance between two points using Haversine formula
     */
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // Earth radius in meters

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    private suspend fun getBlockedUserIds(): Set<String> {
        blockedUserIdsCache?.let { return it }
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
        } catch (_: Exception) {
            emptySet()
        }

        // Reverse block lookup is not accessible with current Firestore rules.
        val blockedMe = emptySet<String>()

        return (blockedByMe + blockedMe).also { blockedUserIdsCache = it }
    }
}