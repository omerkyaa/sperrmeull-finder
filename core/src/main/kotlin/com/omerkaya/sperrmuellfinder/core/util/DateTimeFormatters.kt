package com.omerkaya.sperrmuellfinder.core.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * ⏰ DATE TIME FORMATTERS - SperrmüllFinder
 * Rules.md compliant - Date formatting utilities
 */
object DateTimeFormatters {
    
    private val timeAgoFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private fun isGermanLocale(): Boolean = Locale.getDefault().language.startsWith("de", ignoreCase = true)
    
    /**
     * Format a date as "time ago" string (e.g., "2 hours ago", "1 day ago")
     */
    fun formatTimeAgo(date: Date): String {
        val now = System.currentTimeMillis()
        val time = date.time
        val diff = now - time
        val isGerman = isGermanLocale()
        
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> if (isGerman) "Gerade eben" else "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                if (isGerman) "vor ${minutes} Min." else "${minutes}m ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                if (isGerman) "vor ${hours} Std." else "${hours}h ago"
            }
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                if (isGerman) "vor ${days} T." else "${days}d ago"
            }
            else -> timeAgoFormatter.format(date)
        }
    }
    
    /**
     * Format a date as a readable string
     */
    fun formatDate(date: Date): String {
        return timeAgoFormatter.format(date)
    }
}
