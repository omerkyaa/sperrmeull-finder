package com.omerkaya.sperrmuellfinder.domain.model

/**
 * Admin role enum
 * Defines different levels of administrative access
 */
enum class AdminRole {
    SUPER_ADMIN,  // Full access - can manage other admins
    ADMIN,        // Most operations - cannot manage admins
    MODERATOR;    // Limited to content moderation
    
    /**
     * Check if this role has a specific permission
     */
    fun hasPermission(permission: AdminPermission): Boolean {
        return when (this) {
            SUPER_ADMIN -> true // Super admin has all permissions
            ADMIN -> permission != AdminPermission.MANAGE_ADMINS
            MODERATOR -> permission in listOf(
                AdminPermission.VIEW_REPORTS,
                AdminPermission.MODERATE_CONTENT,
                AdminPermission.BAN_USER
            )
        }
    }
    
    /**
     * Get display name for this role
     */
    fun getDisplayName(isGerman: Boolean = true): String {
        return when (this) {
            SUPER_ADMIN -> if (isGerman) "Super Administrator" else "Super Administrator"
            ADMIN -> if (isGerman) "Administrator" else "Administrator"
            MODERATOR -> if (isGerman) "Moderator" else "Moderator"
        }
    }
}

/**
 * Admin permissions enum
 * Defines specific actions that can be performed
 */
enum class AdminPermission {
    VIEW_REPORTS,           // View user reports
    MODERATE_CONTENT,       // Moderate posts and comments
    BAN_USER,              // Ban/unban users
    DELETE_POST,           // Delete posts
    DELETE_COMMENT,        // Delete comments
    MANAGE_PREMIUM,        // Grant/revoke premium
    SEND_NOTIFICATIONS,    // Send broadcast notifications
    MANAGE_ADMINS,         // Add/remove admin roles
    VIEW_ANALYTICS;        // View dashboard analytics
    
    /**
     * Get display name for this permission
     */
    fun getDisplayName(isGerman: Boolean = true): String {
        return when (this) {
            VIEW_REPORTS -> if (isGerman) "Berichte ansehen" else "View Reports"
            MODERATE_CONTENT -> if (isGerman) "Inhalte moderieren" else "Moderate Content"
            BAN_USER -> if (isGerman) "Benutzer sperren" else "Ban Users"
            DELETE_POST -> if (isGerman) "Beiträge löschen" else "Delete Posts"
            DELETE_COMMENT -> if (isGerman) "Kommentare löschen" else "Delete Comments"
            MANAGE_PREMIUM -> if (isGerman) "Premium verwalten" else "Manage Premium"
            SEND_NOTIFICATIONS -> if (isGerman) "Benachrichtigungen senden" else "Send Notifications"
            MANAGE_ADMINS -> if (isGerman) "Admins verwalten" else "Manage Admins"
            VIEW_ANALYTICS -> if (isGerman) "Analysen ansehen" else "View Analytics"
        }
    }
}
