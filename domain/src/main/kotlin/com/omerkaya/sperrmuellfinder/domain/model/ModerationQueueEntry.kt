package com.omerkaya.sperrmuellfinder.domain.model

import java.util.Date

/**
 * Moderation Queue Entry Model
 * Represents an item in the moderation queue
 */
data class ModerationQueueEntry(
    val id: String,
    val type: ModerationItemType,
    val targetId: String,
    val targetOwnerId: String,
    val targetOwnerName: String,
    val targetContent: String?,
    val targetImageUrl: String?,
    val reportCount: Int,
    val priority: ReportPriority,
    val createdAt: Date,
    val lastReportedAt: Date,
    val reasons: List<String>
)

/**
 * Type of item in moderation queue
 */
enum class ModerationItemType {
    POST,
    COMMENT,
    USER
}
