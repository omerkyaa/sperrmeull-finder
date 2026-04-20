package com.omerkaya.sperrmuellfinder.data.util

/**
 * 🔥 COMPREHENSIVE FIRESTORE CONSTANTS - SperrmüllFinder
 * Rules.md compliant - Single source of truth for all Firestore operations
 */
object FirestoreConstants {
    
    // ========================================
    // BUSINESS RULES & LIMITS
    // ========================================
    
    const val MAX_CATEGORIES_PER_POST = 3
    const val MIN_CATEGORIES_PER_POST = 1
    const val MAX_POST_IMAGES = 3
    const val POST_EXPIRE_HOURS = 72
    
    // ========================================
    // SAVED STATE KEYS
    // ========================================
    
    const val SAVED_STATE_SEARCH_QUERY = "search_query"
    
    // ========================================
    // COLLECTIONS
    // ========================================
    
    const val USERS = "users"
    const val USERS_PUBLIC = "users_public"
    const val USERS_PRIVATE = "users_private"
    const val POSTS = "posts"
    const val COMMENTS = "comments"
    const val NOTIFICATIONS = "notifications"
    const val LIKES = "likes"
    const val FOLLOWS = "follows"
    const val DEVICE_TOKENS = "device_tokens"
    const val REPORTS = "reports"
    const val BLOCKED_USERS = "blocked_users"
    const val ACCOUNT_DELETIONS = "account_deletions"
    
    object Collections {
        const val USERS = "users"
        const val USERS_PUBLIC = "users_public"
        const val USERS_PRIVATE = "users_private"
        const val POSTS = "posts"
        const val COMMENTS = "comments"
        const val NOTIFICATIONS = "notifications"
        const val LIKES = "likes"
        const val FOLLOWS = "follows"
        const val DEVICE_TOKENS = "device_tokens"
    }
    
    // ========================================
    // COMMON FIELDS
    // ========================================
    
    const val FIELD_CREATED_AT = "created_at"
    const val FIELD_UPDATED_AT = "updated_at"
    const val FIELD_USER_ID = "userId"
    const val FIELD_POST_ID = "postId"
    const val FIELD_LIKES_COUNT = "likes_count"
    const val FIELD_COMMENTS_COUNT = "comments_count"
    const val FIELD_DISPLAY_NAME = "displayname"
    const val FIELD_FIRST_NAME = "firstname"
    const val FIELD_LAST_NAME = "lastname"
    const val FIELD_PHOTO_URL = "photourl"
    const val FIELD_LEVEL = "level"
    const val FIELD_IS_PREMIUM = "ispremium"
    
    // ========================================
    // USER FIELDS
    // ========================================
    
    object User {
        const val USER_ID = "userId"
        const val DISPLAY_NAME = "displayname"
        const val NICKNAME = "nickname"
        const val FIRST_NAME = "firstname"
        const val LAST_NAME = "lastname"
        const val PHOTO_URL = "photourl"
        const val EMAIL = "email"
        const val CITY = "city"
        const val DOB = "dob"
        const val GENDER = "gender"
        const val XP = "xp"
        const val LEVEL = "level"
        const val HONESTY = "honesty"
        const val IS_PREMIUM = "ispremium"
        const val PREMIUM_UNTIL = "premiumuntil"
        const val CREATED_AT = "created_at"
        const val UPDATED_AT = "updated_at"
        const val DEVICE_TOKENS = "device_tokens"
        const val FOLLOWERS_COUNT = "followers_count"
        const val FOLLOWING_COUNT = "following_count"
        const val BADGES = "badges"
        const val FAVORITES = "favorites"
        const val FRAME_LEVEL = "frame_level"
    }
    
    // ========================================
    // POST FIELDS
    // ========================================
    
    object Post {
        const val OWNER_ID = "ownerid"
        const val OWNER_DISPLAY_NAME = "owner_display_name"
        const val OWNER_PHOTO_URL = "owner_photo_url"
        const val OWNER_LEVEL = "owner_level"
        const val IS_OWNER_PREMIUM = "is_owner_premium"
        const val IMAGES = "images"
        const val DESCRIPTION = "description"
        const val LOCATION = "location"
        const val CITY = "city"
        const val CREATED_AT = "created_at"
        const val EXPIRES_AT = "expires_at"
        const val CATEGORY_EN = "category_en"
        const val CATEGORY_DE = "category_de"
        const val AVAILABILITY_PERCENT = "availability_percent"
        const val LIKES_COUNT = "likes_count"
        const val COMMENTS_COUNT = "comments_count"
        const val STATUS = "status"
        const val VIEWS_COUNT = "views_count"
    }
    
