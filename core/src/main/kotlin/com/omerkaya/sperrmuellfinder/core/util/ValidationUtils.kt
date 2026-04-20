package com.omerkaya.sperrmuellfinder.core.util

import android.util.Patterns
import java.util.regex.Pattern

object ValidationUtils {

    /**
     * Email validation
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Password validation - minimum 6 characters
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * Strong password validation
     */
    fun isStrongPassword(password: String): Boolean {
        if (password.length < 8) return false
        
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        
        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
    }

    /**
     * Display name validation
     */
    fun isValidDisplayName(name: String): Boolean {
        return name.isNotBlank() && name.length >= 2 && name.length <= 50
    }

    /**
     * City name validation
     */
    fun isValidCity(city: String): Boolean {
        return city.isNotBlank() && city.length >= 2 && city.length <= 100
    }

    /**
     * Post description validation
     */
    fun isValidPostDescription(description: String): Boolean {
        return description.isNotBlank() && description.length >= 10 && description.length <= 500
    }

    /**
     * Comment validation
     */
    fun isValidComment(comment: String): Boolean {
        return comment.isNotBlank() && comment.length >= 1 && comment.length <= 300
    }

    /**
     * Phone number validation (German format)
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        val germanPhonePattern = Pattern.compile(
            "^(\\+49|0)[1-9][0-9]{1,14}$"
        )
        return phone.isNotBlank() && germanPhonePattern.matcher(phone).matches()
    }

    /**
     * Age validation (minimum 13 years old)
     */
    fun isValidAge(age: Int): Boolean {
        return age in 13..120
    }

    /**
     * Category validation
     */
    fun isValidCategory(category: String): Boolean {
        val validCategories = setOf(
            "furniture", "electronics", "clothing", "books", "toys", "household", "other"
        )
        return category.lowercase() in validCategories
    }

    /**
     * Location coordinates validation
     */
    fun isValidLatitude(latitude: Double): Boolean {
        return latitude in -90.0..90.0
    }

    fun isValidLongitude(longitude: Double): Boolean {
        return longitude in -180.0..180.0
    }

    /**
     * Image file validation
     */
    fun isValidImageExtension(fileName: String): Boolean {
        val validExtensions = setOf("jpg", "jpeg", "png", "webp")
        val extension = fileName.substringAfterLast(".", "").lowercase()
        return extension in validExtensions
    }

    /**
     * Image size validation (max 10MB)
     */
    fun isValidImageSize(sizeBytes: Long): Boolean {
        val maxSizeBytes = 10 * 1024 * 1024 // 10MB
        return sizeBytes <= maxSizeBytes
    }
}
