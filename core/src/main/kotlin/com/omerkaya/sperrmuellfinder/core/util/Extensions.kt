package com.omerkaya.sperrmuellfinder.core.util

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

/**
 * Context Extensions
 */
fun Context.isGerman(): Boolean {
    val locale = resources.configuration.locales[0]
    return locale.language == "de"
}

fun Context.getCurrentLanguage(): String {
    return resources.configuration.locales[0].language
}

/**
 * String Extensions
 */
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidPassword(): Boolean {
    return this.length >= 6
}

/**
 * Compose Extensions
 */
@Composable
fun getLocalizedString(germanRes: Int, englishRes: Int): String {
    val context = LocalContext.current
    return if (context.isGerman()) {
        stringResource(germanRes)
    } else {
        stringResource(englishRes)
    }
}

/**
 * Distance formatting
 */
fun Double.formatDistance(): String {
    return when {
        this < 1000 -> "${this.toInt()} m"
        else -> "${"%.1f".format(this / 1000)} km"
    }
}

/**
 * Time formatting
 */
fun Long.formatTimeAgo(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        seconds < 60 -> "Gerade eben"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> "${days / 7}w"
    }
}

/**
 * Collection Extensions
 */
fun <T> List<T>.safeGet(index: Int): T? {
    return if (index in 0 until size) this[index] else null
}

// ===============================
// Additional Extensions
// ===============================

/**
 * Show toast message
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Open URL in browser
 */
fun Context.openUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    } catch (e: Exception) {
        showToast("Konnte URL nicht öffnen")
    }
}

/**
 * Share text content
 */
fun Context.shareText(text: String, title: String = "Teilen") {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, title))
    } catch (e: Exception) {
        showToast("Konnte nicht geteilt werden")
    }
}

/**
 * Enhanced email validation
 */
fun String?.isValidEmailEnhanced(): Boolean {
    if (this.isNullOrBlank()) return false
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches() && this.length <= 254
}

/**
 * Enhanced password validation
 */
fun String?.isValidPasswordEnhanced(): Boolean {
    if (this.isNullOrBlank()) return false
    return this.length >= 8 && 
           this.any { it.isDigit() } && 
           this.any { it.isLetter() } &&
           this.any { it.isUpperCase() }
}

/**
 * Calculate distance between two coordinates
 */
fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0 // Earth's radius in kilometers
    
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    
    return R * c
}

/**
 * Format timestamp to relative time with localization
 */
fun Long.toRelativeTime(context: Context): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    
    // Use context for localization
    val isGerman = context.isGerman()
    
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        seconds < 60 -> if (isGerman) "Gerade eben" else "Just now"
        minutes < 60 -> "${minutes}${if (isGerman) "m" else "m"}"
        hours < 24 -> "${hours}${if (isGerman) "h" else "h"}"
        days < 7 -> "${days}${if (isGerman) "d" else "d"}"
        else -> {
            val weeks = days / 7
            "${weeks}${if (isGerman) "w" else "w"}"
        }
    }
}

/**
 * Format number with K/M suffixes
 */
fun Int.toShortString(): String {
    return when {
        this >= 1_000_000 -> String.format("%.1fM", this / 1_000_000.0)
        this >= 1_000 -> String.format("%.1fK", this / 1_000.0)
        else -> this.toString()
    }
}

/**
 * Wrap Flow with Result for error handling
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> {
    return this
        .map<T, Result<T>> { Result.Success(it) }
        .onStart { emit(Result.Loading) }
        .catch { emit(Result.Error(it)) }
}

/**
 * Composable loading indicator
 */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Composable function to show toast
 */
@Composable
fun ShowToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    val context = LocalContext.current
    LaunchedEffect(message) {
        context.showToast(message, duration)
    }
}

/**
 * Check if coordinates are valid
 */
fun Double.isValidLatitude(): Boolean = this in -90.0..90.0
fun Double.isValidLongitude(): Boolean = this in -180.0..180.0

/**
 * Truncate string with ellipsis
 */
fun String.truncate(maxLength: Int): String {
    return if (this.length <= maxLength) this
    else "${this.take(maxLength - 3)}..."
}
