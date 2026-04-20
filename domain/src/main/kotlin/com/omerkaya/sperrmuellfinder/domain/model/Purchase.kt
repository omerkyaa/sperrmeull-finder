package com.omerkaya.sperrmuellfinder.domain.model

import java.util.Date

/**
 * Domain model for purchase records
 * Rules.md compliant - Clean Architecture domain layer
 */
data class Purchase(
    val id: String,
    val userId: String,
    val productId: String,
    val purchaseToken: String,
    val revenueCatTransactionId: String,
    val purchaseTime: Date,
    val createdAt: Date
)
