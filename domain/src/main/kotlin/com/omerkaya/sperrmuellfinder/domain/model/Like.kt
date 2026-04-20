package com.omerkaya.sperrmuellfinder.domain.model

import java.util.Date

/**
 * 💖 LIKE DOMAIN MODEL - SperrmüllFinder
 * Real-time Firebase likes with denormalized user data
 * Rules.md compliant - Clean Architecture domain entity
 */
data class Like(
    val id: String,
    val postId: String,
    val userId: String,
    val username: String,
    val userPhotoUrl: String?,
    val userLevel: Int,
    val userIsPremium: Boolean,
    val createdAt: Date,
    val updatedAt: Date
) {
    companion object {
        fun empty() = Like(
            id = "",
            postId = "",
            userId = "",
            username = "",
            userPhotoUrl = null,
            userLevel = 1,
            userIsPremium = false,
            createdAt = Date(),
            updatedAt = Date()
        )
    }
}