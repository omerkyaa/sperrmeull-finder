package com.omerkaya.sperrmuellfinder.data.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.omerkaya.sperrmuellfinder.data.dto.user.UserDto
import com.omerkaya.sperrmuellfinder.data.dto.user.UserFavoritesDto
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.model.UserFavorites
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper class for User related models
 * Rules.md compliant - Clean Architecture data layer
 */
@Singleton
class UserMapper @Inject constructor() {
    
    /**
     * Maps Firestore DocumentSnapshot to User domain model
     */
    fun mapFromFirestore(document: DocumentSnapshot): User? {
        return try {
            document.toUser()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Maps User domain model to UserDto
     */
    fun mapToDto(user: User): UserDto {
        return user.toDto()
    }
    
    /**
     * Maps UserDto to User domain model
     */
    fun mapFromDto(dto: UserDto): User {
        return dto.toDomainModel()
    }
}

fun UserDto.toDomainModel(): User {
    return User(
        uid = uid,
        email = email,
        displayName = displayName,
        nickname = nickname,
        firstName = firstName,
        lastName = lastName,
        photoUrl = photoUrl,
        city = city,
        dob = dateOfBirth,
        gender = gender,
        xp = xp,
        level = level,
        honesty = honesty,
        isPremium = isPremium,
        premiumUntil = premiumUntil,
        badges = badges,
        favorites = UserFavorites(
            regions = favorites.regions,
            categories = favorites.categories
        ),
        fcmToken = fcmToken,
        deviceTokens = deviceTokens,
        deviceLang = deviceLang,
        deviceModel = deviceModel,
        deviceOs = deviceOs,
        frameLevel = frameLevel,
        followersCount = followersCount,
        followingCount = followingCount,
        createdAt = (createdAt as? Timestamp)?.toDate(),
        updatedAt = (updatedAt as? Timestamp)?.toDate(),
        lastLoginAt = (lastLoginAt as? Timestamp)?.toDate(),
        isBanned = isBanned,
        banType = banType,
        banUntil = banUntil,
        banReason = banReason,
        bannedBy = bannedBy,
        bannedAt = (bannedAt as? Timestamp)?.toDate(),
        authDisabled = authDisabled
    )
}

fun User.toDto(): UserDto {
    return UserDto(
        uid = uid,
        email = email,
        displayName = displayName,
        nickname = nickname,
        firstName = firstName,
        lastName = lastName,
        photoUrl = photoUrl,
        city = city ?: "",
        dateOfBirth = dob,
        gender = gender,
        xp = xp,
        level = level,
        honesty = honesty,
        isPremium = isPremium,
        premiumUntil = premiumUntil,
        badges = badges,
        favorites = UserFavoritesDto(
            regions = favorites.regions,
            categories = favorites.categories
        ),
        fcmToken = fcmToken,
        deviceTokens = deviceTokens,
        deviceLang = deviceLang,
        deviceModel = deviceModel,
        deviceOs = deviceOs,
        frameLevel = frameLevel,
        isBanned = isBanned,
        banType = banType,
        banUntil = banUntil,
        banReason = banReason,
        bannedBy = bannedBy,
        bannedAt = bannedAt,
        authDisabled = authDisabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastLoginAt = lastLoginAt
    )
}

/**
 * Convert Firestore DocumentSnapshot to User domain model.
 * Used for real-time listeners and direct Firestore queries.
 */
fun DocumentSnapshot.toUser(): User {
    val resolvedDisplayName = getString(FirestoreConstants.User.DISPLAY_NAME)
        ?: getString("displayname")
        ?: getString("displayName")
        ?: getString("display_name")
        ?: getString("username")
        ?: getString("userName")
        ?: getString(FirestoreConstants.User.NICKNAME)
        ?: getString("nickname")
        ?: buildString {
            val first = getString(FirestoreConstants.User.FIRST_NAME) ?: getString("firstName") ?: ""
            val last = getString(FirestoreConstants.User.LAST_NAME) ?: getString("lastName") ?: ""
            append(first)
            if (first.isNotBlank() && last.isNotBlank()) append(" ")
            append(last)
        }.takeIf { it.isNotBlank() }
        ?: getString(FirestoreConstants.User.FIRST_NAME)
        ?: getString("firstname")
        ?: getString(FirestoreConstants.User.EMAIL)
        ?: ""
    val resolvedPhotoUrl = getString(FirestoreConstants.User.PHOTO_URL)
        ?: getString("photoUrl")
        ?: getString("photoURL")
        ?: getString("profilePhotoUrl")
        ?: getString("avatarUrl")
    val rawDeviceTokens = get(FirestoreConstants.User.DEVICE_TOKENS)
    val resolvedDeviceTokens = when (rawDeviceTokens) {
        is List<*> -> rawDeviceTokens.mapNotNull { it as? String }
        is String -> listOf(rawDeviceTokens)
        else -> emptyList()
    }
    val resolvedFcmToken = when {
        rawDeviceTokens is String -> rawDeviceTokens
        resolvedDeviceTokens.isNotEmpty() -> resolvedDeviceTokens.firstOrNull()
        else -> getString("fcmToken")
    }

    return User(
        uid = getString(FirestoreConstants.User.USER_ID) ?: id,
        email = getString(FirestoreConstants.User.EMAIL) ?: "",
        displayName = resolvedDisplayName,
        nickname = getString(FirestoreConstants.User.NICKNAME) ?: getString("nickname") ?: "",
        firstName = getString(FirestoreConstants.User.FIRST_NAME) ?: "",
        lastName = getString(FirestoreConstants.User.LAST_NAME) ?: "",
        photoUrl = resolvedPhotoUrl,
        city = getString(FirestoreConstants.User.CITY)
            ?: getString("City")
            ?: getString("cityName"),
        dob = (get(FirestoreConstants.User.DOB) as? Timestamp)?.toDate(),
        gender = getString(FirestoreConstants.User.GENDER),
        xp = getLong(FirestoreConstants.User.XP)?.toInt() ?: 0,
        level = getLong(FirestoreConstants.User.LEVEL)?.toInt() ?: 1,
        honesty = getLong(FirestoreConstants.User.HONESTY)?.toInt() ?: 100,
        isPremium = getBoolean(FirestoreConstants.User.IS_PREMIUM)
            ?: getBoolean("isPremium")
            ?: false,
        premiumUntil = (get(FirestoreConstants.User.PREMIUM_UNTIL) as? Timestamp)?.toDate()
            ?: (get("premiumUntil") as? Timestamp)?.toDate(),
        badges = get(FirestoreConstants.User.BADGES) as? List<String> ?: emptyList(),
        favorites = UserFavorites(
            regions = (get(FirestoreConstants.User.FAVORITES + ".regions") as? List<String>) ?: emptyList(),
            categories = (get(FirestoreConstants.User.FAVORITES + ".categories") as? List<String>) ?: emptyList()
        ),
        fcmToken = resolvedFcmToken,
        deviceTokens = resolvedDeviceTokens,
        deviceLang = getString("devicelang") ?: "de",
        deviceModel = getString("devicemodel") ?: "",
        deviceOs = getString("deviceos") ?: "",
        frameLevel = getLong(FirestoreConstants.User.FRAME_LEVEL)?.toInt() ?: 0,
        followersCount = getLong(FirestoreConstants.User.FOLLOWERS_COUNT)?.toInt() ?: 0,
        followingCount = getLong(FirestoreConstants.User.FOLLOWING_COUNT)?.toInt() ?: 0,
        createdAt = (get(FirestoreConstants.User.CREATED_AT) as? Timestamp)?.toDate() ?: Date(),
        updatedAt = (get(FirestoreConstants.User.UPDATED_AT) as? Timestamp)?.toDate() ?: Date(),
        lastLoginAt = (get("lastloginat") as? Timestamp)?.toDate(),
        isBanned = getBoolean("isBanned") ?: false,
        banType = getString("banType"),
        banUntil = (get("banUntil") as? Timestamp)?.toDate(),
        banReason = getString("banReason"),
        bannedBy = getString("bannedBy"),
        bannedAt = (get("bannedAt") as? Timestamp)?.toDate(),
        authDisabled = getBoolean("authDisabled") ?: false
    )
}

/**
 * Extension function to convert UserDto to User domain model
 */
fun UserDto.toDomain(): User {
    return User(
        uid = this.uid,
        email = this.email,
        displayName = this.displayName,
        nickname = this.nickname,
        firstName = this.firstName,
        lastName = this.lastName,
        photoUrl = this.photoUrl,
        city = this.city,
        dob = this.dateOfBirth,
        gender = this.gender,
        xp = this.xp,
        level = this.level,
        honesty = this.honesty,
        isPremium = this.isPremium,
        premiumUntil = this.premiumUntil,
        badges = this.badges,
        favorites = UserFavorites(
            regions = this.favorites.regions,
            categories = this.favorites.categories
        ),
        fcmToken = this.fcmToken,
        deviceTokens = this.deviceTokens,
        deviceLang = this.deviceLang,
        deviceModel = this.deviceModel,
        deviceOs = this.deviceOs,
        frameLevel = this.frameLevel,
        followersCount = this.followersCount,
        followingCount = this.followingCount,
        createdAt = this.createdAt as? Date ?: Date(),
        updatedAt = this.updatedAt as? Date ?: Date(),
        lastLoginAt = this.lastLoginAt as? Date,
        isBanned = this.isBanned,
        banType = this.banType,
        banUntil = this.banUntil,
        banReason = this.banReason,
        bannedBy = this.bannedBy,
        bannedAt = this.bannedAt as? Date,
        authDisabled = this.authDisabled
    )
}
