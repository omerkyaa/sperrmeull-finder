package com.omerkaya.sperrmuellfinder.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.mapper.PostMapper
import com.omerkaya.sperrmuellfinder.data.mapper.UserMapper
import com.omerkaya.sperrmuellfinder.data.paging.SearchPostsPagingSource
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.SearchFilters
import com.omerkaya.sperrmuellfinder.domain.model.SearchHistory
import com.omerkaya.sperrmuellfinder.domain.model.SearchSuggestion
import com.omerkaya.sperrmuellfinder.domain.model.SearchSuggestionType
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.repository.PostSortBy
import com.omerkaya.sperrmuellfinder.domain.repository.SearchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🔍 SEARCH REPOSITORY IMPLEMENTATION - SperrmüllFinder
 * Rules.md compliant - Data layer Firestore implementation
 * 
 * Features:
 * - Multi-domain search with Firestore queries
 * - NO PREMIUM RESTRICTIONS - All users have full access
 * - Location-based filtering with GeoPoint queries
 * - Category filtering with EN/DE mapping
 * - Pagination with Paging 3
 * - Real-time suggestions and search history
 * - Comprehensive logging for debugging
 */
@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val postMapper: PostMapper,
    private val userMapper: UserMapper,
    private val logger: Logger
) : SearchRepository {
    private var blockedUsersCache: Set<String>? = null
    private var blockedUsersCacheAtMs: Long = 0L
    
    companion object {
        private const val SEARCH_PAGE_SIZE = 20
        private const val EARTH_RADIUS_KM = 6371.0
        private const val TAG = "SearchRepositoryImpl"
        private const val BLOCK_CACHE_TTL_MS = 30_000L
        private const val SEARCH_HISTORY_FIELD = "search_history"
        private const val SEARCH_HISTORY_MAX_ENTRIES = 30
    }
    
    override fun searchPosts(
        filters: SearchFilters,
        isPremium: Boolean
    ): Flow<PagingData<Post>> {
        logger.d(TAG, "searchPosts called - isPremium=$isPremium")
        return Pager(
            config = PagingConfig(
                pageSize = SEARCH_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                SearchPostsPagingSource(
                    firestore = firestore,
                    postMapper = postMapper,
                    filters = filters,
                    isPremium = isPremium,
                    logger = logger
                )
            }
        ).flow
    }
    
    override suspend fun searchUsers(
        query: String,
        limit: Int
    ): Result<List<User>> {
        return try {
            val normalizedQuery = query.trim().lowercase()
            val tokens = normalizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
            val blockedIds = getInteractionBlockedUserIds()
            val docsById = linkedMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()

            // Public profile search must use users_public; rules cap list queries at 50.
            val broadDocs = firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .limit(limit.coerceAtMost(50).toLong())
                .get()
                .await()
                .documents
            broadDocs.forEach { docsById[it.id] = it }

            // Prefix query by displayName for relevance.
            runCatching {
                firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                    .orderBy(FirestoreConstants.User.DISPLAY_NAME)
                    .startAt(normalizedQuery)
                    .endAt(normalizedQuery + '\uf8ff')
                    .limit(limit.coerceAtMost(50).toLong())
                    .get()
                    .await()
                    .documents
            }.getOrNull()?.forEach { docsById[it.id] = it }

            val users = docsById.values
                .mapNotNull { doc -> runCatching { userMapper.mapFromFirestore(doc) }.getOrNull() }
                .filterNot { user -> blockedIds.contains(user.uid) }
                .filter { user ->
                    if (tokens.isEmpty()) return@filter true
                    val searchable = buildString {
                        append(user.displayName.lowercase())
                        append(' ')
                        append((user.city ?: "").lowercase())
                        append(' ')
                        append(user.email.lowercase())
                    }
                    tokens.all { token -> searchable.contains(token) }
                }
                .sortedWith(
                    compareBy<User> { user ->
                        val name = user.displayName.lowercase()
                        when {
                            name == normalizedQuery -> 0
                            name.startsWith(normalizedQuery) -> 1
                            else -> 2
                        }
                    }.thenBy { it.displayName.lowercase() }
                )
                .take(limit)

            Result.Success(users)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeUsersSearch(
        query: String,
        limit: Int
    ): Flow<Result<List<User>>> = callbackFlow {
        val normalizedQuery = query.trim().lowercase()
        val tokens = normalizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
        val registrations = mutableListOf<ListenerRegistration>()
        val docsBySource = mutableMapOf<String, Map<String, com.google.firebase.firestore.DocumentSnapshot>>()

        fun emitMergedResult() {
            CoroutineScope(Dispatchers.IO).launch {
                val blockedIds = getInteractionBlockedUserIds()
                val mergedDocs = linkedMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()
                docsBySource.values.forEach { sourceDocs ->
                    sourceDocs.forEach { (id, doc) -> mergedDocs[id] = doc }
                }

                val users = mergedDocs.values
                    .mapNotNull { doc -> runCatching { userMapper.mapFromFirestore(doc) }.getOrNull() }
                    .filterNot { blockedIds.contains(it.uid) }
                    .filter { user ->
                        if (tokens.isEmpty()) return@filter true
                        val searchable = buildString {
                            append(user.displayName.lowercase())
                            append(' ')
                            append((user.city ?: "").lowercase())
                            append(' ')
                            append(user.email.lowercase())
                        }
                        tokens.all { token -> searchable.contains(token) }
                    }
                    .sortedWith(
                        compareBy<User> { user ->
                            val name = user.displayName.lowercase()
                            when {
                                name == normalizedQuery -> 0
                                name.startsWith(normalizedQuery) -> 1
                                else -> 2
                            }
                        }.thenBy { it.displayName.lowercase() }
                    )
                    .take(limit)

                trySend(Result.Success(users))
            }
        }

        // Broad live source: keeps profile edits live and avoids missing users with inconsistent fields.
        val broadRegistration = firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
            .limit(limit.coerceAtMost(50).toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(TAG, "Error observing users (broad source)", error)
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents.orEmpty().associateBy { it.id }
                docsBySource["broad"] = docs
                emitMergedResult()
            }
        registrations.add(broadRegistration)

        // Prefix live source for relevance ordering.
        val prefixRegistration = firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
            .orderBy(FirestoreConstants.User.DISPLAY_NAME)
            .startAt(normalizedQuery)
            .endAt(normalizedQuery + '\uf8ff')
            .limit(limit.coerceAtMost(50).toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(TAG, "Error observing users (prefix source)", error)
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents.orEmpty().associateBy { it.id }
                docsBySource["prefix"] = docs
                emitMergedResult()
            }
        registrations.add(prefixRegistration)

        awaitClose { registrations.forEach { it.remove() } }
    }

    private suspend fun getInteractionBlockedUserIds(): Set<String> {
        val now = System.currentTimeMillis()
        blockedUsersCache?.let { cached ->
            if (now - blockedUsersCacheAtMs < BLOCK_CACHE_TTL_MS) {
                return cached
            }
        }

        val currentUserId = auth.currentUser?.uid ?: return emptySet()

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
                logger.e(TAG, "Error loading blockedByMe users for search", e)
            }
            emptySet()
        }

        // Reverse lookup is not readable under current rules.
        val blockedMe = emptySet<String>()

        return (blockedByMe + blockedMe).also {
            blockedUsersCache = it
            blockedUsersCacheAtMs = now
        }
    }
    
    override suspend fun getSearchSuggestions(
        query: String,
        limit: Int
    ): Result<List<SearchSuggestion>> {
        return try {
            val suggestions = mutableListOf<SearchSuggestion>()
            val queryLower = query.lowercase().trim()
            
            // Category suggestions (German and English)
            val categoriesResult = getPopularCategories("de")
            if (categoriesResult is Result.Success) {
                val categories: List<String> = categoriesResult.data
                categories.filter { it.lowercase().contains(queryLower) }
                    .take(3)
                    .forEach { category: String ->
                        suggestions.add(
                            SearchSuggestion(
                                text = category,
                                type = SearchSuggestionType.CATEGORY,
                                count = 0 // Could be populated with actual count
                            )
                        )
                    }
            }
            
            // English category suggestions for broader search
            val englishCategoriesResult = getPopularCategories("en")
            if (englishCategoriesResult is Result.Success) {
                val englishCategories: List<String> = englishCategoriesResult.data
                englishCategories.filter { it.lowercase().contains(queryLower) }
                    .take(2) // Fewer English suggestions
                    .forEach { category: String ->
                        suggestions.add(
                            SearchSuggestion(
                                text = category,
                                type = SearchSuggestionType.CATEGORY,
                                count = 0
                            )
                        )
                    }
            }
            
            // City suggestions
            val citiesResult = getGermanCities(queryLower)
            if (citiesResult is Result.Success) {
                val cities: List<String> = citiesResult.data
                cities.take(3).forEach { city: String ->
                    suggestions.add(
                        SearchSuggestion(
                            text = city,
                            type = SearchSuggestionType.CITY,
                            count = 0 // Could be populated with actual count
                        )
                    )
                }
            }
            
            // User suggestions (top 2)
            val usersResult = searchUsers(query, 2)
            if (usersResult is Result.Success) {
                val users: List<User> = usersResult.data
                users.forEach { user: User ->
                    suggestions.add(
                        SearchSuggestion(
                            text = user.displayName,
                            type = SearchSuggestionType.USER,
                            count = 0
                        )
                    )
                }
            }
            
            Result.Success(suggestions.take(limit))
            
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getPopularCategories(language: String): Result<List<String>> {
        return try {
            val categories = if (language == "de") {
                FirestoreConstants.CATEGORIES_DE
            } else {
                FirestoreConstants.CATEGORIES_EN
            }
            Result.Success(categories)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Maps German category to English for backend queries
     */
    private fun mapCategoryToEnglish(germanCategory: String): String {
        return FirestoreConstants.CATEGORY_MAPPING[germanCategory] ?: germanCategory.lowercase()
    }
    
    /**
     * Maps list of categories (mixed DE/EN) to English for backend queries
     */
    private fun mapCategoriesToEnglish(categories: List<String>): List<String> {
        return categories.map { category ->
            FirestoreConstants.CATEGORY_MAPPING[category] ?: category.lowercase()
        }
    }
    
    override suspend fun getGermanCities(query: String?): Result<List<String>> {
        return try {
            val cities = com.omerkaya.sperrmuellfinder.domain.model.GermanCities.MAJOR_CITIES
            val filteredCities = if (query != null) {
                cities.filter { city ->
                    city.lowercase().contains(query.lowercase())
                }
            } else {
                cities
            }
            Result.Success(filteredCities)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun saveSearchHistory(
        query: String,
        filters: SearchFilters,
        resultCount: Int
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.Error(Exception("User not authenticated"))

            val historyEntry = mapOf(
                "id" to UUID.randomUUID().toString(),
                "query" to query.trim(),
                "filters" to mapOf(
                    "categories" to filters.categories,
                    "city" to filters.city,
                    "radiusMeters" to filters.radiusMeters,
                    "sortBy" to filters.sortBy.name
                ),
                "resultCount" to resultCount,
                "searchedAt" to Timestamp.now()
            )

            val userPrivateRef = firestore
                .collection(FirestoreConstants.Collections.USERS_PRIVATE)
                .document(currentUser.uid)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userPrivateRef)
                val existingRaw = snapshot.get(SEARCH_HISTORY_FIELD) as? List<*>
                val existing = existingRaw
                    ?.mapNotNull { it as? Map<String, Any?> }
                    ?.toMutableList()
                    ?: mutableListOf()

                val normalizedQuery = query.trim().lowercase()
                val deduped = existing.filterNot { entry ->
                    val existingQuery = (entry["query"] as? String)?.trim()?.lowercase()
                    val existingFilters = entry["filters"] as? Map<*, *>
                    val existingSortBy = existingFilters?.get("sortBy") as? String
                    existingQuery == normalizedQuery && existingSortBy == filters.sortBy.name
                }.toMutableList()

                deduped.add(0, historyEntry)
                val bounded = deduped.take(SEARCH_HISTORY_MAX_ENTRIES)

                transaction.set(
                    userPrivateRef,
                    mapOf(
                        SEARCH_HISTORY_FIELD to bounded,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }.await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getSearchHistory(limit: Int): Result<List<SearchHistory>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.Error(Exception("User not authenticated"))

            val userPrivateDoc = firestore.collection(FirestoreConstants.Collections.USERS_PRIVATE)
                .document(currentUser.uid)
                .get()
                .await()

            val historyEntries = (userPrivateDoc.get(SEARCH_HISTORY_FIELD) as? List<*>)
                ?.mapNotNull { it as? Map<*, *> }
                ?: emptyList()

            val history = historyEntries.mapNotNull { raw ->
                val data = raw.entries.associate { (key, value) -> key.toString() to value }
                val queryText = (data["query"] as? String)?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val filtersData = data["filters"] as? Map<*, *> ?: emptyMap<String, Any>()
                val sortByRaw = filtersData["sortBy"] as? String ?: PostSortBy.NEWEST.name
                val sortBy = runCatching { PostSortBy.valueOf(sortByRaw) }.getOrDefault(PostSortBy.NEWEST)
                val searchedAt = when (val value = data["searchedAt"]) {
                    is Timestamp -> value.toDate()
                    is Date -> value
                    is Number -> Date(value.toLong())
                    else -> Date()
                }

                SearchHistory(
                    id = data["id"] as? String ?: UUID.randomUUID().toString(),
                    query = queryText,
                    filters = SearchFilters(
                        query = queryText,
                        categories = (filtersData["categories"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        city = filtersData["city"] as? String,
                        radiusMeters = (filtersData["radiusMeters"] as? Number)?.toInt() ?: 1500,
                        sortBy = sortBy
                    ),
                    resultCount = (data["resultCount"] as? Number)?.toInt() ?: 0,
                    searchedAt = searchedAt
                )
            }
                .sortedByDescending { it.searchedAt.time }
                .take(limit.coerceAtLeast(0))

            Result.Success(history)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
