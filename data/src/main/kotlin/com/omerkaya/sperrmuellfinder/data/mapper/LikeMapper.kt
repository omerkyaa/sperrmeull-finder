package com.omerkaya.sperrmuellfinder.data.mapper

import com.omerkaya.sperrmuellfinder.data.dto.LikeDto
import com.omerkaya.sperrmuellfinder.domain.model.Like

/**
 * 💖 LIKE MAPPER - SperrmüllFinder
 * Maps between Like DTO and Domain model
 * Rules.md compliant - Clean Architecture data mapping
 */
object LikeMapper {
    
    /**
     * Convert LikeDto to Like domain model
     */
    fun LikeDto.toDomain(): Like {
        return Like(
            id = "${this.postId}_${this.userId}", // Generate consistent ID
            postId = this.postId,
            userId = this.userId,
            username = this.username,
            userPhotoUrl = this.userPhotoUrl,
            userLevel = this.userLevel,
            userIsPremium = this.userIsPremium,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
    
    /**
     * Convert Like domain model to LikeDto
     */
    fun Like.toDto(): LikeDto {
        return LikeDto(
            postId = this.postId,
            userId = this.userId,
            username = this.username,
            userPhotoUrl = this.userPhotoUrl,
            userLevel = this.userLevel,
            userIsPremium = this.userIsPremium,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
    
    /**
     * Convert list of LikeDto to list of Like domain models
     */
    fun List<LikeDto>.toDomain(): List<Like> {
        return this.map { it.toDomain() }
    }
}