package com.omerkaya.sperrmuellfinder.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.data.mapper.toAdminLog
import com.omerkaya.sperrmuellfinder.domain.model.AdminLog
import kotlinx.coroutines.tasks.await

/**
 * Paging source for admin logs
 */
class AdminLogsPagingSource(
    private val firestore: FirebaseFirestore,
    private val adminId: String?,
    private val action: String?,
    private val logger: Logger
) : PagingSource<QuerySnapshot, AdminLog>() {
    
    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, AdminLog> {
        return try {
            var query: Query = firestore.collection("admin_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
            
            // Apply filters
            if (adminId != null) {
                query = query.whereEqualTo("adminId", adminId)
            }
            if (action != null) {
                query = query.whereEqualTo("action", action)
            }
            
            query = query.limit(params.loadSize.toLong())
            
            // Handle pagination
            val currentPage = params.key
            if (currentPage != null) {
                val lastDocument = currentPage.documents.lastOrNull()
                if (lastDocument != null) {
                    query = query.startAfter(lastDocument)
                }
            }
            
            val snapshot = query.get().await()
            val logs = snapshot.documents.mapNotNull { it.toAdminLog() }
            
            LoadResult.Page(
                data = logs,
                prevKey = null, // Only forward pagination
                nextKey = if (snapshot.isEmpty) null else snapshot
            )
        } catch (e: Exception) {
            logger.e("AdminLogsPagingSource", "Error loading admin logs", e)
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<QuerySnapshot, AdminLog>): QuerySnapshot? {
        return null
    }
}
