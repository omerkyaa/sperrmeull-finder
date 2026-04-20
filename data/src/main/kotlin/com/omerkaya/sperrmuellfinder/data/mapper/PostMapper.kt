package com.omerkaya.sperrmuellfinder.data.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.data.dto.PostDto
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.PostStatus
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper for converting between Post domain model and Firestore documents.
 * Rules.md compliant - Clean Architecture data layer mapping.
 */
@Singleton
class PostMapper @Inject constructor() {

    private fun Map<String, Any>.stringValue(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            (this[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        }
    }

    private fun Map<String, Any>.intValue(vararg keys: String): Int? {
        return keys.firstNotNullOfOrNull { key ->
            (this[key] as? Number)?.toInt()
        }
    }

    private fun Map<String, Any>.booleanValue(vararg keys: String): Boolean? {
        return keys.firstNotNullOfOrNull { key ->
            this[key] as? Boolean
        }
    }

    /**
     * Create Post from Firestore DocumentSnapshot with null-safe mapping
     * Rules.md compliant - Safe Firestore document parsing
     */
    fun mapFromFirestore(document: DocumentSnapshot): Post? {
        return try {
            val data = document.data ?: return null
            
            // Extract location data safely with enhanced mapping
            val locationData = data[FirestoreConstants.FIELD_LOCATION] as? Map<String, Any>
            val location = locationData?.let { locData ->
                val lat = parseCoordinate(locData["latitude"])
                val lng = parseCoordinate(locData["longitude"])
                val city = locData["city"] as? String
                val country = locData["country"] as? String
                val address = locData["address"] as? String
                
                if (lat != null && lng != null) {
                    PostLocation(lat, lng, city, country, address)
                } else null
            }
            
            // Enhanced location street and city extraction
            val locationStreet = data[FirestoreConstants.FIELD_LOCATION_STREET] as? String 
                ?: data["street"] as? String 
                ?: locationData?.get("street") as? String
                ?: locationData?.get("address") as? String
                
            val locationCity = data[FirestoreConstants.FIELD_LOCATION_CITY] as? String 
                ?: data["city"] as? String
                ?: locationData?.get("city") as? String
                ?: location?.city
            
            // Extract timestamps safely
            val createdAtTimestamp = data[FirestoreConstants.FIELD_CREATED_AT] as? Timestamp
            val expiresAtTimestamp = data[FirestoreConstants.FIELD_EXPIRES_AT] as? Timestamp
            val updatedAtTimestamp = data[FirestoreConstants.FIELD_UPDATED_AT] as? Timestamp
            
            // Extract status
            val statusString = data[FirestoreConstants.FIELD_STATUS] as? String ?: "ACTIVE"
            
            val resolvedOwnerId = data.stringValue(
                FirestoreConstants.FIELD_OWNER_ID,
                "ownerId",
                "owner_id",
                "userId",
                "userid",
                "authorId",
                "author_id"
            ) ?: ""

            val resolvedOwnerDisplayName = data.stringValue(
                FirestoreConstants.FIELD_OWNER_DISPLAY_NAME,
                "ownerDisplayName",
                "owner_name",
                "ownerName",
                "ownername",
                "displayName",
                "displayname",
                "username",
                "nickname"
            )

            val resolvedOwnerPhotoUrl = data.stringValue(
                FirestoreConstants.FIELD_OWNER_PHOTO_URL,
                "ownerPhotoUrl",
                "owner_photo",
                "photoUrl",
                "photoURL",
                "avatarUrl",
                "profilePhotoUrl"
            )

            val post = Post(
                id = document.id,
                ownerId = resolvedOwnerId,
                ownerDisplayName = resolvedOwnerDisplayName,
                ownerPhotoUrl = resolvedOwnerPhotoUrl,
                ownerLevel = data.intValue(FirestoreConstants.FIELD_OWNER_LEVEL, "ownerLevel", "owner_level"),
                isOwnerPremium = data.booleanValue(FirestoreConstants.FIELD_IS_OWNER_PREMIUM, "isOwnerPremium", "owner_premium"),
                ownerPremiumFrameType = PremiumFrameType.NONE, // Will be calculated from level and premium status
                images = (data[FirestoreConstants.FIELD_IMAGES] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                description = data[FirestoreConstants.FIELD_DESCRIPTION] as? String ?: "",
                location = location,
                locationStreet = locationStreet,
                locationCity = locationCity,
                city = data[FirestoreConstants.FIELD_CITY] as? String ?: locationCity ?: "",
                categoriesEn = (data[FirestoreConstants.FIELD_CATEGORY_EN] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                categoriesDe = (data[FirestoreConstants.FIELD_CATEGORY_DE] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                categoryEn = (data[FirestoreConstants.FIELD_CATEGORY_EN] as? List<*>)?.firstOrNull() as? String,
                categoryDe = (data[FirestoreConstants.FIELD_CATEGORY_DE] as? List<*>)?.firstOrNull() as? String,
                availabilityPercent = (data[FirestoreConstants.FIELD_AVAILABILITY_PERCENT] as? Number)?.toInt(),
                likesCount = (data[FirestoreConstants.FIELD_LIKES_COUNT] as? Number)?.toInt() ?: 0,
                commentsCount = (data[FirestoreConstants.FIELD_COMMENTS_COUNT] as? Number)?.toInt() ?: 0,
                viewsCount = (data[FirestoreConstants.FIELD_VIEWS_COUNT] as? Number)?.toInt() ?: 0,
                sharesCount = (data[FirestoreConstants.FIELD_SHARES_COUNT] as? Number)?.toInt() ?: 0,
                status = PostStatus.valueOf(statusString.uppercase()),
                createdAt = createdAtTimestamp?.toDate() ?: Date(),
                expiresAt = expiresAtTimestamp?.toDate() ?: Date(System.currentTimeMillis() + 72 * 60 * 60 * 1000),
                updatedAt = updatedAtTimestamp?.toDate() ?: Date(),
                isLikedByCurrentUser = false, // Will be set by repository layer
                isFavoritedByCurrentUser = false, // Will be set by repository layer
                distanceFromUser = null // Will be calculated by repository layer
            )
            
            post
        } catch (e: Exception) {
            null // Return null for invalid documents
        }
    }

    /**
     * Convert Post to Firestore document map
     * Rules.md compliant - Safe Firestore document creation
     */
    fun toMap(post: Post): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        
        map[FirestoreConstants.FIELD_OWNER_ID] = post.ownerId
        map[FirestoreConstants.FIELD_OWNER_DISPLAY_NAME] = post.ownerDisplayName
        map[FirestoreConstants.FIELD_OWNER_PHOTO_URL] = post.ownerPhotoUrl
        map[FirestoreConstants.FIELD_OWNER_LEVEL] = post.ownerLevel
        map[FirestoreConstants.FIELD_IS_OWNER_PREMIUM] = post.isOwnerPremium
        map[FirestoreConstants.FIELD_IMAGES] = post.images
        map[FirestoreConstants.FIELD_DESCRIPTION] = post.description
        
        // Convert location to map
        post.location?.let { loc ->
            map[FirestoreConstants.FIELD_LOCATION] = mapOf(
                "latitude" to loc.latitude,
                "longitude" to loc.longitude,
                "city" to loc.city,
                "country" to loc.country,
                "address" to loc.address,
                "geohash" to loc.geohash
            )
        }
        
        map[FirestoreConstants.FIELD_LOCATION_STREET] = post.locationStreet
        map[FirestoreConstants.FIELD_LOCATION_CITY] = post.locationCity
        map[FirestoreConstants.FIELD_CITY] = post.city
        map[FirestoreConstants.FIELD_CATEGORY_EN] = post.categoriesEn
        map[FirestoreConstants.FIELD_CATEGORY_DE] = post.categoriesDe
        map[FirestoreConstants.FIELD_AVAILABILITY_PERCENT] = post.availabilityPercent
        map[FirestoreConstants.FIELD_LIKES_COUNT] = post.likesCount
        map[FirestoreConstants.FIELD_COMMENTS_COUNT] = post.commentsCount
        map[FirestoreConstants.FIELD_VIEWS_COUNT] = post.viewsCount
        map[FirestoreConstants.FIELD_SHARES_COUNT] = post.sharesCount
        map[FirestoreConstants.FIELD_STATUS] = post.status.name.lowercase()
        map[FirestoreConstants.FIELD_CREATED_AT] = Timestamp(post.createdAt)
        map[FirestoreConstants.FIELD_EXPIRES_AT] = Timestamp(post.expiresAt)
        map[FirestoreConstants.FIELD_UPDATED_AT] = Timestamp(post.updatedAt)
        
        return map
    }

    private fun parseCoordinate(value: Any?): Double? {
        val parsed = when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
        if (parsed == null || parsed.isNaN() || parsed.isInfinite()) return null
        return parsed
    }

    /**
     * Create Post from PostDto with null-safe mapping
     * Rules.md compliant - Safe DTO to domain model conversion
     */
    fun fromDto(dto: PostDto): Post {
        // Convert PostLocationDto to PostLocation
        val location = dto.location?.let { locationDto ->
            PostLocation(
                latitude = locationDto.latitude,
                longitude = locationDto.longitude,
                city = null,
                country = null,
                address = locationDto.address,
                geohash = locationDto.geohash
            )
        }
        
        // Parse premium frame type safely
        val premiumFrameType = try {
            PremiumFrameType.valueOf(dto.ownerPremiumFrameType.uppercase())
        } catch (e: Exception) {
        PremiumFrameType.NONE
        }
        
        // Parse post status safely
        val postStatus = try {
            PostStatus.valueOf(dto.status.uppercase())
        } catch (e: Exception) {
        PostStatus.ACTIVE
        }
        
        return Post(
            id = dto.id,
            ownerId = dto.ownerId,
            ownerDisplayName = dto.ownerDisplayName.takeIf { it.isNotBlank() },
            ownerPhotoUrl = dto.ownerPhotoUrl,
            ownerLevel = dto.ownerLevel.takeIf { it > 0 },
            isOwnerPremium = dto.isOwnerPremium,
            ownerPremiumFrameType = premiumFrameType,
            images = dto.images,
            description = dto.description,
            location = location,
            locationStreet = dto.locationStreet,
            locationCity = dto.locationCity,
            city = dto.city,
            categoriesEn = dto.categoriesEn,
            categoriesDe = dto.categoriesDe,
            categoryEn = dto.categoriesEn.firstOrNull(),
            categoryDe = dto.categoriesDe.firstOrNull(),
            availabilityPercent = dto.availabilityPercent.takeIf { it in 0..100 },
            likesCount = dto.likesCount,
            commentsCount = dto.commentsCount,
            viewsCount = dto.viewsCount,
            sharesCount = dto.sharesCount,
            status = postStatus,
            createdAt = dto.createdAt,
            expiresAt = dto.expiresAt,
            updatedAt = dto.updatedAt,
            isLikedByCurrentUser = false, // Will be set by repository layer
            isFavoritedByCurrentUser = false, // Will be set by repository layer
            distanceFromUser = null // Will be calculated by repository layer
        )
    }
}

/**
 * Extension function for DocumentSnapshot to convert to Post domain model.
 * Provides a more convenient API for repository implementations.
 */
fun DocumentSnapshot.toPost(): Post {
    return PostMapper().mapFromFirestore(this) ?: throw IllegalStateException("Failed to convert document ${this.id} to Post")
}
