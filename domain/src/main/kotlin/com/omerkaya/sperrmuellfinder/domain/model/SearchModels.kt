package com.omerkaya.sperrmuellfinder.domain.model

import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.domain.repository.PostSortBy
import java.util.Date

/**
 * Search filter configuration for posts and users
 */
data class SearchFilters(
    val query: String = "",
    val categories: List<String> = emptyList(), // English categories for backend
    val city: String? = null,
    val radiusMeters: Int = 1500, // Basic: 1500m, Premium: unlimited
    val timeRange: TimeRange = TimeRange.ALL_TIME,
    val sortBy: PostSortBy = PostSortBy.NEWEST,
    val searchType: SearchType = SearchType.POSTS,
    val userLocation: PostLocation? = null // User's current location for distance filtering
) {
    /**
     * Check if any filters are applied (excluding query and sort)
     */
    fun hasActiveFilters(): Boolean {
        return categories.isNotEmpty() || 
               city != null || 
               timeRange != TimeRange.ALL_TIME
    }
    
    /**
     * Get filter count for UI display
     */
    fun getActiveFilterCount(): Int {
        var count = 0
        if (categories.isNotEmpty()) count++
        if (city != null) count++
        if (timeRange != TimeRange.ALL_TIME) count++
        return count
    }
}

/**
 * Search type enum
 */
enum class SearchType {
    POSTS,
    USERS,
    ALL
}

/**
 * Time range filter for posts
 */
enum class TimeRange(val hours: Int) {
    LAST_24H(24),
    LAST_48H(48),
    LAST_72H(72),
    ALL_TIME(Int.MAX_VALUE);
    
    fun getDisplayName(): String {
        return when (this) {
            LAST_24H -> "Last 24 hours"
            LAST_48H -> "Last 48 hours" 
            LAST_72H -> "Last 72 hours"
            ALL_TIME -> "All time"
        }
    }
}

/**
 * Search result wrapper for mixed content
 */
sealed class SearchResult {
    data class PostResult(val post: Post) : SearchResult()
    data class UserResult(val user: User) : SearchResult()
}

/**
 * Search suggestion for autocomplete
 */
data class SearchSuggestion(
    val text: String,
    val type: SearchSuggestionType,
    val count: Int = 0 // Number of results for this suggestion
)

/**
 * Search suggestion types
 */
enum class SearchSuggestionType {
    QUERY,      // Recent search query
    CATEGORY,   // Category suggestion
    CITY,       // City suggestion
    USER        // User suggestion
}

/**
 * Search history entry
 */
data class SearchHistory(
    val id: String,
    val query: String,
    val filters: SearchFilters,
    val resultCount: Int,
    val searchedAt: Date = Date()
)

/**
 * Category with localized names
 */
data class CategoryData(
    val id: String,
    val nameEn: String,    // English name for backend
    val nameDe: String,    // German name for display
    val icon: String? = null,
    val color: String? = null,
    val isPopular: Boolean = false
) {
    companion object {
        // Popular categories for quick selection
        val POPULAR_CATEGORIES = listOf(
            CategoryData("furniture", "Furniture", "Möbel", "🪑", "#8B4513", true),
            CategoryData("electronics", "Electronics", "Elektronik", "📱", "#4169E1", true),
            CategoryData("books", "Books", "Bücher", "📚", "#228B22", true),
            CategoryData("clothing", "Clothing", "Kleidung", "👕", "#FF69B4", true),
            CategoryData("toys", "Toys", "Spielzeug", "🧸", "#FF6347", true),
            CategoryData("sports", "Sports", "Sport", "⚽", "#32CD32", true),
            CategoryData("kitchen", "Kitchen", "Küche", "🍳", "#FFD700", true),
            CategoryData("garden", "Garden", "Garten", "🌱", "#90EE90", true),
            CategoryData("decoration", "Decoration", "Dekoration", "🎨", "#DDA0DD", true),
            CategoryData("tools", "Tools", "Werkzeuge", "🔧", "#696969", true)
        )
        
        // All available categories
        val ALL_CATEGORIES = POPULAR_CATEGORIES + listOf(
            CategoryData("appliances", "Appliances", "Haushaltsgeräte", "🏠", "#4682B4"),
            CategoryData("automotive", "Automotive", "Auto & Motor", "🚗", "#DC143C"),
            CategoryData("beauty", "Beauty", "Schönheit", "💄", "#FF1493"),
            CategoryData("health", "Health", "Gesundheit", "🏥", "#00CED1"),
            CategoryData("music", "Music", "Musik", "🎵", "#9370DB"),
            CategoryData("art", "Art", "Kunst", "🎭", "#FF4500"),
            CategoryData("office", "Office", "Büro", "💼", "#2F4F4F"),
            CategoryData("baby", "Baby", "Baby", "👶", "#FFB6C1"),
            CategoryData("pet", "Pet", "Haustier", "🐕", "#8FBC8F"),
            CategoryData("other", "Other", "Sonstiges", "📦", "#A9A9A9")
        )
    }
}

/**
 * German cities for location filter
 */
object GermanCities {
    val MAJOR_CITIES = listOf(
        "Berlin", "Hamburg", "München", "Köln", "Frankfurt am Main",
        "Stuttgart", "Düsseldorf", "Dortmund", "Essen", "Leipzig",
        "Bremen", "Dresden", "Hannover", "Nürnberg", "Duisburg",
        "Bochum", "Wuppertal", "Bielefeld", "Bonn", "Münster",
        "Karlsruhe", "Mannheim", "Augsburg", "Wiesbaden", "Gelsenkirchen",
        "Mönchengladbach", "Braunschweig", "Chemnitz", "Kiel", "Aachen"
    )
    
    val ALL_CITIES = MAJOR_CITIES + listOf(
        "Halle (Saale)", "Magdeburg", "Freiburg im Breisgau", "Krefeld", "Lübeck",
        "Oberhausen", "Erfurt", "Mainz", "Rostock", "Kassel",
        "Hagen", "Hamm", "Saarbrücken", "Mülheim an der Ruhr", "Potsdam",
        "Ludwigshafen am Rhein", "Oldenburg", "Leverkusen", "Osnabrück", "Solingen",
        "Heidelberg", "Herne", "Neuss", "Darmstadt", "Paderborn",
        "Regensburg", "Ingolstadt", "Würzburg", "Fürth", "Wolfsburg",
        "Offenbach am Main", "Ulm", "Heilbronn", "Pforzheim", "Göttingen",
        "Bottrop", "Trier", "Recklinghausen", "Reutlingen", "Bremerhaven"
    )
}

/**
 * Search analytics data
 */
data class SearchAnalytics(
    val query: String,
    val filters: SearchFilters,
    val resultCount: Int,
    val searchDuration: Long, // milliseconds
    val clickedResults: List<String> = emptyList(), // clicked post/user IDs
    val timestamp: Date = Date()
)