    // ========================================
    // LIKE FIELDS
    // ========================================
    
    object Like {
        const val USER_ID = "userId"
        const val POST_ID = "postId"
        const val USERNAME = "username"
        const val USER_PHOTO_URL = "userPhotoUrl"
        const val USER_LEVEL = "userLevel"
        const val USER_IS_PREMIUM = "userIsPremium"
        const val CREATED_AT = "createdAt"  // ✅ Match Firestore field name
        const val UPDATED_AT = "updatedAt"  // ✅ Match Firestore field name
    }
    
    // ========================================
    // NOTIFICATION FIELDS
    // ========================================
    
    object Notification {
        const val NOTIF_ID = "notifId"
        const val USER_ID = "userId"
        const val TYPE = "type"
        const val TITLE = "title"
        const val BODY = "body"
        const val DEEPLINK = "deeplink"
        const val IS_READ = "isRead"
        const val DATA = "data"
        const val CREATED_AT = "created_at"
        
        // Actor (person who triggered the notification)
        const val ACTOR_USER_ID = "actorUserId"
        const val ACTOR_USERNAME = "actorUsername"
        const val ACTOR_PHOTO_URL = "actorPhotoUrl"
        const val ACTOR_LEVEL = "actorLevel"
        const val ACTOR_IS_PREMIUM = "actorIsPremium"
        
        // Post data (for like/comment notifications)
        const val POST_ID = "postId"
        const val POST_IMAGE_URL = "postImageUrl"
        const val POST_DESCRIPTION = "postDescription"
        
        // Comment data (for comment notifications)
        const val COMMENT_ID = "commentId"
        const val COMMENT_TEXT = "commentText"
    }

    // Deep Link Prefixes
    object DeepLinks {
        const val POST_DETAIL_PREFIX = "sperrmuellfinder://post/"
        const val USER_PROFILE_PREFIX = "sperrmuellfinder://user/"
        const val PREMIUM_PREFIX = "sperrmuellfinder://premium"
    }

    // ========================================
    // COMMENT FIELDS
    // ========================================
    
    object Comment {
        const val COMMENT_ID = "commentId"
        const val POST_ID = "postId"
        const val AUTHOR_ID = "authorId"
        const val AUTHOR_NAME = "authorName"
        const val AUTHOR_PHOTO_URL = "authorPhotoUrl"
        const val AUTHOR_CITY = "authorCity"
        const val AUTHOR_LEVEL = "authorLevel"
        const val CONTENT = "content"
        const val LIKES_COUNT = "likesCount"
        const val CREATED_AT = "created_at"
        const val UPDATED_AT = "updated_at"
        const val IS_EDITED = "isEdited"
        const val STATUS = "status"
    }

    // ========================================
    // FOLLOW FIELDS
    // ========================================
    
    object Follow {
        const val FOLLOW_ID = "followId"
        const val FOLLOWER_ID = "followerId"
        const val FOLLOWING_ID = "followingId"
        const val FOLLOWED_ID = "followedId"
        const val USER_ID = "userId"
        const val CREATED_AT = "created_at"
        const val IS_ACTIVE = "isActive"
        
        // Denormalized follower (current user) data
        const val FOLLOWER_DISPLAY_NAME = "followerDisplayName"
        const val FOLLOWER_NICKNAME = "followerNickname"
        const val FOLLOWER_PHOTO_URL = "followerPhotoUrl"
        const val FOLLOWER_LEVEL = "followerLevel"
        const val FOLLOWER_IS_PREMIUM = "followerIsPremium"
        const val FOLLOWER_CITY = "followerCity"
        
        // Denormalized followed (target user) data
        const val FOLLOWED_DISPLAY_NAME = "followedDisplayName"
        const val FOLLOWED_NICKNAME = "followedNickname"
        const val FOLLOWED_PHOTO_URL = "followedPhotoUrl"
        const val FOLLOWED_LEVEL = "followedLevel"
        const val FOLLOWED_IS_PREMIUM = "followedIsPremium"
        const val FOLLOWED_CITY = "followedCity"
    }

