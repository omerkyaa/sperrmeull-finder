package com.omerkaya.sperrmuellfinder.data.mapper

import com.google.firebase.firestore.DocumentSnapshot
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import java.util.Date

/**
 * 💬 COMMENT MAPPER - SperrmüllFinder
 * Maps between Firestore documents and Comment domain model
 * Rules.md compliant - Clean Architecture data mapping
 */

/**
 * Convert Firestore DocumentSnapshot to Comment domain model
 */
fun DocumentSnapshot.toComment(): Comment {
    return Comment(
        id = id,
        postId = getString(FirestoreConstants.Comment.POST_ID)
            ?: getString("post_id")
            ?: "",
        authorId = getString(FirestoreConstants.Comment.AUTHOR_ID)
            ?: getString("author_id")
            ?: "",
        authorName = getString(FirestoreConstants.Comment.AUTHOR_NAME)
            ?: getString("author_name")
            ?: "",
        authorPhotoUrl = getString(FirestoreConstants.Comment.AUTHOR_PHOTO_URL)
            ?: getString("author_photo_url"),
        authorLevel = getLong(FirestoreConstants.Comment.AUTHOR_LEVEL)?.toInt() ?: 1,
        content = getString(FirestoreConstants.Comment.CONTENT) ?: "",
        likesCount = getLong(FirestoreConstants.Comment.LIKES_COUNT)?.toInt() ?: 0,
        createdAt = getDate(FirestoreConstants.Comment.CREATED_AT)
            ?: getDate("createdAt")
            ?: Date(),
        updatedAt = getDate(FirestoreConstants.Comment.UPDATED_AT)
            ?: getDate("updatedAt")
            ?: Date(),
        isLikedByCurrentUser = false, // This will be set separately by the repository
        authorCity = getString(FirestoreConstants.Comment.AUTHOR_CITY)
            ?: getString("author_city")
    )
}
