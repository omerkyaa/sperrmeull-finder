package com.omerkaya.sperrmuellfinder.domain.model

/**
 * Post status enum for SperrmüllFinder
 * Represents the current state of a post in the system
 * 
 * Values are compatible with FirestoreConstants.FIELD_STATUS
 * Rules.md compliant - Domain layer enum with Firestore string mapping
 */
enum class PostStatus(val value: String) {
    /**
     * Post is active and visible to users
     * Firestore value: "active"
     */
    ACTIVE("active"),
    
    /**
     * Post has been archived (after 72 hours or manually)
     * Firestore value: "archived"
     */
    ARCHIVED("archived"),
    
    /**
     * Post has been removed by moderator or user
     * Firestore value: "removed"
     */
    REMOVED("removed"),
    
    /**
     * Post is pending moderation review
     * Firestore value: "pending"
     */
    PENDING("pending"),
    
    /**
     * Post has been reported and is under review
     * Firestore value: "reported"
     */
    REPORTED("reported");
    
    companion object {
        /**
         * Convert string value from Firestore to PostStatus enum
         * @param value String value from Firestore document (FirestoreConstants.FIELD_STATUS)
         * @return PostStatus enum or ACTIVE as default fallback
         */
        fun fromString(value: String?): PostStatus {
            return values().find { it.value == value } ?: ACTIVE
        }
        
        /**
         * Get all status values as strings for Firestore queries
         * @return List of all possible status string values
         */
        fun getAllValues(): List<String> {
            return values().map { it.value }
        }
    }
}
