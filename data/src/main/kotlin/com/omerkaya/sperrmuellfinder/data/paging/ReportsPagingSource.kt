package com.omerkaya.sperrmuellfinder.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.domain.model.ModerationPriority
import com.omerkaya.sperrmuellfinder.domain.model.Report
import com.omerkaya.sperrmuellfinder.domain.model.ReportStatus
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import com.omerkaya.sperrmuellfinder.data.mapper.toDomain
import kotlinx.coroutines.tasks.await

/**
 * Paging source for reports
 * TODO: Implement full Report mapping in Phase 2
 */
class ReportsPagingSource(
    private val firestore: FirebaseFirestore,
    private val status: ReportStatus?,
    private val priority: ModerationPriority?,
    private val type: ReportTargetType?,
    private val logger: Logger
) : PagingSource<QuerySnapshot, Report>() {
    
    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Report> {
        return try {
            // Build query with simpler filtering to avoid complex indexes
            // Priority: type > status > priority (only one filter active)
            var query: Query = firestore.collection("reports")
            
            when {
                // Type filter has highest priority
                type != null -> {
                    query = query
                        .whereEqualTo("type", type.name)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                }
                // Status filter second
                status != null -> {
                    query = query
                        .whereEqualTo("status", status.name)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                }
                // Priority filter third
                priority != null -> {
                    query = query
                        .whereEqualTo("priority", priority.name)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                }
                // No filter - just orderBy
                else -> {
                    query = query.orderBy("createdAt", Query.Direction.DESCENDING)
                }
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
            
            // Map documents to Report domain model
            val reports = snapshot.documents.mapNotNull { doc ->
                try {
                    val dto = doc.toObject(com.omerkaya.sperrmuellfinder.data.dto.ReportDto::class.java)
                    dto?.toDomain()
                } catch (e: Exception) {
                    logger.e("ReportsPagingSource", "Error mapping report: ${doc.id}", e)
                    null
                }
            }
            
            LoadResult.Page(
                data = reports,
                prevKey = null,
                nextKey = if (snapshot.isEmpty) null else snapshot
            )
        } catch (e: Exception) {
            logger.e("ReportsPagingSource", "Error loading reports", e)
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<QuerySnapshot, Report>): QuerySnapshot? {
        return null
    }
}
