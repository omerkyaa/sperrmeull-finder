package com.omerkaya.sperrmuellfinder.domain.usecase.search

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.SearchSuggestion
import com.omerkaya.sperrmuellfinder.domain.repository.SearchRepository
import javax.inject.Inject

/**
 * 💡 GET SEARCH SUGGESTIONS USE CASE - SperrmüllFinder
 * Rules.md compliant - Domain layer use case
 * 
 * Features:
 * - Real-time search suggestions
 * - Category, city, and user suggestions
 * - Debounced query processing
 */
class GetSearchSuggestionsUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    
    /**
     * Get search suggestions for query
     * @param query Partial search query
     * @param limit Maximum suggestions to return
     * @return Result with list of search suggestions
     */
    suspend operator fun invoke(
        query: String,
        limit: Int = 10
    ): Result<List<SearchSuggestion>> {
        if (query.isBlank() || query.length < 2) {
            return Result.Success(emptyList())
        }
        
        return searchRepository.getSearchSuggestions(
            query = query.trim(),
            limit = limit
        )
    }
}