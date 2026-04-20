package com.omerkaya.sperrmuellfinder.domain.usecase.search

import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 👥 SEARCH USERS USE CASE - SperrmüllFinder
 * Rules.md compliant - Domain layer use case
 * 
 * Features:
 * - Instagram-style user search
 * - Username and display name matching
 * - Partial query support ("ömer" -> all "ömer", "ömer kaya" -> exact match)
 */
class SearchUsersUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    
    /**
     * Execute user search
     * @param query Search query (username or display name)
     * @param limit Maximum results to return
     * @return Result with list of matching users
     */
    suspend operator fun invoke(
        query: String,
        limit: Int = 30
    ): Result<List<User>> {
        if (query.isBlank()) {
            return Result.Success(emptyList())
        }
        
        return searchRepository.searchUsers(
            query = query.trim(),
            limit = limit
        )
    }

    fun observe(
        query: String,
        limit: Int = 30
    ): Flow<Result<List<User>>> {
        return searchRepository.observeUsersSearch(
            query = query.trim(),
            limit = limit
        )
    }
}