package com.omerkaya.sperrmuellfinder.data.dto

import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * DTO for Report entity
 * Rules.md compliant - Firestore mapping with @PropertyName
 */
data class ReportDto(
    @PropertyName("id") val id: String = "",
    @PropertyName("type") val type: String = "",
    @PropertyName("targetId") val targetId: String = "",
    @PropertyName("reporterId") val reporterId: String = "",
    @PropertyName("reporterName") val reporterName: String = "",
    @PropertyName("reporterPhotoUrl") val reporterPhotoUrl: String? = null,
    @PropertyName("reason") val reason: String = "",
    @PropertyName("description") val description: String? = null,
    @PropertyName("createdAt") val createdAt: Date = Date(),
    @PropertyName("status") val status: String = "",
    @PropertyName("priority") val priority: String = "",
    @PropertyName("assignedTo") val assignedTo: String? = null,
    @PropertyName("assignedToName") val assignedToName: String? = null,
    @PropertyName("adminNotes") val adminNotes: String? = null,
    @PropertyName("resolvedAt") val resolvedAt: Date? = null,
    @PropertyName("resolvedBy") val resolvedBy: String? = null,
    @PropertyName("resolvedByName") val resolvedByName: String? = null,
    @PropertyName("moderationQueueId") val moderationQueueId: String? = null,
    @PropertyName("targetOwnerId") val targetOwnerId: String? = null,
    @PropertyName("targetOwnerName") val targetOwnerName: String? = null,
    @PropertyName("targetContent") val targetContent: String? = null,
    @PropertyName("targetImageUrl") val targetImageUrl: String? = null
)
