package com.omerkaya.sperrmuellfinder.domain.model

/**
 * Premium frame types for user profiles
 * Each frame type represents a different visual style that can be applied
 * to a user's profile picture or post frame.
 */
enum class PremiumFrameType {
    NONE,           // No frame (default)
    BRONZE,         // Bronze frame
    SILVER,         // Silver frame
    GOLD,           // Gold frame
    PLATINUM,       // Platinum frame
    DIAMOND,        // Diamond frame
    RAINBOW,        // Rainbow animated frame
    FIRE,           // Fire animated frame
    ICE,            // Ice animated frame
    NATURE,         // Nature themed frame
    URBAN;          // Urban themed frame

    companion object {
        /**
         * Get frame color code
         */
        fun getColorCode(type: PremiumFrameType): String {
            return when (type) {
                NONE -> "#00000000" // Transparent
                BRONZE -> "#CD7F32"
                SILVER -> "#C0C0C0"
                GOLD -> "#FFD700"
                PLATINUM -> "#E5E4E2"
                DIAMOND -> "#B9F2FF"
                RAINBOW -> "#FF1493" // Deep pink for base color
                FIRE -> "#FF4500"    // Orange red
                ICE -> "#00CED1"     // Dark turquoise
                NATURE -> "#228B22"   // Forest green
                URBAN -> "#4B0082"    // Indigo
            }
        }

        /**
         * Get frame name in German
         */
        fun getNameDe(type: PremiumFrameType): String {
            return when (type) {
                NONE -> "Kein Rahmen"
                BRONZE -> "Bronze"
                SILVER -> "Silber"
                GOLD -> "Gold"
                PLATINUM -> "Platin"
                DIAMOND -> "Diamant"
                RAINBOW -> "Regenbogen"
                FIRE -> "Feuer"
                ICE -> "Eis"
                NATURE -> "Natur"
                URBAN -> "Urban"
            }
        }

        /**
         * Get frame name in English
         */
        fun getNameEn(type: PremiumFrameType): String {
            return when (type) {
                NONE -> "No Frame"
                BRONZE -> "Bronze"
                SILVER -> "Silver"
                GOLD -> "Gold"
                PLATINUM -> "Platinum"
                DIAMOND -> "Diamond"
                RAINBOW -> "Rainbow"
                FIRE -> "Fire"
                ICE -> "Ice"
                NATURE -> "Nature"
                URBAN -> "Urban"
            }
        }

        /**
         * Check if frame is animated
         */
        fun isAnimated(type: PremiumFrameType): Boolean {
            return when (type) {
                RAINBOW, FIRE, ICE -> true
                else -> false
            }
        }

        /**
         * Get required user level for frame
         */
        fun getRequiredLevel(type: PremiumFrameType): Int {
            return when (type) {
                NONE -> 0
                BRONZE -> 5
                SILVER -> 10
                GOLD -> 20
                PLATINUM -> 30
                DIAMOND -> 50
                RAINBOW -> 75
                FIRE -> 60
                ICE -> 60
                NATURE -> 40
                URBAN -> 40
            }
        }
    }
}
