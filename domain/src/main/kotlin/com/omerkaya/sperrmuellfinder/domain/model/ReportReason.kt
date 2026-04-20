package com.omerkaya.sperrmuellfinder.domain.model

/**
 * Enum for report reasons
 * Used for reporting posts, comments, and users
 * 
 * Rules.md compliant - Professional enum with display names
 * for reporting system implementation
 */
enum class ReportReason(
    val displayName: String,
    val description: String
) {
    SPAM(
        displayName = "Spam",
        description = "Unwanted commercial content or repetitive messages"
    ),
    INAPPROPRIATE_CONTENT(
        displayName = "Inappropriate Content", 
        description = "Content that violates community guidelines"
    ),
    HARASSMENT(
        displayName = "Harassment",
        description = "Bullying, harassment, or abusive behavior"
    ),
    MISINFORMATION(
        displayName = "Misinformation",
        description = "False or misleading information"
    ),
    COPYRIGHT(
        displayName = "Copyright Violation",
        description = "Unauthorized use of copyrighted material"
    ),
    DANGEROUS_GOODS(
        displayName = "Dangerous Items",
        description = "Posting dangerous or illegal items"
    ),
    SCAM(
        displayName = "Scam",
        description = "Fraudulent or deceptive content"
    ),
    OTHER(
        displayName = "Other",
        description = "Other violation not covered above"
    )
}