package com.omerkaya.sperrmuellfinder.data.dto

import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * Admin role DTO for Firestore mapping
 */
data class AdminRoleDto(
    @PropertyName("role") val role: String = "",
    @PropertyName("grantedBy") val grantedBy: String = "",
    @PropertyName("grantedAt") val grantedAt: Date = Date(),
    @PropertyName("permissions") val permissions: List<String> = emptyList()
)

/**
 * Admin log DTO for Firestore mapping
 */
data class AdminLogDto(
    @PropertyName("id") val id: String = "",
    @PropertyName("adminId") val adminId: String = "",
    @PropertyName("action") val action: String = "",
    @PropertyName("targetType") val targetType: String = "",
    @PropertyName("targetId") val targetId: String = "",
    @PropertyName("reason") val reason: String = "",
    @PropertyName("metadata") val metadata: Map<String, Any> = emptyMap(),
    @PropertyName("timestamp") val timestamp: Date = Date()
)

/**
 * Moderation queue DTO for Firestore mapping
 */
data class ModerationQueueDto(
    @PropertyName("id") val id: String = "",
    @PropertyName("type") val type: String = "", // "report" or "auto_flag"
    @PropertyName("priority") val priority: String = "", // "low", "medium", "high", "critical"
    @PropertyName("reportId") val reportId: String? = null,
    @PropertyName("targetType") val targetType: String = "", // "post", "comment", "user"
    @PropertyName("targetId") val targetId: String = "",
    @PropertyName("status") val status: String = "", // "pending", "in_review", "resolved", "dismissed"
    @PropertyName("assignedTo") val assignedTo: String? = null,
    @PropertyName("createdAt") val createdAt: Date = Date(),
    @PropertyName("resolvedAt") val resolvedAt: Date? = null,
    @PropertyName("resolvedBy") val resolvedBy: String? = null,
    // Denormalized data
    @PropertyName("targetContent") val targetContent: String? = null,
    @PropertyName("targetImageUrl") val targetImageUrl: String? = null,
    @PropertyName("targetOwnerId") val targetOwnerId: String? = null
)
