package com.omerkaya.sperrmuellfinder.core.util

import java.util.Locale

/**
 * Location formatting utilities for SperrmüllFinder app.
 * Rules.md compliant - Professional location formatting with localization support.
 */
object LocationFormatters {

    /**
     * Format street and city for display in post cards
     * Handles missing fields gracefully with fallback strings
     * 
     * @param street Street name (Strasse)
     * @param city City name (Stadt)
     * @param locale Device locale for formatting
     * @return Formatted location string or localized unknown location
     */
    fun formatStreetCity(
        street: String?, 
        city: String?, 
        locale: Locale = Locale.getDefault()
    ): String {
        val cleanStreet = street?.trim()?.takeIf { it.isNotBlank() }
        val cleanCity = city?.trim()?.takeIf { it.isNotBlank() }
        
        return when {
            // Both street and city available
            cleanStreet != null && cleanCity != null -> "$cleanStreet, $cleanCity"
            
            // Only street available
            cleanStreet != null -> cleanStreet
            
            // Only city available  
            cleanCity != null -> cleanCity
            
            // Neither available - return localized unknown location
            else -> {
                if (isGermanLocale(locale)) {
                    "Unbekannter Ort"
                } else {
                    "Unknown location"
                }
            }
        }
    }

    /**
     * Format location with distance for display
     * 
     * @param street Street name
     * @param city City name
     * @param distanceMeters Distance in meters (optional)
     * @param locale Device locale for formatting
     * @return Formatted location with distance
     */
    fun formatLocationWithDistance(
        street: String?,
        city: String?,
        distanceMeters: Double?,
        locale: Locale = Locale.getDefault()
    ): String {
        val locationPart = formatStreetCity(street, city, locale)
        val distancePart = distanceMeters?.let { formatDistance(it, locale) }
        
        return if (distancePart != null) {
            "$locationPart • $distancePart"
        } else {
            locationPart
        }
    }

    /**
     * Format distance for display
     * 
     * @param distanceMeters Distance in meters
     * @param locale Device locale for formatting
     * @return Formatted distance string (e.g., "150m" or "2.3km")
     */
    fun formatDistance(distanceMeters: Double, locale: Locale = Locale.getDefault()): String {
        return when {
            distanceMeters < 1000 -> "${distanceMeters.toInt()}m"
            distanceMeters < 10000 -> {
                val km = distanceMeters / 1000
                if (isGermanLocale(locale)) {
                    "${String.format(locale, "%.1f", km)}km"
                } else {
                    "${String.format(locale, "%.1f", km)}km"
                }
            }
            else -> "${(distanceMeters / 1000).toInt()}km"
        }
    }

    /**
     * Format full address for display
     * 
     * @param street Street name
     * @param city City name
     * @param country Country name
     * @param locale Device locale for formatting
     * @return Formatted full address
     */
    fun formatFullAddress(
        street: String?,
        city: String?,
        country: String?,
        locale: Locale = Locale.getDefault()
    ): String {
        val parts = listOfNotNull(
            street?.trim()?.takeIf { it.isNotBlank() },
            city?.trim()?.takeIf { it.isNotBlank() },
            country?.trim()?.takeIf { it.isNotBlank() }
        )
        
        return when {
            parts.isNotEmpty() -> parts.joinToString(", ")
            else -> {
                if (isGermanLocale(locale)) {
                    "Unbekannte Adresse"
                } else {
                    "Unknown address"
                }
            }
        }
    }

    /**
     * Format city for display (handles multiple city formats)
     * 
     * @param city City name (could include state/region)
     * @param locale Device locale for formatting
     * @return Formatted city name
     */
    fun formatCity(city: String?, locale: Locale = Locale.getDefault()): String {
        val cleanCity = city?.trim()?.takeIf { it.isNotBlank() }
        
        return cleanCity ?: run {
            if (isGermanLocale(locale)) {
                "Unbekannte Stadt"
            } else {
                "Unknown city"
            }
        }
    }

    /**
     * Get short location for display in compact spaces
     * Prioritizes city over street for brevity
     * 
     * @param street Street name
     * @param city City name
     * @param locale Device locale for formatting
     * @return Short location string
     */
    fun formatShortLocation(
        street: String?,
        city: String?,
        locale: Locale = Locale.getDefault()
    ): String {
        val cleanCity = city?.trim()?.takeIf { it.isNotBlank() }
        val cleanStreet = street?.trim()?.takeIf { it.isNotBlank() }
        
        return when {
            // Prefer city for short display
            cleanCity != null -> cleanCity
            cleanStreet != null -> cleanStreet
            else -> {
                if (isGermanLocale(locale)) {
                    "Unbekannt"
                } else {
                    "Unknown"
                }
            }
        }
    }

    /**
     * Check if coordinates are valid
     */
    fun areCoordinatesValid(latitude: Double?, longitude: Double?): Boolean {
        return latitude != null && longitude != null &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    /**
     * Format coordinates for display
     */
    fun formatCoordinates(latitude: Double, longitude: Double): String {
        return "${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}"
    }

    /**
     * Check if locale is German
     */
    private fun isGermanLocale(locale: Locale): Boolean {
        return locale.language.equals("de", ignoreCase = true)
    }

    /**
     * Get localized unknown location string
     */
    fun getUnknownLocationString(locale: Locale = Locale.getDefault()): String {
        return if (isGermanLocale(locale)) {
            "Unbekannter Ort"
        } else {
            "Unknown location"
        }
    }

    /**
     * Get localized distance unit
     */
    fun getDistanceUnit(locale: Locale = Locale.getDefault()): String {
        // Germany uses metric system
        return "km"
    }
}
