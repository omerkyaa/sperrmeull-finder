package com.omerkaya.sperrmuellfinder.domain.usecase.search

import androidx.paging.PagingData
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.SearchFilters
import com.omerkaya.sperrmuellfinder.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 📦 SEARCH POSTS USE CASE - SperrmüllFinder
 * Rules.md compliant - Domain layer use case
 * 
 * Features:
 * - Multi-criteria post search with pagination
 * - Premium radius restrictions enforcement
 * - Location and category filtering
 * - Sorting by date (newest/oldest first)
 */
class SearchPostsUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    
    /**
     * Execute post search with filters
     * @param filters Search filters configuration
     * @param userLocation User's current location for radius filtering
     * @return Flow of paginated post results
     */
    operator fun invoke(
        filters: SearchFilters,
        userLocation: PostLocation,
        isPremium: Boolean
    ): Flow<PagingData<Post>> {
        return searchRepository.searchPosts(
            filters = filters,
            isPremium = isPremium
        )
    }
}