    // ========================================
    // LEGACY COLLECTION CONSTANTS (for compatibility)
    // ========================================
    
    const val COLLECTION_POSTS = "posts"
    const val COLLECTION_USERS = "users"
    const val COLLECTION_LIKES = "likes"
    const val COLLECTION_COMMENTS = "comments"
    const val COLLECTION_NOTIFICATIONS = "notifications"
    const val COLLECTION_FAVORITES = "favorites"
    const val COLLECTION_FOLLOWERS = "followers"
    const val COLLECTION_XP_TRANSACTIONS = "xp_transactions"
    const val COLLECTION_HONESTY_TRANSACTIONS = "honesty_transactions"
    
    // ========================================
    // LEGACY SUBCOLLECTION CONSTANTS
    // ========================================
    
    const val SUBCOLLECTION_POST_LIKES = "likes"
    const val SUBCOLLECTION_POST_COMMENTS = "comments"
    const val SUBCOLLECTION_USER_NOTIFICATIONS = "user_notifications"
    const val SUBCOLLECTION_FOLLOWERS = "followers"
    const val SUBCOLLECTION_FOLLOWING = "following"
    const val SUBCOLLECTION_USER_XP_TRANSACTIONS = "user_xp_transactions"
    
    // ========================================
    // LEGACY FIELD CONSTANTS
    // ========================================
    
    const val FIELD_OWNER_ID = "ownerid"
    const val FIELD_OWNER_DISPLAY_NAME = "owner_display_name"
    const val FIELD_OWNER_PHOTO_URL = "owner_photo_url"
    const val FIELD_OWNER_LEVEL = "owner_level"
    const val FIELD_IS_OWNER_PREMIUM = "is_owner_premium"
    const val FIELD_IMAGES = "images"
    const val FIELD_DESCRIPTION = "description"
    const val FIELD_LOCATION = "location"
    const val FIELD_LOCATION_STREET = "location_street"
    const val FIELD_LOCATION_CITY = "location_city"
    const val FIELD_CITY = "city"
    const val FIELD_CATEGORY_EN = "category_en"
    const val FIELD_CATEGORY_DE = "category_de"
    const val FIELD_AVAILABILITY_PERCENT = "availability_percent"
    const val FIELD_VIEWS_COUNT = "views_count"
    const val FIELD_SHARES_COUNT = "shares_count"
    const val FIELD_STATUS = "status"
    const val FIELD_EXPIRES_AT = "expires_at"
    const val FIELD_XP = "xp"
    const val FIELD_HONESTY = "honesty"
    const val FIELD_FOLLOWERS_COUNT = "followers_count"
    const val FIELD_FOLLOWING_COUNT = "following_count"
    
    // Transaction fields
    const val FIELD_DELTA = "delta"
    const val FIELD_REASON = "reason"
    const val FIELD_PREMIUM_BONUS_APPLIED = "premium_bonus_applied"
    const val FIELD_LEVEL_BEFORE = "level_before"
    const val FIELD_LEVEL_AFTER = "level_after"
    const val FIELD_BEFORE = "before"
    const val FIELD_AFTER = "after"
    
    // Timestamp fields
    const val FIELD_LIKED_AT = "liked_at"
    const val FIELD_FAVORITED_AT = "favorited_at"
    
    // ========================================
    // NOTIFICATION FIELD CONSTANTS (Legacy)
    // ========================================
    
    object NotificationFields {
        const val TYPE = "type"
        const val TITLE = "title"
        const val BODY = "body"
        const val DATA = "data"
        const val IS_READ = "isRead"
        const val CREATED_AT = "created_at"
    }
    
    // ========================================
    // NOTIFICATION TYPE CONSTANTS
    // ========================================
    
    const val NOTIFICATION_TYPE_LIKE = "like"
    const val NOTIFICATION_TYPE_COMMENT = "comment"
    const val NOTIFICATION_TYPE_FOLLOW = "follow"
    const val NOTIFICATION_TYPE_FAVORITE = "favorite"
    const val NOTIFICATION_TYPE_LEVEL_UP = "level_up"
    
