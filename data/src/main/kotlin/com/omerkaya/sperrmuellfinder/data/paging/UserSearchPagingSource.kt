package com.omerkaya.sperrmuellfinder.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.User
import kotlinx.coroutines.tasks.await

/**
 * Paging source for user search
 * TODO: Implement full User mapping
 */
class UserSearchPagingSource(
    private val firestore: FirebaseFirestore,
    private val query: String,
    private val logger: Logger
) : PagingSource<QuerySnapshot, User>() {
    
    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, User> {
        return try {
            // Search by displayname or email
            // Note: Firestore doesn't support full-text search, so we use prefix matching
            val searchQuery = query.lowercase()
            
            var firestoreQuery: Query = firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                .orderBy("displayname")
                .startAt(searchQuery)
                .endAt(searchQuery + "\uf8ff")
                .limit(params.loadSize.toLong().coerceAtMost(50L))
            
            // Handle pagination
            val currentPage = params.key
            if (currentPage != null) {
                val lastDocument = currentPage.documents.lastOrNull()
                if (lastDocument != null) {
                    firestoreQuery = firestoreQuery.startAfter(lastDocument)
                }
            }
            
            val snapshot = firestoreQuery.get().await()
            
            // TODO: Map documents to User domain model
            // For now, return empty list - will use existing UserMapper
            val users = emptyList<User>()
            
            LoadResult.Page(
                data = users,
                prevKey = null,
                nextKey = if (snapshot.isEmpty) null else snapshot
            )
        } catch (e: Exception) {
            logger.e("UserSearchPagingSource", "Error searching users", e)
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<QuerySnapshot, User>): QuerySnapshot? {
        return null
    }
}
