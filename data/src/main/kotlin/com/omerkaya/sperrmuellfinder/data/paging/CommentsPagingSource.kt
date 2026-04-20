package com.omerkaya.sperrmuellfinder.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.data.util.PublicUserResolver
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * PagingSource for loading comments from Firestore with pagination.
 * Supports real-time updates and efficient loading.
 * Rules.md compliant - Professional comment pagination system.
 */
class CommentsPagingSource(
    private val firestore: FirebaseFirestore,
    private val postId: String
) : PagingSource<DocumentSnapshot, Comment>() {
    private var blockedUserIdsCache: Set<String>? = null

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Comment> {
        return try {
            val pageSize = params.loadSize
            
            android.util.Log.d("CommentsPagingSource", "Loading comments for postId: $postId, pageSize: $pageSize")
            
            // Build query for comments
            var query: Query = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .document(postId)
                .collection(FirestoreConstants.SUBCOLLECTION_POST_COMMENTS)
                .orderBy(FirestoreConstants.Comment.CREATED_AT, Query.Direction.DESCENDING)
                .limit(pageSize.toLong())

            // Add pagination cursor if available
            params.key?.let { lastDocument ->
                query = query.startAfter(lastDocument)
            }

            // Execute query
            val querySnapshot = query.get().await()
            val documents = querySnapshot.documents
            val blockedUserIds = getBlockedUserIds()
            
            android.util.Log.d("CommentsPagingSource", "Query returned ${documents.size} documents")
            
            // Map documents to Comment objects
            val comments = documents.mapNotNull { document ->
                try {
                    val comment = mapDocumentToComment(document)
                    if (blockedUserIds.contains(comment.authorId)) {
                        return@mapNotNull null
                    }
                    android.util.Log.d("CommentsPagingSource", "Mapped comment: ${comment.id} - ${comment.content}")
                    comment
                } catch (e: Exception) {
                    android.util.Log.e("CommentsPagingSource", "Error mapping document ${document.id}", e)
                    // Skip invalid documents
                    null
                }
            }

            // Enrich comments with latest user profile data (displayName + photoUrl).
            val authorIds = comments.map { it.authorId }.filter { it.isNotBlank() }.distinct()
            val usersById = try {
                PublicUserResolver.resolveUsersByIds(firestore, authorIds)
            } catch (e: Exception) {
                android.util.Log.e("CommentsPagingSource", "Error enriching comments with user data", e)
                emptyMap()
            }

            val enrichedComments = comments.map { comment ->
                val latestUser = usersById[comment.authorId]
                if (latestUser != null) {
                    comment.copy(
                        authorName = latestUser.displayName.takeIf { it.isNotBlank() } ?: comment.authorName,
                        authorPhotoUrl = latestUser.photoUrl ?: comment.authorPhotoUrl,
                        authorCity = latestUser.city ?: comment.authorCity
                    )
                } else {
                    comment
                }
            }

            // Determine next key for pagination
            val nextKey = if (documents.size < pageSize) {
                null // No more pages
            } else {
                documents.lastOrNull()
            }

            android.util.Log.d("CommentsPagingSource", "Returning ${enrichedComments.size} comments, nextKey: ${nextKey != null}")
            
            LoadResult.Page(
                data = enrichedComments,
                prevKey = null, // We only support forward pagination for comments
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Comment>): DocumentSnapshot? {
        // Return null to always start from the beginning on refresh
        return null
    }

    /**
     * Maps Firestore document to Comment domain model.
     */
    private fun mapDocumentToComment(document: DocumentSnapshot): Comment {
        val timestamp = document.getTimestamp(FirestoreConstants.Comment.CREATED_AT)
        val updatedTimestamp = document.getTimestamp(FirestoreConstants.Comment.UPDATED_AT)
        
        return Comment(
            id = document.getString(FirestoreConstants.Comment.COMMENT_ID) ?: document.id,
            postId = document.getString(FirestoreConstants.Comment.POST_ID) ?: postId,
            authorId = document.getString(FirestoreConstants.Comment.AUTHOR_ID)
                ?: document.getString("author_id")
                ?: "",
            authorName = document.getString(FirestoreConstants.Comment.AUTHOR_NAME)
                ?: document.getString("author_name")
                ?: "Anonymous",
            authorPhotoUrl = document.getString(FirestoreConstants.Comment.AUTHOR_PHOTO_URL)
                ?: document.getString("author_photo_url"),
            authorLevel = document.getLong(FirestoreConstants.Comment.AUTHOR_LEVEL)?.toInt() ?: 1,
            content = document.getString(FirestoreConstants.Comment.CONTENT) ?: "",
            likesCount = document.getLong(FirestoreConstants.Comment.LIKES_COUNT)?.toInt() ?: 0,
            createdAt = timestamp?.toDate()
                ?: document.getTimestamp("createdAt")?.toDate()
                ?: Date(),
            updatedAt = updatedTimestamp?.toDate()
                ?: document.getTimestamp("updatedAt")?.toDate()
                ?: Date(),
            isLikedByCurrentUser = false, // TODO: Implement like status check
            authorCity = document.getString(FirestoreConstants.Comment.AUTHOR_CITY)
                ?: document.getString("author_city")
        )
    }

    private suspend fun getBlockedUserIds(): Set<String> {
        blockedUserIdsCache?.let { return it }
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
        } catch (_: Exception) {
            emptySet()
        }

        // Reverse block lookup is not accessible with current Firestore rules.
        val blockedMe = emptySet<String>()

        return (blockedByMe + blockedMe).also { blockedUserIdsCache = it }
    }
}
