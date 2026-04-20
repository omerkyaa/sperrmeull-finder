package com.omerkaya.sperrmuellfinder.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.core.util.FirebaseErrorHandler
import com.omerkaya.sperrmuellfinder.core.util.GooglePlayServicesChecker
import com.omerkaya.sperrmuellfinder.data.datasource.FirebasePostDataSource
import com.omerkaya.sperrmuellfinder.data.datasource.FirebaseStorageDataSource
import com.omerkaya.sperrmuellfinder.data.mapper.PostMapper
import com.omerkaya.sperrmuellfinder.data.util.PublicUserResolver
import com.omerkaya.sperrmuellfinder.data.paging.CommentsPagingSource
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.domain.model.Report
import com.omerkaya.sperrmuellfinder.domain.model.ReportReason
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import com.omerkaya.sperrmuellfinder.domain.repository.PostRepository
import com.omerkaya.sperrmuellfinder.domain.repository.PostSortBy
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val firebaseStorageDataSource: FirebaseStorageDataSource,
    private val firebasePostDataSource: FirebasePostDataSource,
    private val postMapper: PostMapper,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val logger: Logger,
    private val firebaseErrorHandler: FirebaseErrorHandler,
    private val googlePlayServicesChecker: GooglePlayServicesChecker,
    private val firestoreRepository: com.omerkaya.sperrmuellfinder.domain.repository.FirestoreRepository
) : PostRepository {

    // SharedFlow for broadcasting post creation events
    private val _postCreationEvents = MutableSharedFlow<Post>()
    override val postCreationEvents: Flow<Post> = _postCreationEvents.asSharedFlow()

    /**
     * Listen to all posts in real-time with proper ordering (newest first)
     * Uses addSnapshotListener for instant updates when posts change
     */
    override fun listenToPosts(): Flow<List<Post>> = callbackFlow {
        logger.d(Logger.TAG_DEFAULT, "Setting up real-time posts listener")
        
        val listener = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
            .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(Logger.TAG_DEFAULT, "Error listening to posts", error)
                    
                    // Professional error handling with graceful degradation
                    val errorResult = firebaseErrorHandler.handleFirebaseError(
                        exception = error,
                        operation = "Real-time posts listening",
                        onFallback = {
                            // Provide empty list as fallback instead of crashing
                            trySend(emptyList())
                        }
                    )
                    
                    // Handle specific error types
                    when (errorResult) {
                        is com.omerkaya.sperrmuellfinder.core.util.FirebaseErrorResult.IndexMissing -> {
                            logger.w(Logger.TAG_DEFAULT, "Firestore index missing - using fallback empty list")
                            trySend(emptyList())
                        }
                        is com.omerkaya.sperrmuellfinder.core.util.FirebaseErrorResult.SecurityError -> {
                            logger.w(Logger.TAG_DEFAULT, "Security error (likely GPS) - using fallback empty list")
                            trySend(emptyList())
                        }
                        else -> {
                            errorResult.onFallback?.invoke()
                        }
                    }
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    try {
                        val allPosts = snapshot.documents.mapNotNull { document ->
                            postMapper.mapFromFirestore(document)
                        }
                        
                        val activePosts = allPosts.mapNotNull { post ->
                            // Client-side filtering for active posts
                            if (post.status.name.lowercase() == "active") {
                                post.copy(isLikedByCurrentUser = false)
                            } else null
                        }
                        
                        logger.d(Logger.TAG_DEFAULT, "Real-time posts updated: ${activePosts.size} active posts out of ${allPosts.size} total")
                        trySend(activePosts)
                    } catch (e: Exception) {
                        logger.e(Logger.TAG_DEFAULT, "Error processing posts snapshot", e)
                        // Continue with empty list instead of crashing
                        trySend(emptyList())
                    }
                } else {
                    logger.w(Logger.TAG_DEFAULT, "Posts snapshot is null")
                    trySend(emptyList())
                }
            }
        
        awaitClose { 
            logger.d(Logger.TAG_DEFAULT, "Removing real-time posts listener")
            listener.remove() 
        }
    }

    override fun listenHomePosts(
        pageSize: Int,
        startAfter: Long?
    ): Flow<List<Post>> = callbackFlow {
        var listenerRegistration: ListenerRegistration? = null
        
        try {
            // TEMPORARY FIX: Use simple query without complex filtering to avoid index requirement
            // This will work immediately without needing Firebase Console index creation
            
            var query = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .orderBy(FirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .limit(pageSize.toLong())
            
            // Apply pagination if startAfter is provided
            if (startAfter != null) {
                query = query.startAfter(startAfter)
            }
            
            // Listen to posts collection with realtime updates
            listenerRegistration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Log the error for debugging
                    android.util.Log.e("PostRepository", "Firestore query failed", error)
                    
                    // Check if it's an index error
                    if (error.message?.contains("requires an index") == true) {
                        android.util.Log.w("PostRepository", "FIRESTORE INDEX MISSING: Using fallback query")
                        // Don't close the flow, just log the error
                        trySend(emptyList()) // Send empty list instead of crashing
                        return@addSnapshotListener
                    }
                    
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { document ->
                        postMapper.mapFromFirestore(document)?.let { post ->
                            // Client-side filtering for active posts (works without index)
                            if (post.status.name.lowercase() == "active") {
                                // Return the post with isLikedByCurrentUser = false initially
                                // UI layer will handle optimistic updates
                                post.copy(isLikedByCurrentUser = false)
                            } else null
                        }
                    }
                    
                    trySend(posts)
                }
            }
        } catch (e: Exception) {
            close(e)
        }
        
        awaitClose {
            listenerRegistration?.remove()
        }
    }

    override suspend fun toggleLike(postId: String, currentlyLiked: Boolean): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.Error(Exception("User not authenticated"))
            }

            val userId = currentUser.uid
            
            // Delegate to FirestoreRepository for actual implementation
            logger.d(Logger.TAG_DEFAULT, "Delegating like toggle to FirestoreRepository for post: $postId")
            
            firestoreRepository.toggleLike(postId, userId)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error in PostRepository toggleLike", e)
            Result.Error(e)
        }
    }

    override fun listenCommentsCount(postId: String): Flow<Int> = callbackFlow {
        var listenerRegistration: ListenerRegistration? = null
        
        try {
            // Listen to post document for commentsCount field
            listenerRegistration = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .document(postId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        val commentsCount = snapshot.getLong(FirestoreConstants.FIELD_COMMENTS_COUNT)?.toInt() ?: 0
                        trySend(commentsCount)
                    }
                }
        } catch (e: Exception) {
            close(e)
        }
        
        awaitClose {
            listenerRegistration?.remove()
        }
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }

    override suspend fun uploadImage(file: File): Result<String> {
        return firebaseStorageDataSource.uploadImage(file)
    }

    override fun getPostsNearUser(
        userLocation: PostLocation,
        radiusMeters: Int
    ): Flow<PagingData<Post>> {
        return firebasePostDataSource.getPostsNearUser(userLocation, radiusMeters)
    }

    override fun getPostsNearUserSortedByDate(
        userLocation: PostLocation,
        radiusMeters: Int
    ): Flow<PagingData<Post>> {
        return firebasePostDataSource.getPostsNearUser(userLocation, radiusMeters)
    }

    override fun getPostsNearUserSortedByDistance(
        userLocation: PostLocation,
        radiusMeters: Int
    ): Flow<PagingData<Post>> {
        return firebasePostDataSource.getPostsNearUserSortedByDistance(userLocation, radiusMeters)
    }

    override fun getPostsByUser(
        userId: String,
        includeArchived: Boolean
    ): Flow<PagingData<Post>> {
        return firebasePostDataSource.getPostsByUser(userId, includeArchived)
    }

    override suspend fun getPostById(postId: String): Result<Post?> {
        return firebasePostDataSource.getPostById(postId)
    }

    /**
     * Listen to a specific post in real-time with instant updates.
     * Uses addSnapshotListener for real-time updates when the post changes.
     */
    override fun listenToPost(postId: String): Flow<Result<Post?>> = callbackFlow {
        logger.d(Logger.TAG_DEFAULT, "Setting up real-time post listener for: $postId")
        
        // Emit loading state first
        trySend(Result.Loading)
        
        val listener = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
            .document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(Logger.TAG_DEFAULT, "Error listening to post: $postId", error)
                    
                    // Professional error handling
                    val errorResult = firebaseErrorHandler.handleFirebaseError(
                        exception = error,
                        operation = "Real-time post listening",
                        onFallback = {
                            trySend(Result.Error(error))
                        }
                    )
                    
                    when (errorResult) {
                        is com.omerkaya.sperrmuellfinder.core.util.FirebaseErrorResult.SecurityError -> {
                            logger.w(Logger.TAG_DEFAULT, "Security error for post: $postId")
                            trySend(Result.Error(error))
                        }
                        else -> {
                            errorResult.onFallback?.invoke()
                        }
                    }
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    try {
                        if (snapshot.exists()) {
                            val post = postMapper.mapFromFirestore(snapshot)
                            if (post != null) {
                                logger.d(Logger.TAG_DEFAULT, "Post updated: ${post.id}")
                                trySend(Result.Success(post))
                            } else {
                                logger.w(Logger.TAG_DEFAULT, "Failed to map post: $postId")
                                trySend(Result.Success(null))
                            }
                        } else {
                            logger.w(Logger.TAG_DEFAULT, "Post not found: $postId")
                            trySend(Result.Success(null))
                        }
                    } catch (e: Exception) {
                        logger.e(Logger.TAG_DEFAULT, "Error processing post snapshot: $postId", e)
                        trySend(Result.Error(e))
                    }
                } else {
                    logger.w(Logger.TAG_DEFAULT, "Null snapshot for post: $postId")
                    trySend(Result.Success(null))
                }
            }
        
        awaitClose {
            logger.d(Logger.TAG_DEFAULT, "Removing real-time post listener for: $postId")
            listener.remove()
        }
    }

    override suspend fun createPost(
        images: List<String>,
        description: String,
        location: PostLocation,
        city: String,
        categoriesEn: List<String>,
        categoriesDe: List<String>
    ): Result<Post> {
        return try {
            // Create post in Firestore
            val result = firebasePostDataSource.createPost(
                images = images,
                description = description,
                location = location,
                city = city,
                categoriesEn = categoriesEn,
                categoriesDe = categoriesDe
            )
            
            // Emit post creation event for real-time updates if successful
            if (result is Result.Success) {
                _postCreationEvents.emit(result.data)
            }
            
            result
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun toggleLike(postId: String): Result<Boolean> {
        return firebasePostDataSource.toggleLike(postId)
    }

    override fun getComments(postId: String): Flow<PagingData<Comment>> = callbackFlow {
        var commentsListener: ListenerRegistration? = null
        val userListeners = mutableListOf<ListenerRegistration>()
        var lastComments: List<Comment> = emptyList()

        suspend fun emitEnrichedComments(comments: List<Comment>) {
            val authorIds = comments.map { it.authorId }.filter { it.isNotBlank() }.distinct()
            val usersById = try {
                PublicUserResolver.resolveUsersByIds(firestore, authorIds)
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error enriching post comments with users", e)
                emptyMap()
            }

            val enriched = comments.map { comment ->
                val user = usersById[comment.authorId]
                if (user != null) {
                    comment.copy(
                        authorName = user.displayName.takeIf { it.isNotBlank() } ?: comment.authorName,
                        authorPhotoUrl = user.photoUrl ?: comment.authorPhotoUrl,
                        authorCity = user.city ?: comment.authorCity
                    )
                } else comment
            }

            trySend(PagingData.from(enriched))
        }

        fun refreshUserListeners(comments: List<Comment>) {
            userListeners.forEach { it.remove() }
            userListeners.clear()

            val authorIds = comments.map { it.authorId }.filter { it.isNotBlank() }.distinct()
            authorIds.chunked(10).forEach { chunk ->
                val docIdReg = firestore.collection(FirestoreConstants.Collections.USERS_PUBLIC)
                    .whereIn(FieldPath.documentId(), chunk)
                    .limit(chunk.size.toLong())
                    .addSnapshotListener { _, _ ->
                        launch(Dispatchers.IO) { emitEnrichedComments(lastComments) }
                    }
                userListeners.add(docIdReg)
            }
        }

        commentsListener = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
            .document(postId)
            .collection(FirestoreConstants.SUBCOLLECTION_POST_COMMENTS)
            .orderBy(FirestoreConstants.Comment.CREATED_AT, Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(Logger.TAG_DEFAULT, "Error listening comments for post: $postId", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val mapped = snapshot.documents.mapNotNull { document ->
                    try {
                        val timestamp = document.getTimestamp(FirestoreConstants.Comment.CREATED_AT)
                        val updatedTimestamp = document.getTimestamp(FirestoreConstants.Comment.UPDATED_AT)
                        Comment(
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
                            isLikedByCurrentUser = false,
                            authorCity = document.getString(FirestoreConstants.Comment.AUTHOR_CITY)
                                ?: document.getString("author_city")
                        )
                    } catch (e: Exception) {
                        logger.e(Logger.TAG_DEFAULT, "Error mapping comment ${document.id}", e)
                        null
                    }
                }

                lastComments = mapped
                refreshUserListeners(mapped)
                launch(Dispatchers.IO) { emitEnrichedComments(lastComments) }
            }

        awaitClose {
            commentsListener?.remove()
            userListeners.forEach { it.remove() }
        }
    }

    override fun getPostLikes(postId: String): Flow<PagingData<com.omerkaya.sperrmuellfinder.domain.model.User>> {
        // TODO: Implement with FirebasePostDataSource when likes are ready
        return flowOf(PagingData.empty())
    }

    override suspend fun addComment(
        postId: String,
        content: String
    ): Result<Comment> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.Error(Exception("User not authenticated"))

            // Resolve latest profile data from Firestore to avoid stale/anonymous comment author data.
            val currentUserDoc = firestore
                .collection(FirestoreConstants.Collections.USERS_PRIVATE)
                .document(currentUser.uid)
                .get()
                .await()

            val resolvedAuthorName = currentUserDoc.getString(FirestoreConstants.User.DISPLAY_NAME)
                ?: currentUserDoc.getString("displayName")
                ?: currentUserDoc.getString(FirestoreConstants.User.NICKNAME)
                ?: currentUserDoc.getString("nickname")
                ?: currentUser.displayName
                ?: "Anonymous"
            val resolvedAuthorPhotoUrl = currentUserDoc.getString(FirestoreConstants.User.PHOTO_URL)
                ?: currentUserDoc.getString("photoUrl")
                ?: currentUserDoc.getString("photoURL")
                ?: currentUser.photoUrl?.toString()
            val resolvedAuthorCity = currentUserDoc.getString(FirestoreConstants.User.CITY)
                ?: currentUserDoc.getString("City")
            val resolvedAuthorLevel = currentUserDoc.getLong(FirestoreConstants.User.LEVEL)?.toInt() ?: 1

            val commentId = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .document(postId)
                .collection(FirestoreConstants.SUBCOLLECTION_POST_COMMENTS)
                .document().id

            val commentData = mapOf(
                FirestoreConstants.Comment.COMMENT_ID to commentId,
                FirestoreConstants.Comment.POST_ID to postId,
                FirestoreConstants.Comment.AUTHOR_ID to currentUser.uid,
                FirestoreConstants.Comment.AUTHOR_NAME to resolvedAuthorName,
                FirestoreConstants.Comment.AUTHOR_PHOTO_URL to resolvedAuthorPhotoUrl,
                FirestoreConstants.Comment.AUTHOR_CITY to resolvedAuthorCity,
                FirestoreConstants.Comment.AUTHOR_LEVEL to resolvedAuthorLevel,
                FirestoreConstants.Comment.CONTENT to content.trim(),
                FirestoreConstants.Comment.LIKES_COUNT to 0,
                FirestoreConstants.Comment.CREATED_AT to com.google.firebase.Timestamp.now(),
                FirestoreConstants.Comment.UPDATED_AT to com.google.firebase.Timestamp.now()
            )

            // Use transaction to add comment and increment comments count
            firestore.runTransaction { transaction ->
                val postRef = firestore.collection(FirestoreConstants.COLLECTION_POSTS).document(postId)
                val commentRef = postRef.collection(FirestoreConstants.SUBCOLLECTION_POST_COMMENTS).document(commentId)

                transaction.set(commentRef, commentData)
                transaction.update(postRef, FirestoreConstants.FIELD_COMMENTS_COUNT, FieldValue.increment(1L))

                null
            }.await()

            // Create Comment object to return
            val comment = Comment(
                id = commentId,
                postId = postId,
                authorId = currentUser.uid,
                authorName = resolvedAuthorName,
                authorPhotoUrl = resolvedAuthorPhotoUrl,
                authorLevel = resolvedAuthorLevel,
                content = content.trim(),
                likesCount = 0,
                createdAt = Date(),
                updatedAt = Date(),
                isLikedByCurrentUser = false,
                authorCity = resolvedAuthorCity
            )

            Result.Success(comment)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteComment(commentId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.Error(Exception("User not authenticated"))

            // Nested comments path only: posts/{postId}/comments/{commentId}
            val commentsQuery = firestore.collectionGroup(FirestoreConstants.SUBCOLLECTION_POST_COMMENTS)
                .whereEqualTo(FirestoreConstants.Comment.COMMENT_ID, commentId)
                .limit(1)
                .get()
                .await()

            if (commentsQuery.isEmpty) {
                return Result.Error(Exception("Comment not found or not authorized"))
            }

            val nestedCommentDoc = commentsQuery.documents.first()
            val authorId = nestedCommentDoc.getString(FirestoreConstants.Comment.AUTHOR_ID)
            if (authorId != currentUser.uid) {
                return Result.Error(Exception("Comment not found or not authorized"))
            }

            val postId = nestedCommentDoc.getString(FirestoreConstants.Comment.POST_ID)
                ?: return Result.Error(Exception("Invalid comment data"))
            val postRef = firestore.collection(FirestoreConstants.COLLECTION_POSTS).document(postId)

            firestore.runTransaction { transaction ->
                transaction.delete(nestedCommentDoc.reference)
                transaction.update(postRef, FirestoreConstants.FIELD_COMMENTS_COUNT, FieldValue.increment(-1L))
                null
            }.await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun toggleCommentLike(commentId: String): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.Error(Exception("User not authenticated"))

            // Find the comment to get postId
            val commentsQuery = firestore.collectionGroup(FirestoreConstants.SUBCOLLECTION_POST_COMMENTS)
                .whereEqualTo(FirestoreConstants.Comment.COMMENT_ID, commentId)
                .limit(1)
                .get()
                .await()

            if (commentsQuery.isEmpty) {
                return Result.Error(Exception("Comment not found"))
            }

            val commentDoc = commentsQuery.documents.first()
            val postId = commentDoc.getString(FirestoreConstants.Comment.POST_ID)
                ?: return Result.Error(Exception("Invalid comment data"))

            val commentRef = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .document(postId)
                .collection(FirestoreConstants.SUBCOLLECTION_POST_COMMENTS)
                .document(commentId)

            val likeRef = commentRef.collection(FirestoreConstants.Subcollections.LIKES)
                .document(currentUser.uid)

            // Check if already liked
            val likeSnapshot = likeRef.get().await()
            val isCurrentlyLiked = likeSnapshot.exists()

            // Use transaction to toggle like and update count
            firestore.runTransaction { transaction ->
                val commentSnapshot = transaction.get(commentRef)
                val currentLikesCount = commentSnapshot.getLong(FirestoreConstants.Comment.LIKES_COUNT) ?: 0

                if (isCurrentlyLiked) {
                    // Remove like
                    transaction.delete(likeRef)
                    transaction.update(commentRef, FirestoreConstants.Comment.LIKES_COUNT, maxOf(0, currentLikesCount - 1))
                } else {
                    // Add like
                    transaction.set(likeRef, mapOf(
                        "userId" to currentUser.uid,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    ))
                    transaction.update(commentRef, FirestoreConstants.Comment.LIKES_COUNT, currentLikesCount + 1)
                }
                
                null
            }.await()

            Result.Success(!isCurrentlyLiked)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun reportContent(
        targetType: ReportTargetType,
        targetId: String,
        reason: ReportReason,
        description: String?
    ): Result<Report> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            val reportId = firestore.collection(FirestoreConstants.REPORTS).document().id
            val now = FieldValue.serverTimestamp()

            val reportData = hashMapOf<String, Any?>(
                FirestoreConstants.Report.ID to reportId,
                FirestoreConstants.Report.TYPE to targetType.name.lowercase(),
                FirestoreConstants.Report.TARGET_ID to targetId,
                FirestoreConstants.Report.REPORTER_ID to currentUserId,
                FirestoreConstants.Report.REASON to reason.name.lowercase(),
                FirestoreConstants.Report.DESCRIPTION to (description ?: ""),
                FirestoreConstants.Report.CREATED_AT to now,
                FirestoreConstants.Report.STATUS to "open"
            )

            val moderationData = hashMapOf<String, Any?>(
                "reportId" to reportId,
                "targetType" to targetType.name.lowercase(),
                "targetId" to targetId,
                "reporterId" to currentUserId,
                "reason" to reason.name.lowercase(),
                "description" to (description ?: ""),
                "createdAt" to now,
                "status" to "open",
                "priority" to if (reason == ReportReason.HARASSMENT || reason == ReportReason.DANGEROUS_GOODS || reason == ReportReason.SCAM) "high" else "normal"
            )

            val batch = firestore.batch()
            batch.set(
                firestore.collection(FirestoreConstants.REPORTS).document(reportId),
                reportData
            )
            batch.set(
                firestore.collection("moderation_queue").document(reportId),
                moderationData
            )
            batch.commit().await()

            val report = Report(
                id = reportId,
                type = targetType,
                targetId = targetId,
                reporterId = currentUserId,
                reason = reason,
                description = description
            )
            Result.Success(report)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error reporting content", e)
            Result.Error(e)
        }
    }

    override suspend fun extendPostExpiration(
        postId: String,
        hours: Int
    ): Result<Unit> {
        // TODO: Implement with FirebasePostDataSource when post extension is ready
        return Result.Error(Exception("Not implemented"))
    }

    override fun searchPosts(
        query: String?,
        userLocation: PostLocation,
        radiusMeters: Int,
        categories: List<String>,
        city: String?,
        maxHoursOld: Int,
        sortBy: PostSortBy
    ): Flow<PagingData<Post>> {
        return firebasePostDataSource.searchPosts(
            query = query,
            userLocation = userLocation,
            radiusMeters = radiusMeters,
            categories = categories,
            city = city,
            maxHoursOld = maxHoursOld,
            sortBy = sortBy
        )
    }

    override fun getTrendingPosts(
        userLocation: PostLocation,
        radiusMeters: Int
    ): Flow<PagingData<Post>> {
        // Use the same as getPostsNearUser for now, sorted by likes in future
        return firebasePostDataSource.getPostsNearUser(userLocation, radiusMeters)
    }

    override suspend fun refreshFeed(): Result<Unit> {
        // Firebase real-time updates handle this automatically
        return Result.Success(Unit)
    }

    override suspend fun incrementViewCount(postId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                logger.w(Logger.TAG_DEFAULT, "Cannot increment view count: user not authenticated")
                return Result.Success(Unit) // Don't fail for anonymous views
            }

            val userId = currentUser.uid
            logger.d(Logger.TAG_DEFAULT, "Incrementing view count for post: $postId by user: $userId")
            
            // Delegate to FirestoreRepository
            firestoreRepository.incrementViewCount(postId, userId)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error incrementing view count for post: $postId", e)
            Result.Error(e)
        }
    }

    override suspend fun incrementShareCount(postId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.Error(Exception("User not authenticated"))
            }

            val userId = currentUser.uid
            logger.d(Logger.TAG_DEFAULT, "Incrementing share count for post: $postId by user: $userId")
            
            // Delegate to FirestoreRepository
            firestoreRepository.incrementShareCount(postId, userId)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error incrementing share count for post: $postId", e)
            Result.Error(e)
        }
    }

    override suspend fun updatePost(
        postId: String,
        description: String,
        categoriesEn: List<String>,
        categoriesDe: List<String>
    ): Result<Unit> {
        // TODO: Implement post update in FirebasePostDataSource
        return Result.Error(Exception("Not implemented"))
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        return firebasePostDataSource.deletePost(postId)
    }
}
