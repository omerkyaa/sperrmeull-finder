package com.omerkaya.sperrmuellfinder.domain.repository

import androidx.paging.PagingData
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.SearchFilters
import com.omerkaya.sperrmuellfinder.domain.model.SearchSuggestion
import com.omerkaya.sperrmuellfinder.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * 🔍 SEARCH REPOSITORY INTERFACE - SperrmüllFinder
 * Rules.md compliant - Domain layer repository interface
 * 
 * Features:
 * - Multi-domain search (posts, users, locations, categories)
 * - Premium radius restrictions (Basic: 1.5km, Premium: unlimited)
 * - Real-time suggestions and debounced search
 * - Pagination support with Paging 3
 * - Location-based filtering with GeoHash/Haversine
 * - Category filtering with EN/DE mapping
 */
interface SearchRepository {
    
    /**
     * Search posts with comprehensive filtering
     * @param filters Search filters including query, location, categories, radius
     * @param isPremium Premium status for radius restrictions
     * @return Flow of paginated post results
     */
    fun searchPosts(
        filters: SearchFilters,
        isPremium: Boolean
    ): Flow<PagingData<Post>>
    
    /**
     * Search users by username and display name (Instagram-style)
     * @param query Search query (supports partial matching)
     * @param limit Maximum results to return
     * @return Result with list of matching users
     */
    suspend fun searchUsers(
        query: String,
        limit: Int = 30
    ): Result<List<User>>

    /**
     * Observe users search results in real-time from Firestore.
     * Results should reflect latest profile edits immediately.
     */
    fun observeUsersSearch(
        query: String,
        limit: Int = 30
    ): Flow<Result<List<User>>>
    
    /**
     * Get search suggestions based on query
     * @param query Partial search query
     * @param limit Maximum suggestions to return
     * @return Result with list of search suggestions
     */
    suspend fun getSearchSuggestions(
        query: String,
        limit: Int = 10
    ): Result<List<SearchSuggestion>>
    
    /**
     * Get popular categories for filter chips
     * @param language Language code (de/en)
     * @return Result with list of popular categories
     */
    suspend fun getPopularCategories(
        language: String = "de"
    ): Result<List<String>>
    
    /**
     * Get German cities for location filtering
     * @param query Optional query to filter cities
     * @return Result with list of matching cities
     */
    suspend fun getGermanCities(
        query: String? = null
    ): Result<List<String>>
    
    /**
     * Save search query to history (Premium feature)
     * @param query Search query
     * @param filters Applied filters
     * @param resultCount Number of results found
     */
    suspend fun saveSearchHistory(
        query: String,
        filters: SearchFilters,
        resultCount: Int
    ): Result<Unit>
    
    /**
     * Get user's search history (Premium feature)
     * @param limit Maximum history entries to return
     * @return Result with list of search history
     */
    suspend fun getSearchHistory(
        limit: Int = 10
    ): Result<List<com.omerkaya.sperrmuellfinder.domain.model.SearchHistory>>
}