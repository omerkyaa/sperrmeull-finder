package com.omerkaya.sperrmuellfinder.data.mapper

import com.google.firebase.firestore.DocumentSnapshot
import com.omerkaya.sperrmuellfinder.data.dto.AdminLogDto
import com.omerkaya.sperrmuellfinder.data.dto.AdminRoleDto
import com.omerkaya.sperrmuellfinder.data.dto.ModerationQueueDto
import com.omerkaya.sperrmuellfinder.domain.model.AdminAction
import com.omerkaya.sperrmuellfinder.domain.model.AdminLog
import com.omerkaya.sperrmuellfinder.domain.model.AdminRole
import com.omerkaya.sperrmuellfinder.domain.model.AdminTargetType
import com.omerkaya.sperrmuellfinder.domain.model.ModerationItemType
import com.omerkaya.sperrmuellfinder.domain.model.ModerationPriority
import com.omerkaya.sperrmuellfinder.domain.model.ModerationQueue
import com.omerkaya.sperrmuellfinder.domain.model.ModerationQueueEntry
import com.omerkaya.sperrmuellfinder.domain.model.ModerationStatus
import com.omerkaya.sperrmuellfinder.domain.model.ModerationType
import com.omerkaya.sperrmuellfinder.domain.model.ReportPriority
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType

/**
 * Map Firestore document to AdminRole
 */
fun DocumentSnapshot.toAdminRole(): AdminRole? {
    val roleString = getString("role") ?: return null
    return when (roleString.lowercase()) {
        "super_admin" -> AdminRole.SUPER_ADMIN
        "admin" -> AdminRole.ADMIN
        "moderator" -> AdminRole.MODERATOR
        else -> null
    }
}

/**
 * Map AdminRole to string for Firestore
 */
fun AdminRole.toFirestoreString(): String {
    return when (this) {
        AdminRole.SUPER_ADMIN -> "super_admin"
        AdminRole.ADMIN -> "admin"
        AdminRole.MODERATOR -> "moderator"
    }
}

/**
 * Map AdminRoleDto to domain model
 */
fun AdminRoleDto.toDomain(): AdminRole? {
    return when (role.lowercase()) {
        "super_admin" -> AdminRole.SUPER_ADMIN
        "admin" -> AdminRole.ADMIN
        "moderator" -> AdminRole.MODERATOR
        else -> null
    }
}

/**
 * Map AdminLogDto to domain model
 */
fun AdminLogDto.toDomain(): AdminLog {
    return AdminLog(
        id = id,
        adminId = adminId,
        adminName = null, // Will be fetched separately if needed
        action = mapStringToAdminAction(action),
        targetType = mapStringToAdminTargetType(targetType),
        targetId = targetId,
        reason = reason,
        metadata = metadata,
        timestamp = timestamp
    )
}

/**
 * Map DocumentSnapshot to AdminLog
 */
fun DocumentSnapshot.toAdminLog(): AdminLog {
    return AdminLog(
        id = id,
        adminId = getString("adminId") ?: "",
        adminName = getString("adminName"),
        action = mapStringToAdminAction(getString("action") ?: ""),
        targetType = mapStringToAdminTargetType(getString("targetType") ?: ""),
        targetId = getString("targetId") ?: "",
        reason = getString("reason") ?: "",
        metadata = get("metadata") as? Map<String, Any> ?: emptyMap(),
        timestamp = getDate("timestamp") ?: java.util.Date()
    )
}

/**
 * Map ModerationQueueDto to domain model
 */
fun ModerationQueueDto.toDomain(): ModerationQueue {
    return ModerationQueue(
        id = id,
        type = mapStringToModerationType(type),
        priority = mapStringToModerationPriority(priority),
        reportId = reportId,
        targetType = mapStringToReportTargetType(targetType),
        targetId = targetId,
        status = mapStringToModerationStatus(status),
        assignedTo = assignedTo,
        assignedToName = null, // Will be fetched separately if needed
        createdAt = createdAt,
        resolvedAt = resolvedAt,
        resolvedBy = resolvedBy,
        resolvedByName = null, // Will be fetched separately if needed
        targetContent = targetContent,
        targetImageUrl = targetImageUrl,
        targetOwnerId = targetOwnerId,
        targetOwnerName = null // Will be fetched separately if needed
    )
}

/**
 * Map DocumentSnapshot to ModerationQueue
 */
fun DocumentSnapshot.toModerationQueue(): ModerationQueue {
    return ModerationQueue(
        id = id,
        type = mapStringToModerationType(getString("type") ?: ""),
        priority = mapStringToModerationPriority(getString("priority") ?: ""),
        reportId = getString("reportId"),
        targetType = mapStringToReportTargetType(getString("targetType") ?: ""),
        targetId = getString("targetId") ?: "",
        status = mapStringToModerationStatus(getString("status") ?: ""),
        assignedTo = getString("assignedTo"),
        assignedToName = getString("assignedToName"),
        createdAt = getDate("createdAt") ?: java.util.Date(),
        resolvedAt = getDate("resolvedAt"),
        resolvedBy = getString("resolvedBy"),
        resolvedByName = getString("resolvedByName"),
        targetContent = getString("targetContent"),
        targetImageUrl = getString("targetImageUrl"),
        targetOwnerId = getString("targetOwnerId"),
        targetOwnerName = getString("targetOwnerName")
    )
}

