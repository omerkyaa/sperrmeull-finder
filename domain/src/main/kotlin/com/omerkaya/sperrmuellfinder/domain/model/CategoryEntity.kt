package com.omerkaya.sperrmuellfinder.domain.model

/**
 * Domain entity representing a post category
 */
data class CategoryEntity(
    val id: String,
    val nameEn: String,
    val nameDe: String,
    val iconName: String,
    val colorHex: String,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val parentCategoryId: String? = null,
    val mlKeywords: List<String> = emptyList(),
    val confidence: Float = 0f
) {
    companion object {
        /**
         * Default categories for SperrmüllFinder
         */
        fun getDefaultCategories(): List<CategoryEntity> = listOf(
            CategoryEntity(
                id = "furniture",
                nameEn = "Furniture",
                nameDe = "Möbel",
                iconName = "ic_furniture",
                colorHex = "#8B4513",
                sortOrder = 1,
                mlKeywords = listOf("chair", "table", "sofa", "bed", "cabinet", "desk", "shelf")
            ),
            CategoryEntity(
                id = "electronics",
                nameEn = "Electronics",
                nameDe = "Elektronik",
                iconName = "ic_electronics",
                colorHex = "#4169E1",
                sortOrder = 2,
                mlKeywords = listOf("computer", "tv", "phone", "radio", "speaker", "monitor", "laptop")
            ),
            CategoryEntity(
                id = "appliances",
                nameEn = "Appliances",
                nameDe = "Haushaltsgeräte",
                iconName = "ic_appliances",
                colorHex = "#32CD32",
                sortOrder = 3,
                mlKeywords = listOf("washing machine", "refrigerator", "microwave", "dishwasher", "vacuum")
            ),
            CategoryEntity(
                id = "clothing",
                nameEn = "Clothing",
                nameDe = "Kleidung",
                iconName = "ic_clothing",
                colorHex = "#FF69B4",
                sortOrder = 4,
                mlKeywords = listOf("shirt", "pants", "dress", "jacket", "shoes", "clothing", "textile")
            ),
            CategoryEntity(
                id = "books_media",
                nameEn = "Books & Media",
                nameDe = "Bücher & Medien",
                iconName = "ic_books",
                colorHex = "#DAA520",
                sortOrder = 5,
                mlKeywords = listOf("book", "magazine", "cd", "dvd", "vinyl", "newspaper")
            ),
            CategoryEntity(
                id = "toys_games",
                nameEn = "Toys & Games",
                nameDe = "Spielzeug & Spiele",
                iconName = "ic_toys",
                colorHex = "#FF6347",
                sortOrder = 6,
                mlKeywords = listOf("toy", "game", "puzzle", "doll", "ball", "bicycle", "skateboard")
            ),
            CategoryEntity(
                id = "sports_outdoor",
                nameEn = "Sports & Outdoor",
                nameDe = "Sport & Outdoor",
                iconName = "ic_sports",
                colorHex = "#228B22",
                sortOrder = 7,
                mlKeywords = listOf("bicycle", "skateboard", "sports equipment", "camping", "outdoor")
            ),
            CategoryEntity(
                id = "home_garden",
                nameEn = "Home & Garden",
                nameDe = "Haus & Garten",
                iconName = "ic_home_garden",
                colorHex = "#9ACD32",
                sortOrder = 8,
                mlKeywords = listOf("plant", "pot", "garden", "tools", "decoration", "vase")
            ),
            CategoryEntity(
                id = "art_crafts",
                nameEn = "Art & Crafts",
                nameDe = "Kunst & Handwerk",
                iconName = "ic_art",
                colorHex = "#9370DB",
                sortOrder = 9,
                mlKeywords = listOf("painting", "frame", "craft", "art", "canvas", "sculpture")
            ),
            CategoryEntity(
                id = "other",
                nameEn = "Other",
                nameDe = "Sonstiges",
                iconName = "ic_other",
                colorHex = "#808080",
                sortOrder = 10,
                mlKeywords = listOf("misc", "other", "unknown", "various")
            )
        )
        
        /**
         * Get category by ML keyword
         */
        fun getCategoryByKeyword(keyword: String): CategoryEntity? {
            return getDefaultCategories().find { category ->
                category.mlKeywords.any { it.contains(keyword, ignoreCase = true) }
            }
        }
        
        /**
         * Get category by English name
         */
        fun getCategoryByEnglishName(name: String): CategoryEntity? {
            return getDefaultCategories().find { 
                it.nameEn.equals(name, ignoreCase = true) 
            }
        }
        
        /**
         * Get category by German name
         */
        fun getCategoryByGermanName(name: String): CategoryEntity? {
            return getDefaultCategories().find { 
                it.nameDe.equals(name, ignoreCase = true) 
            }
        }
    }
}
