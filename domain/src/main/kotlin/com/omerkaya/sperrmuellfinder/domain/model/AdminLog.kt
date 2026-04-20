package com.omerkaya.sperrmuellfinder.domain.model

import java.util.Date

/**
 * Admin log entry
 * Records all administrative actions for audit trail
 */
data class AdminLog(
    val id: String,
    val adminId: String,
    val adminName: String?,
    val action: AdminAction,
    val targetType: AdminTargetType,
    val targetId: String,
    val reason: String,
    val metadata: Map<String, Any>,
    val timestamp: Date
) {
    /**
     * Get formatted description of the action
     */
    fun getDescription(isGerman: Boolean = true): String {
        val actionName = action.getDisplayName(isGerman)
        val targetName = targetType.getDisplayName(isGerman)
        return if (isGerman) {
            "$actionName auf $targetName: $reason"
        } else {
            "$actionName on $targetName: $reason"
        }
    }
}

/**
 * Admin action types
 */
enum class AdminAction {
    SET_ADMIN_ROLE,
    REMOVE_ADMIN_ROLE,
    BAN_USER,
    UNBAN_USER,
    DELETE_POST,
    DELETE_COMMENT,
    GRANT_PREMIUM,
    REVOKE_PREMIUM,
    SEND_NOTIFICATION,
    RESOLVE_REPORT,
    DISMISS_REPORT,
    ADJUST_HONESTY;
    
    fun getDisplayName(isGerman: Boolean = true): String {
        return when (this) {
            SET_ADMIN_ROLE -> if (isGerman) "Admin-Rolle gesetzt" else "Set Admin Role"
            REMOVE_ADMIN_ROLE -> if (isGerman) "Admin-Rolle entfernt" else "Remove Admin Role"
            BAN_USER -> if (isGerman) "Benutzer gesperrt" else "Ban User"
            UNBAN_USER -> if (isGerman) "Benutzer entsperrt" else "Unban User"
            DELETE_POST -> if (isGerman) "Beitrag gelöscht" else "Delete Post"
            DELETE_COMMENT -> if (isGerman) "Kommentar gelöscht" else "Delete Comment"
            GRANT_PREMIUM -> if (isGerman) "Premium gewährt" else "Grant Premium"
            REVOKE_PREMIUM -> if (isGerman) "Premium entzogen" else "Revoke Premium"
            SEND_NOTIFICATION -> if (isGerman) "Benachrichtigung gesendet" else "Send Notification"
            RESOLVE_REPORT -> if (isGerman) "Bericht gelöst" else "Resolve Report"
            DISMISS_REPORT -> if (isGerman) "Bericht abgelehnt" else "Dismiss Report"
            ADJUST_HONESTY -> if (isGerman) "Ehrlichkeit angepasst" else "Adjust Honesty"
        }
    }
}

/**
 * Admin target types
 */
enum class AdminTargetType {
    USER,
    POST,
    COMMENT,
    REPORT,
    SYSTEM;
    
    fun getDisplayName(isGerman: Boolean = true): String {
        return when (this) {
            USER -> if (isGerman) "Benutzer" else "User"
            POST -> if (isGerman) "Beitrag" else "Post"
            COMMENT -> if (isGerman) "Kommentar" else "Comment"
            REPORT -> if (isGerman) "Bericht" else "Report"
            SYSTEM -> if (isGerman) "System" else "System"
        }
    }
}
