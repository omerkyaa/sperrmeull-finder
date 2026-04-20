package com.omerkaya.sperrmuellfinder.domain.model

/**
 * Enum representing post categories
 * Rules.md compliant - Clean Architecture domain layer
 */
enum class Category(
    val nameResKey: String,
    val iconResKey: String,
    val colorHex: String,
    val mlKeywords: List<String>
) {
    FURNITURE(
        nameResKey = CategoryResources.FURNITURE_NAME,
        iconResKey = CategoryResources.FURNITURE_ICON,
        colorHex = "#8B4513",
        mlKeywords = listOf("chair", "table", "sofa", "bed", "cabinet", "desk", "shelf")
    ),
    ELECTRONICS(
        nameResKey = CategoryResources.ELECTRONICS_NAME,
        iconResKey = CategoryResources.ELECTRONICS_ICON,
        colorHex = "#4169E1",
        mlKeywords = listOf("computer", "tv", "phone", "radio", "speaker", "monitor", "laptop")
    ),
    APPLIANCES(
        nameResKey = CategoryResources.APPLIANCES_NAME,
        iconResKey = CategoryResources.APPLIANCES_ICON,
        colorHex = "#32CD32",
        mlKeywords = listOf("washing machine", "refrigerator", "microwave", "dishwasher", "vacuum")
    ),
    CLOTHING(
        nameResKey = CategoryResources.CLOTHING_NAME,
        iconResKey = CategoryResources.CLOTHING_ICON,
        colorHex = "#FF69B4",
        mlKeywords = listOf("shirt", "pants", "dress", "jacket", "shoes", "clothing", "textile")
    ),
    BOOKS_MEDIA(
        nameResKey = CategoryResources.BOOKS_MEDIA_NAME,
        iconResKey = CategoryResources.BOOKS_MEDIA_ICON,
        colorHex = "#DAA520",
        mlKeywords = listOf("book", "magazine", "cd", "dvd", "vinyl", "newspaper")
    ),
    TOYS_GAMES(
        nameResKey = CategoryResources.TOYS_GAMES_NAME,
        iconResKey = CategoryResources.TOYS_GAMES_ICON,
        colorHex = "#FF6347",
        mlKeywords = listOf("toy", "game", "puzzle", "doll", "ball", "bicycle", "skateboard")
    ),
    SPORTS_OUTDOOR(
        nameResKey = CategoryResources.SPORTS_OUTDOOR_NAME,
        iconResKey = CategoryResources.SPORTS_OUTDOOR_ICON,
        colorHex = "#228B22",
        mlKeywords = listOf("bicycle", "skateboard", "sports equipment", "camping", "outdoor")
    ),
    HOME_GARDEN(
        nameResKey = CategoryResources.HOME_GARDEN_NAME,
        iconResKey = CategoryResources.HOME_GARDEN_ICON,
        colorHex = "#9ACD32",
        mlKeywords = listOf("plant", "pot", "garden", "tools", "decoration", "vase")
    ),
    ART_CRAFTS(
        nameResKey = CategoryResources.ART_CRAFTS_NAME,
        iconResKey = CategoryResources.ART_CRAFTS_ICON,
        colorHex = "#9370DB",
        mlKeywords = listOf("painting", "frame", "craft", "art", "canvas", "sculpture")
    ),
    OTHER(
        nameResKey = CategoryResources.OTHER_NAME,
        iconResKey = CategoryResources.OTHER_ICON,
        colorHex = "#808080",
        mlKeywords = listOf("misc", "other", "unknown", "various")
    );

    companion object {
        /**
         * Popular categories for quick selection
         */
        val POPULAR_CATEGORIES = listOf(
            FURNITURE,
            ELECTRONICS,
            APPLIANCES,
            CLOTHING,
            BOOKS_MEDIA,
            TOYS_GAMES,
            SPORTS_OUTDOOR,
            HOME_GARDEN,
            ART_CRAFTS
        )

        /**
         * Get category by ML keyword
         */
        fun getCategoryByKeyword(keyword: String): Category? {
            return values().find { category ->
                category.mlKeywords.any { it.contains(keyword, ignoreCase = true) }
            }
        }

        /**
         * Get category from ML label
         */
        fun fromLabel(label: String): Category? {
            return getCategoryByKeyword(label)
        }
    }
}
