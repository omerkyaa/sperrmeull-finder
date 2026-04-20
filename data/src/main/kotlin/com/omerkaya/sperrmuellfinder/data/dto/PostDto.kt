package com.omerkaya.sperrmuellfinder.data.dto

import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * Post DTO for Firestore mapping.
 * Follows rules.md Firestore schema: posts/{postid}
 * Rules.md compliant - Professional Firebase data mapping.
 */
data class PostDto(
    @PropertyName("id") val id: String = "",
    @PropertyName("ownerid") val ownerId: String = "",
    @PropertyName("owner_display_name") val ownerDisplayName: String = "",
    @PropertyName("owner_photo_url") val ownerPhotoUrl: String? = null,
    @PropertyName("owner_level") val ownerLevel: Int = 0,
    @PropertyName("is_owner_premium") val isOwnerPremium: Boolean = false,
    @PropertyName("owner_premium_frame_type") val ownerPremiumFrameType: String = "NONE",
    @PropertyName("location_street") val locationStreet: String? = null,
    @PropertyName("location_city") val locationCity: String? = null,
    @PropertyName("images") val images: List<String> = emptyList(),
    @PropertyName("description") val description: String = "",
    @PropertyName("location") val location: PostLocationDto? = null,
    @PropertyName("city") val city: String = "",
    @PropertyName("category_en") val categoriesEn: List<String> = emptyList(),
    @PropertyName("category_de") val categoriesDe: List<String> = emptyList(),
    @PropertyName("availability_percent") val availabilityPercent: Int = 100,
    @PropertyName("likes_count") val likesCount: Int = 0,
    @PropertyName("comments_count") val commentsCount: Int = 0,
    @PropertyName("views_count") val viewsCount: Int = 0,
    @PropertyName("shares_count") val sharesCount: Int = 0,
    @PropertyName("status") val status: String = "active",
    @PropertyName("created_at") val createdAt: Date = Date(),
    @PropertyName("expires_at") val expiresAt: Date = Date(),
    @PropertyName("updated_at") val updatedAt: Date = Date(),
    @PropertyName("geohash") val geohash: String? = null // For efficient location queries
)

/**
 * Post location DTO for Firestore mapping.
 */
data class PostLocationDto(
    @PropertyName("latitude") val latitude: Double = 0.0,
    @PropertyName("longitude") val longitude: Double = 0.0,
    @PropertyName("address") val address: String? = null,
    @PropertyName("geohash") val geohash: String? = null
)

/**
 * Comment DTO for Firestore mapping.
 */
data class CommentDto(
    @PropertyName("id") val id: String = "",
    @PropertyName("post_id") val postId: String = "",
    @PropertyName("author_id") val authorId: String = "",
    @PropertyName("author_name") val authorName: String = "",
    @PropertyName("author_photo_url") val authorPhotoUrl: String? = null,
    @PropertyName("author_level") val authorLevel: Int = 0,
    @PropertyName("content") val content: String = "",
    @PropertyName("likes_count") val likesCount: Int = 0,
    @PropertyName("created_at") val createdAt: Date = Date(),
    @PropertyName("updated_at") val updatedAt: Date = Date(),
    @PropertyName("is_liked_by_current_user") val isLikedByCurrentUser: Boolean = false
)

/**
 * Availability Vote DTO for Firestore mapping.
 */
data class AvailabilityVoteDto(
    @PropertyName("id") val id: String = "",
    @PropertyName("post_id") val postId: String = "",
    @PropertyName("voter_id") val voterId: String = "",
    @PropertyName("vote") val vote: String = "", // "still_there" or "taken"
    @PropertyName("created_at") val createdAt: Date = Date(),
    @PropertyName("judged_by_admin") val judgedByAdmin: Boolean = false,
    @PropertyName("honesty_delta") val honestyDelta: Int? = null
)