/**
 * Map DocumentSnapshot to ModerationQueueEntry
 */
fun DocumentSnapshot.toModerationQueueEntry(): ModerationQueueEntry {
    val targetTypeStr = getString("targetType") ?: "post"
    val priorityStr = getString("priority") ?: "medium"
    
    return ModerationQueueEntry(
        id = id,
        type = mapStringToModerationItemType(targetTypeStr),
        targetId = getString("targetId") ?: "",
        targetOwnerId = getString("targetOwnerId") ?: "",
        targetOwnerName = getString("targetOwnerName") ?: "Unknown",
        targetContent = getString("targetContent"),
        targetImageUrl = getString("targetImageUrl"),
        reportCount = getLong("reportCount")?.toInt() ?: 1,
        priority = mapStringToReportPriority(priorityStr),
        createdAt = getDate("createdAt") ?: java.util.Date(),
        lastReportedAt = getDate("lastReportedAt") ?: getDate("createdAt") ?: java.util.Date(),
        reasons = (get("reasons") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    )
}

// ========================================
// HELPER MAPPING FUNCTIONS
// ========================================

private fun mapStringToAdminAction(action: String): AdminAction {
    return when (action.lowercase()) {
        "set_admin_role" -> AdminAction.SET_ADMIN_ROLE
        "remove_admin_role" -> AdminAction.REMOVE_ADMIN_ROLE
        "ban_user" -> AdminAction.BAN_USER
        "unban_user" -> AdminAction.UNBAN_USER
        "delete_post" -> AdminAction.DELETE_POST
        "delete_comment" -> AdminAction.DELETE_COMMENT
        "grant_premium" -> AdminAction.GRANT_PREMIUM
        "revoke_premium" -> AdminAction.REVOKE_PREMIUM
        "send_notification" -> AdminAction.SEND_NOTIFICATION
        "resolve_report" -> AdminAction.RESOLVE_REPORT
        "dismiss_report" -> AdminAction.DISMISS_REPORT
        "adjust_honesty" -> AdminAction.ADJUST_HONESTY
        else -> AdminAction.RESOLVE_REPORT // Default
    }
}

private fun mapStringToAdminTargetType(targetType: String): AdminTargetType {
    return when (targetType.lowercase()) {
        "user" -> AdminTargetType.USER
        "post" -> AdminTargetType.POST
        "comment" -> AdminTargetType.COMMENT
        "report" -> AdminTargetType.REPORT
        "system" -> AdminTargetType.SYSTEM
        else -> AdminTargetType.SYSTEM // Default
    }
}

private fun mapStringToModerationType(type: String): ModerationType {
    return when (type.lowercase()) {
        "report" -> ModerationType.REPORT
        "auto_flag" -> ModerationType.AUTO_FLAG
        else -> ModerationType.REPORT // Default
    }
}

private fun mapStringToModerationPriority(priority: String): ModerationPriority {
    return when (priority.lowercase()) {
        "low" -> ModerationPriority.LOW
        "medium" -> ModerationPriority.MEDIUM
        "high" -> ModerationPriority.HIGH
        "critical" -> ModerationPriority.CRITICAL
        else -> ModerationPriority.MEDIUM // Default
    }
}

private fun mapStringToModerationStatus(status: String): ModerationStatus {
    return when (status.lowercase()) {
        "pending" -> ModerationStatus.PENDING
        "in_review" -> ModerationStatus.IN_REVIEW
        "resolved" -> ModerationStatus.RESOLVED
        "dismissed" -> ModerationStatus.DISMISSED
        else -> ModerationStatus.PENDING // Default
    }
}

private fun mapStringToReportTargetType(targetType: String): ReportTargetType {
    return when (targetType.lowercase()) {
        "post" -> ReportTargetType.POST
        "comment" -> ReportTargetType.COMMENT
        "user" -> ReportTargetType.USER
        else -> ReportTargetType.POST // Default
    }
}

private fun mapStringToModerationItemType(type: String): ModerationItemType {
    return when (type.lowercase()) {
        "post" -> ModerationItemType.POST
        "comment" -> ModerationItemType.COMMENT
        "user" -> ModerationItemType.USER
        else -> ModerationItemType.POST // Default
    }
}

private fun mapStringToReportPriority(priority: String): ReportPriority {
    return when (priority.lowercase()) {
        "low" -> ReportPriority.LOW
        "medium" -> ReportPriority.MEDIUM
        "high" -> ReportPriority.HIGH
        "critical" -> ReportPriority.CRITICAL
        else -> ReportPriority.MEDIUM // Default
    }
}
