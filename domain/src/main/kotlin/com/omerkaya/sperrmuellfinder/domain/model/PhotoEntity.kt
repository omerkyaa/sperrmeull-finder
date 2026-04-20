package com.omerkaya.sperrmuellfinder.domain.model

import android.net.Uri
import java.util.Date

/**
 * Domain entity representing a captured photo
 * Rules.md compliant - Android-compatible timestamp using Date instead of LocalDateTime
 */
data class PhotoEntity(
    val id: String,
    val localUri: Uri,
    val remoteUrl: String? = null,
    val captureTimestamp: Date,
    val processingStatus: ProcessingStatus = ProcessingStatus.PENDING,
    val uploadStatus: UploadStatus = UploadStatus.PENDING
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as PhotoEntity
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
}

/**
 * Photo processing status
 */
enum class ProcessingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

/**
 * Photo upload status
 */
enum class UploadStatus {
    PENDING,
    UPLOADING,
    COMPLETED,
    FAILED
}