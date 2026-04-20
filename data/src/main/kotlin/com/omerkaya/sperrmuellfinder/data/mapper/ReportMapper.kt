package com.omerkaya.sperrmuellfinder.data.mapper

import com.omerkaya.sperrmuellfinder.data.dto.ReportDto
import com.omerkaya.sperrmuellfinder.domain.model.Report
import com.omerkaya.sperrmuellfinder.domain.model.ReportPriority
import com.omerkaya.sperrmuellfinder.domain.model.ReportReason
import com.omerkaya.sperrmuellfinder.domain.model.ReportStatus
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType

/**
 * Mapper functions for Report entity
 * Rules.md compliant - Clean Architecture data layer
 */

fun ReportDto.toDomain(): Report {
    val normalizedType = type.trim().uppercase()
    val normalizedReason = reason.trim().uppercase()
    val normalizedStatus = status.trim().uppercase()
    val normalizedPriority = priority.trim().uppercase()

    // Backward-compatible mapping for legacy Firestore values
    val safeType = runCatching { ReportTargetType.valueOf(normalizedType) }
        .getOrElse { ReportTargetType.POST }
    val safeReason = runCatching { ReportReason.valueOf(normalizedReason) }
        .getOrElse { ReportReason.OTHER }
    val safeStatus = when (normalizedStatus) {
        "REJECTED" -> ReportStatus.DISMISSED
        else -> runCatching { ReportStatus.valueOf(normalizedStatus) }
            .getOrElse { ReportStatus.OPEN }
    }
    val safePriority = runCatching { ReportPriority.valueOf(normalizedPriority) }
        .getOrElse { ReportPriority.MEDIUM }

    return Report(
        id = id,
        type = safeType,
        targetId = targetId,
        reporterId = reporterId,
        reporterName = reporterName,
        reporterPhotoUrl = reporterPhotoUrl,
        reason = safeReason,
        description = description,
        createdAt = createdAt,
        status = safeStatus,
        priority = safePriority,
        assignedTo = assignedTo,
        assignedToName = assignedToName,
        adminNotes = adminNotes,
        resolvedAt = resolvedAt,
        resolvedBy = resolvedBy,
        resolvedByName = resolvedByName,
        moderationQueueId = moderationQueueId,
        targetOwnerId = targetOwnerId,
        targetOwnerName = targetOwnerName,
        targetContent = targetContent,
        targetImageUrl = targetImageUrl
    )
}

fun Report.toDto(): ReportDto {
    return ReportDto(
        id = id,
        type = type.name,
        targetId = targetId,
        reporterId = reporterId,
        reporterName = reporterName,
        reporterPhotoUrl = reporterPhotoUrl,
        reason = reason.name,
        description = description,
        createdAt = createdAt,
        status = status.name,
        priority = priority.name,
        assignedTo = assignedTo,
        assignedToName = assignedToName,
        adminNotes = adminNotes,
        resolvedAt = resolvedAt,
        resolvedBy = resolvedBy,
        resolvedByName = resolvedByName,
        moderationQueueId = moderationQueueId,
        targetOwnerId = targetOwnerId,
        targetOwnerName = targetOwnerName,
        targetContent = targetContent,
        targetImageUrl = targetImageUrl
    )
}
