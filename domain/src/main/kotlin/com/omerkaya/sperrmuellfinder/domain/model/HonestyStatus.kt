package com.omerkaya.sperrmuellfinder.domain.model

/**
 * Honesty status levels based on honesty score
 * - EXCELLENT: 80-100 points
 * - GOOD: 60-79 points
 * - FAIR: 40-59 points
 * - WARNING: 30-39 points
 * - BANNED: <30 points
 */
enum class HonestyStatus {
    EXCELLENT,
    GOOD,
    FAIR,
    WARNING,
    BANNED;

    companion object {
        fun fromScore(score: Int): HonestyStatus {
            return when {
                score >= 80 -> EXCELLENT
                score >= 60 -> GOOD
                score >= 40 -> FAIR
                score >= 30 -> WARNING
                else -> BANNED
            }
        }
    }

    fun getColorCode(): String {
        return when (this) {
            EXCELLENT -> "#4CAF50" // Green
            GOOD -> "#8BC34A" // Light Green
            FAIR -> "#FFC107" // Amber
            WARNING -> "#FF9800" // Orange
            BANNED -> "#F44336" // Red
        }
    }

    fun getDescription(isGerman: Boolean = false): String {
        return when (this) {
            EXCELLENT -> if (isGerman) "Ausgezeichnet" else "Excellent"
            GOOD -> if (isGerman) "Gut" else "Good"
            FAIR -> if (isGerman) "Okay" else "Fair"
            WARNING -> if (isGerman) "Warnung" else "Warning"
            BANNED -> if (isGerman) "Gesperrt" else "Banned"
        }
    }
}
