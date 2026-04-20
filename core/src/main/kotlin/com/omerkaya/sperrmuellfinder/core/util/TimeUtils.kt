package com.omerkaya.sperrmuellfinder.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {

    /**
     * Format timestamp for post age display
     */
    fun formatTimeAgo(timestamp: Long, isGerman: Boolean = true): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        
        return when {
            seconds < 60 -> if (isGerman) "Gerade eben" else "Just now"
            minutes < 60 -> if (isGerman) "${minutes}m" else "${minutes}m"
            hours < 24 -> if (isGerman) "${hours}h" else "${hours}h"
            days < 7 -> if (isGerman) "${days}d" else "${days}d"
            days < 30 -> {
                val weeks = days / 7
                if (isGerman) "${weeks}w" else "${weeks}w"
            }
            else -> {
                val months = days / 30
                if (isGerman) "${months}M" else "${months}M"
            }
        }
    }

    /**
     * Format full date for display
     */
    fun formatFullDate(timestamp: Long, isGerman: Boolean = true): String {
        val locale = if (isGerman) Locale.GERMAN else Locale.ENGLISH
        val format = if (isGerman) {
            SimpleDateFormat("dd.MM.yyyy HH:mm", locale)
        } else {
            SimpleDateFormat("MMM dd, yyyy HH:mm", locale)
        }
        return format.format(Date(timestamp))
    }

    /**
     * Format date only
     */
    fun formatDate(timestamp: Long, isGerman: Boolean = true): String {
        val locale = if (isGerman) Locale.GERMAN else Locale.ENGLISH
        val format = if (isGerman) {
            SimpleDateFormat("dd.MM.yyyy", locale)
        } else {
            SimpleDateFormat("MMM dd, yyyy", locale)
        }
        return format.format(Date(timestamp))
    }

    /**
     * Format time only
     */
    fun formatTime(timestamp: Long): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(Date(timestamp))
    }

    /**
     * Get post expiry time (72 hours from creation)
     */
    fun getPostExpiryTime(createdAt: Long): Long {
        return createdAt + TimeUnit.HOURS.toMillis(72)
    }

    /**
     * Check if post is expired
     */
    fun isPostExpired(createdAt: Long): Boolean {
        return System.currentTimeMillis() > getPostExpiryTime(createdAt)
    }

    /**
     * Get remaining time until post expires
     */
    fun getRemainingTime(createdAt: Long, isGerman: Boolean = true): String {
        val expiryTime = getPostExpiryTime(createdAt)
        val remaining = expiryTime - System.currentTimeMillis()
        
        if (remaining <= 0) {
            return if (isGerman) "Abgelaufen" else "Expired"
        }
        
        val hours = TimeUnit.MILLISECONDS.toHours(remaining)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining % TimeUnit.HOURS.toMillis(1))
        
        return when {
            hours > 0 -> if (isGerman) "${hours}h ${minutes}m" else "${hours}h ${minutes}m"
            else -> if (isGerman) "${minutes}m" else "${minutes}m"
        }
    }

    /**
     * Get start of day timestamp
     */
    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Get end of day timestamp
     */
    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * Check if timestamp is today
     */
    fun isToday(timestamp: Long): Boolean {
        val today = getStartOfDay()
        val tomorrow = today + TimeUnit.DAYS.toMillis(1)
        return timestamp in today until tomorrow
    }

    /**
     * Check if timestamp is this week
     */
    fun isThisWeek(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)
        
        calendar.timeInMillis = timestamp
        val timestampWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        val timestampYear = calendar.get(Calendar.YEAR)
        
        return currentWeek == timestampWeek && currentYear == timestampYear
    }
}
