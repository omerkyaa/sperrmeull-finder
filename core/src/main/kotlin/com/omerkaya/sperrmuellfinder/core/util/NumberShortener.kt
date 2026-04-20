package com.omerkaya.sperrmuellfinder.core.util

/**
 * Number formatting utilities for SperrmüllFinder app.
 * Provides short format for large numbers (1.2K, 3.4M, etc.)
 * Rules.md compliant - Core utility for number display.
 */
object NumberShortener {

    /**
     * Format count numbers to short format
     * Examples: 1234 -> 1.2K, 1500000 -> 1.5M
     * 
     * @param count The number to format
     * @return Formatted string (e.g., "1.2K", "3.4M")
     */
    fun formatCount(count: Int): String {
        return when {
            count < 1000 -> count.toString()
            count < 1000000 -> {
                val thousands = count / 1000.0
                if (thousands == thousands.toInt().toDouble()) {
                    "${thousands.toInt()}K"
                } else {
                    "${String.format("%.1f", thousands)}K"
                }
            }
            count < 1000000000 -> {
                val millions = count / 1000000.0
                if (millions == millions.toInt().toDouble()) {
                    "${millions.toInt()}M"
                } else {
                    "${String.format("%.1f", millions)}M"
                }
            }
            else -> {
                val billions = count / 1000000000.0
                if (billions == billions.toInt().toDouble()) {
                    "${billions.toInt()}B"
                } else {
                    "${String.format("%.1f", billions)}B"
                }
            }
        }
    }

    /**
     * Format count with proper pluralization
     * 
     * @param count The number to format
     * @param singular Singular form (e.g., "like")
     * @param plural Plural form (e.g., "likes")
     * @return Formatted string with count and proper pluralization
     */
    fun formatCountWithPlural(count: Int, singular: String, plural: String): String {
        val formattedCount = formatCount(count)
        val word = if (count == 1) singular else plural
        return "$formattedCount $word"
    }

    /**
     * Format likes count specifically
     * 
     * @param count Number of likes
     * @return Formatted likes string
     */
    fun formatLikes(count: Int): String {
        return formatCount(count)
    }

    /**
     * Format comments count specifically
     * 
     * @param count Number of comments
     * @return Formatted comments string
     */
    fun formatComments(count: Int): String {
        return formatCount(count)
    }
}
