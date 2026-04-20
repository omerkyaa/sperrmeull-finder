package com.omerkaya.sperrmuellfinder.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.data.mapper.toModerationQueueEntry
import com.omerkaya.sperrmuellfinder.domain.model.ModerationQueueEntry
import kotlinx.coroutines.tasks.await

/**
 * Paging source for moderation queue
 */
class ModerationQueuePagingSource(
    private val firestore: FirebaseFirestore,
    private val logger: Logger
) : PagingSource<QuerySnapshot, ModerationQueueEntry>() {
    
    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, ModerationQueueEntry> {
        return try {
            var query: Query = firestore.collection("moderation_queue")
                .whereEqualTo("status", "pending")
                .orderBy("priority", Query.Direction.DESCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(params.loadSize.toLong())
            
            // Handle pagination
            val currentPage = params.key
            if (currentPage != null) {
                val lastDocument = currentPage.documents.lastOrNull()
                if (lastDocument != null) {
                    query = query.startAfter(lastDocument)
                }
            }
            
            val snapshot = query.get().await()
            val items = snapshot.documents.mapNotNull { it.toModerationQueueEntry() }
            
            LoadResult.Page(
                data = items,
                prevKey = null,
                nextKey = if (snapshot.isEmpty) null else snapshot
            )
        } catch (e: Exception) {
            logger.e("ModerationQueuePagingSource", "Error loading moderation queue", e)
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<QuerySnapshot, ModerationQueueEntry>): QuerySnapshot? {
        return null
    }
}
