package com.omerkaya.sperrmuellfinder.data.dto

import com.google.firebase.firestore.PropertyName
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import java.util.Date

/**
 * 💖 LIKE DTO - SperrmüllFinder
 * Firebase Firestore data transfer object for likes
 * Rules.md compliant - Field mapping with @PropertyName
 */
data class LikeDto(
    @PropertyName(FirestoreConstants.Like.POST_ID)
    val postId: String = "",
    
    @PropertyName(FirestoreConstants.Like.USER_ID)
    val userId: String = "",
    
    @PropertyName(FirestoreConstants.Like.USERNAME)
    val username: String = "",
    
    @PropertyName(FirestoreConstants.Like.USER_PHOTO_URL)
    val userPhotoUrl: String? = null,
    
    @PropertyName(FirestoreConstants.Like.USER_LEVEL)
    val userLevel: Int = 1,
    
    @PropertyName(FirestoreConstants.Like.USER_IS_PREMIUM)
    val userIsPremium: Boolean = false,
    
    @PropertyName(FirestoreConstants.Like.CREATED_AT)
    val createdAt: Date = Date(),
    
    @PropertyName(FirestoreConstants.Like.UPDATED_AT)
    val updatedAt: Date = Date()
)