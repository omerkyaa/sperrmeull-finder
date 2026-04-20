package com.omerkaya.sperrmuellfinder.data.dto.user

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * User DTO for Firestore mapping
 * Follows rules.md Firestore schema: users/{uid}
 */
data class UserDto(
    @PropertyName("uid") val uid: String = "",
    @PropertyName("email") val email: String = "",
    @PropertyName("displayName") val displayName: String = "",
    @PropertyName("nickname") val nickname: String = "",
    @PropertyName("firstName") val firstName: String = "",
    @PropertyName("lastName") val lastName: String = "",
    @PropertyName("photoUrl") val photoUrl: String? = null,
    @PropertyName("city") val city: String = "",
    @PropertyName("dateOfBirth") val dateOfBirth: Date? = null,
    @PropertyName("gender") val gender: String? = null,
    @PropertyName("xp") val xp: Int = 0,
    @PropertyName("level") val level: Int = 0,
    @PropertyName("honesty") val honesty: Int = 100,
    @PropertyName("isPremium") val isPremium: Boolean = false,
    @PropertyName("premiumUntil") val premiumUntil: Date? = null,
    @PropertyName("badges") val badges: List<String> = emptyList(),
    @PropertyName("favorites") val favorites: UserFavoritesDto = UserFavoritesDto(),
    @PropertyName("fcmToken") val fcmToken: String? = null,
    @PropertyName("deviceTokens") val deviceTokens: List<String> = emptyList(),
    @PropertyName("deviceLang") val deviceLang: String = "de",
    @PropertyName("deviceModel") val deviceModel: String = "",
    @PropertyName("deviceOs") val deviceOs: String = "",
    @PropertyName("frameLevel") val frameLevel: Int = 0,
    @PropertyName("isBanned") val isBanned: Boolean = false,
    @PropertyName("banType") val banType: String? = null,
    @PropertyName("banUntil") val banUntil: Date? = null,
    @PropertyName("banReason") val banReason: String? = null,
    @PropertyName("bannedBy") val bannedBy: String? = null,
    @PropertyName("bannedAt") val bannedAt: Any? = null,
    @PropertyName("authDisabled") val authDisabled: Boolean = false,
    @PropertyName("createdAt") val createdAt: Any? = null,
    @PropertyName("updatedAt") val updatedAt: Any? = null,
    @PropertyName("lastLoginAt") val lastLoginAt: Any? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "email" to email,
        "displayName" to displayName,
        "nickname" to nickname,
        "firstName" to firstName,
        "lastName" to lastName,
        "photoUrl" to photoUrl,
        "city" to city,
        "dateOfBirth" to dateOfBirth,
        "gender" to gender,
        "xp" to xp,
        "level" to level,
        "honesty" to honesty,
        "isPremium" to isPremium,
        "premiumUntil" to premiumUntil,
        "badges" to badges,
        "favorites" to favorites,
        "fcmToken" to fcmToken,
        "deviceTokens" to deviceTokens,
        "deviceLang" to deviceLang,
        "deviceModel" to deviceModel,
        "deviceOs" to deviceOs,
        "frameLevel" to frameLevel,
        "isBanned" to isBanned,
        "banType" to banType,
        "banUntil" to banUntil,
        "banReason" to banReason,
        "bannedBy" to bannedBy,
        "bannedAt" to bannedAt,
        "authDisabled" to authDisabled,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "lastLoginAt" to lastLoginAt
    )
}

/**
 * User favorites DTO
 */
data class UserFavoritesDto(
    @PropertyName("regions") val regions: List<String> = emptyList(),
    @PropertyName("categories") val categories: List<String> = emptyList()
)

/**
 * XP Transaction DTO for Firestore
 */
data class XpTransactionDto(
    @PropertyName("id") val id: String = "",
    @PropertyName("user_id") val userId: String = "",
    @PropertyName("delta") val delta: Int = 0,
    @PropertyName("reason") val reason: String = "",
    @PropertyName("created_at") val createdAt: Date = Date(),
    @PropertyName("premium_bonus_applied") val premiumBonusApplied: Boolean = false,
    @PropertyName("level_before") val levelBefore: Int = 0,
    @PropertyName("level_after") val levelAfter: Int = 0,
    @PropertyName("xp_before") val xpBefore: Int = 0,
    @PropertyName("xp_after") val xpAfter: Int = 0
)

/**
 * Honesty Transaction DTO for Firestore
 */
data class HonestyTransactionDto(
    @PropertyName("id") val id: String = "",
    @PropertyName("user_id") val userId: String = "",
    @PropertyName("delta") val delta: Int = 0,
    @PropertyName("reason") val reason: String = "",
    @PropertyName("created_at") val createdAt: Date = Date(),
    @PropertyName("honesty_before") val honestyBefore: Int = 0,
    @PropertyName("honesty_after") val honestyAfter: Int = 0,
    @PropertyName("related_post_id") val relatedPostId: String? = null,
    @PropertyName("related_report_id") val relatedReportId: String? = null
)

/**
 * Leaderboard Entry DTO
 */
data class LeaderboardEntryDto(
    @PropertyName("position") val position: Int = 0,
    @PropertyName("user") val user: UserDto = UserDto(),
    @PropertyName("xp") val xp: Int = 0,
    @PropertyName("level") val level: Int = 0,
    @PropertyName("weekly_xp") val weeklyXp: Int? = null,
    @PropertyName("monthly_xp") val monthlyXp: Int? = null
)
