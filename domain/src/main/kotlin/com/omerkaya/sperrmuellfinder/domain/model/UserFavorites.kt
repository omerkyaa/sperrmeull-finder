package com.omerkaya.sperrmuellfinder.domain.model

/**
 * Domain model for User favorites
 * Rules.md compliant - Clean Architecture domain layer
 */
data class UserFavorites(
    val regions: List<String> = emptyList(),
    val categories: List<String> = emptyList()
)