    // ========================================
    // POST STATUS VALUES
    // ========================================
    
    object PostStatusValues {
        const val ACTIVE = "active"
        const val ARCHIVED = "archived"
        const val REMOVED = "removed"
        const val EXPIRED = "expired"
    }

    // ========================================
    // SUBCOLLECTIONS
    // ========================================
    
    object Subcollections {
        const val LIKES = "likes"
        const val COMMENTS = "comments"
        const val VIEWS = "views"
    }

    // ========================================
    // CATEGORY CONSTANTS
    // ========================================
    
    val CATEGORIES_DE = listOf(
        "Möbel",
        "Elektronik", 
        "Kleidung",
        "Bücher",
        "Spielzeug",
        "Sport",
        "Küche",
        "Garten",
        "Dekoration",
        "Werkzeuge",
        "Haushaltsgeräte",
        "Auto & Motor",
        "Schönheit",
        "Gesundheit",
        "Musik",
        "Kunst",
        "Büro",
        "Baby",
        "Haustier",
        "Sonstiges"
    )
    
    val CATEGORIES_EN = listOf(
        "furniture",
        "electronics",
        "clothing",
        "books",
        "toys",
        "sports",
        "kitchen",
        "garden",
        "decoration",
        "tools",
        "appliances",
        "automotive",
        "beauty",
        "health",
        "music",
        "art",
        "office",
        "baby",
        "pet",
        "other"
    )

    // ========================================
    // CATEGORY MAPPING (DE → EN)
    // ========================================
    
    /** German (DE) → English (EN) */
    val CATEGORY_MAPPING = mapOf(
        "Möbel" to "furniture",
        "Elektronik" to "electronics",
        "Kleidung" to "clothing",
        "Bücher" to "books",
        "Spielzeug" to "toys",
        "Sport" to "sports",
        "Küche" to "kitchen",
        "Garten" to "garden",
        "Dekoration" to "decoration",
        "Werkzeuge" to "tools",
        "Haushaltsgeräte" to "appliances",
        "Auto & Motor" to "automotive",
        "Schönheit" to "beauty",
        "Gesundheit" to "health",
        "Musik" to "music",
        "Kunst" to "art",
        "Büro" to "office",
        "Baby" to "baby",
        "Haustier" to "pet",
        "Sonstiges" to "other"
    )

    /** English (EN) → German (DE) — used when storing category_de alongside category_en */
    val CATEGORY_EN_TO_DE: Map<String, String> = CATEGORY_MAPPING.entries.associate { (de, en) -> en to de }
    
    // ========================================
    // REPORT FIELDS
    // ========================================
    
    object Report {
        const val ID = "id"
        const val TYPE = "type"
        const val TARGET_ID = "targetId"
        const val REPORTER_ID = "reporterId"
        const val REPORTER_NAME = "reporterName"
        const val REPORTER_PHOTO_URL = "reporterPhotoUrl"
        const val REASON = "reason"
        const val DESCRIPTION = "description"
        const val CREATED_AT = "createdAt"
        const val STATUS = "status"
        const val PRIORITY = "priority"
        const val ASSIGNED_TO = "assignedTo"
        const val ASSIGNED_TO_NAME = "assignedToName"
        const val ADMIN_NOTES = "adminNotes"
        const val RESOLVED_AT = "resolvedAt"
        const val RESOLVED_BY = "resolvedBy"
        const val RESOLVED_BY_NAME = "resolvedByName"
        const val MODERATION_QUEUE_ID = "moderationQueueId"
        const val TARGET_OWNER_ID = "targetOwnerId"
        const val TARGET_OWNER_NAME = "targetOwnerName"
        const val TARGET_CONTENT = "targetContent"
        const val TARGET_IMAGE_URL = "targetImageUrl"
    }

    // ========================================
    // BLOCKED USER FIELDS
    // ========================================
    
    object BlockedUser {
        const val BLOCKER_ID = "blockerId"
        const val BLOCKED_USER_ID = "blockedUserId"
        const val BLOCKED_USER_NAME = "blockedUserName"
        const val BLOCKED_USER_PHOTO_URL = "blockedUserPhotoUrl"
        const val REASON = "reason"
        const val CREATED_AT = "createdAt"
    }

}
