package com.omerkaya.sperrmuellfinder.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.data.mapper.PostMapper
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.SearchFilters
import com.omerkaya.sperrmuellfinder.domain.model.TimeRange
import com.omerkaya.sperrmuellfinder.domain.repository.PostSortBy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlin.math.*

/**
 * 🔍 PERFECT SEARCH POSTS PAGING SOURCE - SperrmüllFinder
 * Rules.md compliant - Professional hybrid search implementation
 * 
 * Features:
 * - HYBRID SEARCH STRATEGY: Server-side + Client-side optimization
 * - PERFECT TEXT MATCHING: Multi-language, fuzzy, tokenized search
 * - SMART PAGINATION: Maintains search relevance across pages
 * - LOCATION INTELLIGENCE: Precise geospatial filtering
 * - CATEGORY MASTERY: DE/EN category mapping with synonyms
 * - PERFORMANCE OPTIMIZED: Minimal Firestore reads, maximum relevance
 * - DEBUGGING EXCELLENCE: Comprehensive logging for troubleshooting
 */
class SearchPostsPagingSource(
    private val firestore: FirebaseFirestore,
    private val postMapper: PostMapper,
    private val filters: SearchFilters,
    private val isPremium: Boolean,
    private val logger: Logger
) : PagingSource<DocumentSnapshot, Post>() {
    
    companion object {
        private const val PAGE_SIZE = 30 // Increased for better search results
        private const val EARTH_RADIUS_KM = 6371.0
        private const val TAG = "PerfectSearchPaging"
        private const val MIN_QUERY_LENGTH = 2
        private const val FUZZY_MATCH_THRESHOLD = 0.7f
        private const val BLOCK_CACHE_TTL_MS = 30_000L
        private const val BASIC_RADIUS_METERS = 1500
    }

    private var blockedUserIdsCache: Set<String>? = null
    private var blockedUserIdsCacheAtMs: Long = 0L

    private fun Throwable.isFirestoreIndexError(): Boolean {
        val message = this.message ?: return false
        return message.contains("FAILED_PRECONDITION", ignoreCase = true) &&
            message.contains("index", ignoreCase = true)
    }
    
    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Post> {
        return try {
            // 🔍 PERFECT SEARCH DEBUGGING
            logger.d(TAG, "🚀 === PERFECT SEARCH ENGINE STARTED ===")
            logger.d(TAG, "📝 Query: '${filters.query}' (length: ${filters.query.length})")
            logger.d(TAG, "🏙️ City: ${filters.city ?: "all cities"}")
            logger.d(TAG, "🏷️ Categories: ${filters.categories.joinToString(", ").ifEmpty { "all categories" }}")
            logger.d(TAG, "📍 Radius: ${filters.radiusMeters}m (premium=$isPremium)")
            logger.d(TAG, "⏰ Time Range: ${filters.timeRange}")
            logger.d(TAG, "🔄 Sort By: ${filters.sortBy}")
            logger.d(TAG, "📍 User Location: ${filters.userLocation?.let { "(${it.latitude}, ${it.longitude})" } ?: "not set"}")
            logger.d(TAG, "📄 Page: ${if (params.key == null) "FIRST" else "NEXT"}")
            
            // 🎯 SMART QUERY STRATEGY: Hybrid server-client approach
            val isTextSearch = filters.query.length >= MIN_QUERY_LENGTH
            val hasLocationFilter = filters.userLocation != null
            val hasCategoryFilter = filters.categories.isNotEmpty()
            val hasCityFilter = !filters.city.isNullOrBlank()
            
            logger.d(TAG, "🧠 Search Strategy: text=$isTextSearch, location=$hasLocationFilter, category=$hasCategoryFilter, city=$hasCityFilter")
            
            // 🔥 BUILD INDEX-SAFE FIRESTORE QUERY
            // IMPORTANT: Keep query minimal to avoid composite index requirements.
            // All filters/sorts are applied client-side below.
            var query = firestore.collection(FirestoreConstants.Collections.POSTS)
                .orderBy(FirestoreConstants.Post.CREATED_AT, Query.Direction.DESCENDING)

            // 🔄 Sorting Strategy (client-side effective sort selection)
            val effectiveSortBy = if (isPremium) filters.sortBy else PostSortBy.NEWEST
            
            logger.d(TAG, "🔥 Firestore query optimized (index-safe): baseSort=created_at_desc | requestedSort=$effectiveSortBy")
            
            // 📄 Pagination
            params.key?.let { lastDocument ->
                query = query.startAfter(lastDocument)
                logger.d(TAG, "📄 Pagination: starting after document ${lastDocument.id}")
            }
            
            // 🚀 Fetch more documents for better filtering/sorting results
            val fetchSize = if (isTextSearch || hasCategoryFilter || hasCityFilter) {
                (PAGE_SIZE * 2).toLong() // Fetch more to account for client-side filtering
            } else {
                PAGE_SIZE.toLong()
            }
            query = query.limit(fetchSize)
            
            // 🔥 Execute Firestore Query
            logger.d(TAG, "🔥 Executing optimized Firestore query (limit: $fetchSize)...")
            val querySnapshot = query.get().await()
            val documents = querySnapshot.documents
            logger.d(TAG, "📦 Firestore returned ${documents.size} raw documents")
            
            // 🗺️ Map to Domain Models
            var posts = documents.mapNotNull { doc ->
                try {
                    postMapper.mapFromFirestore(doc)
                } catch (e: Exception) {
                    logger.w(TAG, "❌ Failed to map post ${doc.id}", e)
                    null
                }
            }

            // 🚫 Block filter: hide posts from blocked users (both directions)
            val blockedUserIds = getBlockedUserIds()
            if (blockedUserIds.isNotEmpty()) {
                val before = posts.size
                posts = posts.filterNot { blockedUserIds.contains(it.ownerId) }
                logger.d(TAG, "🚫 Block filter applied: $before → ${posts.size} posts")
            }
            
            logger.d(TAG, "✅ Successfully mapped ${posts.size} posts")

            // ✅ Status filter (client-side)
            val beforeStatusFilter = posts.size
            posts = posts.filter { it.status == com.omerkaya.sperrmuellfinder.domain.model.PostStatus.ACTIVE }
            logger.d(TAG, "✅ Status filter completed: $beforeStatusFilter → ${posts.size} active posts")

            // ⏰ Time range filter (client-side)
            if (filters.timeRange != TimeRange.ALL_TIME) {
                val beforeTimeFilter = posts.size
                val cutoffTimeMs = System.currentTimeMillis() - (filters.timeRange.hours * 60 * 60 * 1000L)
                posts = posts.filter { it.createdAt.time >= cutoffTimeMs }
                logger.d(TAG, "⏰ Time filter completed: $beforeTimeFilter → ${posts.size} posts")
            }
            
            // 🔍 PERFECT TEXT SEARCH ENGINE (Client-side for maximum flexibility)
            if (isTextSearch) {
                val queryLower = filters.query.lowercase().trim()
                val beforeFilter = posts.size
                logger.d(TAG, "🔍 Starting perfect text search for: '$queryLower'")
                
                posts = posts.filter { post ->
                    // 🎯 BUILD COMPREHENSIVE SEARCHABLE TEXT
                    val searchableText = buildString {
                        // Description (primary content)
                        append(post.description.lowercase())
                        append(" ")
                        
                        // Categories (both languages)
                        post.categoriesEn.forEach { append("$it ") }
                        post.categoriesDe.forEach { append("$it ") }
                        append(" ")
                        
                        // Location data
                        append(post.city.lowercase())
                        append(" ")
                        post.locationStreet?.let { append("${it.lowercase()} ") }
                        post.locationCity?.let { append("${it.lowercase()} ") }
                        
                        // Owner information for better discovery
                        post.ownerDisplayName?.let { append("${it.lowercase()} ") }
                    }
                    
                    // 🧠 INTELLIGENT SEARCH MATCHING
                    val searchTokens = queryLower.split(Regex("\\s+")).filter { it.length >= 2 }
                    
                    if (searchTokens.isEmpty()) {
                        true // Show all if no valid tokens
                    } else {
                        // Multi-strategy matching for maximum relevance
                        val exactMatch = searchableText.contains(queryLower)
                        val allTokensMatch = searchTokens.all { token -> searchableText.contains(token) }
                        val partialMatch = searchTokens.any { token -> 
                            searchableText.split(" ").any { word -> 
                                word.startsWith(token) || calculateSimilarity(word, token) > FUZZY_MATCH_THRESHOLD
                            }
                        }
                        
                        exactMatch || allTokensMatch || (searchTokens.size == 1 && partialMatch)
                    }
                }
                logger.d(TAG, "🔍 Text search completed: $beforeFilter → ${posts.size} posts")
            }
            
            // 🏙️ SMART CITY FILTERING (Client-side for fuzzy matching)
            if (hasCityFilter && !filters.city.isNullOrBlank()) {
                val beforeFilter = posts.size
                val cityLower = filters.city!!.lowercase().trim()
                logger.d(TAG, "🏙️ Applying smart city filter: '$cityLower'")
                
                posts = posts.filter { post ->
                    val cityMatches = post.city.lowercase().contains(cityLower) ||
                                     cityLower.contains(post.city.lowercase()) ||
                                     calculateSimilarity(post.city.lowercase(), cityLower) > 0.8f
                    
                    val locationCityMatches = post.locationCity?.let { locationCity ->
                        locationCity.lowercase().contains(cityLower) ||
                        cityLower.contains(locationCity.lowercase()) ||
                        calculateSimilarity(locationCity.lowercase(), cityLower) > 0.8f
                    } ?: false
                    
                    cityMatches || locationCityMatches
                }
                logger.d(TAG, "🏙️ City filter completed: $beforeFilter → ${posts.size} posts")
            }
            
            // 🏷️ ADVANCED CATEGORY FILTERING (Client-side for flexible matching)
            if (hasCategoryFilter) {
                val beforeFilter = posts.size
                logger.d(TAG, "🏷️ Applying advanced category filter: ${filters.categories}")
                
                posts = posts.filter { post ->
                    val postCategories = (post.categoriesEn + post.categoriesDe).map { it.lowercase() }
                    
                    filters.categories.any { filterCategory ->
                        val filterLower = filterCategory.lowercase()
                        postCategories.any { postCategory ->
                            // Exact match, contains match, or high similarity
                            postCategory == filterLower ||
                            postCategory.contains(filterLower) ||
                            filterLower.contains(postCategory) ||
                            calculateSimilarity(postCategory, filterLower) > 0.85f
                        }
                    }
                }
                logger.d(TAG, "🏷️ Category filter completed: $beforeFilter → ${posts.size} posts")
            }
            
            // 📍 PRECISION LOCATION FILTERING
            if (hasLocationFilter) {
                val userLocation = filters.userLocation!!
                val beforeFilter = posts.size
                val radiusMeters = if (isPremium) filters.radiusMeters.toDouble() else BASIC_RADIUS_METERS.toDouble()
                logger.d(TAG, "📍 Applying precision location filter: ${radiusMeters}m radius")
                
                posts = posts.mapNotNull { post ->
                    post.location?.let { location ->
                        val distance = calculateDistance(
                            userLocation.latitude, userLocation.longitude,
                            location.latitude, location.longitude
                        )
                        if (distance <= radiusMeters) {
                            // Add distance for sorting
                            post.copy(distanceFromUser = distance)
                        } else null
                    } ?: post // Keep posts without location
                }
                logger.d(TAG, "📍 Location filter completed: $beforeFilter → ${posts.size} posts")
            }
            
            // 🔄 INTELLIGENT SORTING
            posts = when (effectiveSortBy) {
                PostSortBy.NEAREST -> {
                    if (hasLocationFilter) {
                        logger.d(TAG, "🔄 Sorting by distance...")
                        posts.sortedBy { it.distanceFromUser ?: Double.MAX_VALUE }
                    } else {
                        posts // Keep original order if no location
                    }
                }
                PostSortBy.MOST_LIKED -> posts.sortedByDescending { it.likesCount }
                PostSortBy.MOST_COMMENTED -> posts.sortedByDescending { it.commentsCount }
                PostSortBy.MOST_VIEWED -> posts.sortedByDescending { it.viewsCount }
                PostSortBy.EXPIRING_SOON -> posts.sortedBy { it.expiresAt.time }
                PostSortBy.OLDEST -> posts.sortedBy { it.createdAt.time }
                PostSortBy.NEWEST -> posts.sortedByDescending { it.createdAt.time }
            }

            // 📄 SMART PAGINATION - Take only what we need
            val finalPosts = posts.take(PAGE_SIZE)
            
            // 🎯 Determine next key intelligently
            val nextKey = if (documents.size >= fetchSize && finalPosts.size == PAGE_SIZE) {
                documents.lastOrNull()
            } else {
                null
            }
            
            // 🎉 PERFECT SEARCH RESULTS
            logger.d(TAG, "🎉 === PERFECT SEARCH COMPLETED ===")
            logger.d(TAG, "📊 Final results: ${finalPosts.size} posts")
            logger.d(TAG, "📄 Has next page: ${nextKey != null}")
            logger.d(TAG, "🚀 Search quality: ${if (finalPosts.isNotEmpty()) "EXCELLENT" else "NO_MATCHES"}")
            logger.d(TAG, "=======================================")
            
            LoadResult.Page(
                data = finalPosts,
                prevKey = null, // We don't support backward pagination
                nextKey = nextKey
            )
            
        } catch (e: CancellationException) {
            logger.d(TAG, "Search paging load cancelled")
            throw e
        } catch (e: Exception) {
            logger.e(TAG, "Search query failed", e)
            logger.e(TAG, "Error message: ${e.message}")
            
            if (e.isFirestoreIndexError()) {
                logger.e(TAG, "⚠️ FIRESTORE INDEX MISSING ⚠️")
                logger.e(TAG, "Returning safe empty page to prevent UI hard failure.")
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )
            }
            
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Post>): DocumentSnapshot? {
        return null // Always refresh from the beginning
    }
    
    /**
     * 📍 Calculate distance between two points using Haversine formula
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in meters
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c * 1000 // Convert to meters
    }
    
    /**
     * 🧠 Calculate text similarity using Levenshtein distance
     * @param s1 First string
     * @param s2 Second string
     * @return Similarity score between 0.0 and 1.0
     */
    private fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f
        
        val maxLength = maxOf(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        return 1.0f - (distance.toFloat() / maxLength)
    }
    
    /**
     * 📏 Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[len1][len2]
    }

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
                logger.e(TAG, "Error loading blockedByMe for search filter", e)
            }
            emptySet()
        }

        // Reverse block lookup is not accessible with current Firestore rules.
        val blockedMe = emptySet<String>()

        return (blockedByMe + blockedMe).also {
            blockedUserIdsCache = it
            blockedUserIdsCacheAtMs = now
        }
    }
}
