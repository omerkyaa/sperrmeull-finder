package com.omerkaya.sperrmuellfinder.core.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Colors - SperrmüllFinder
val BrandPrimary = Color(0xFF2E7D32) // Forest Green
val BrandSecondary = Color(0xFF4CAF50) // Material Green
val BrandTertiary = Color(0xFF81C784) // Light Green

// SperrmüllFinder Orange Theme - Bottom Navigation & FAB
val SfOrange500 = Color(0xFFFF9F2E) // Main orange - selected state
val SfOrange600 = Color(0xFFE6881A) // Pressed state
val SfOrange200 = Color(0xFFFFE0B3) // Ripple/indicator (light)
val SfOrange100 = Color(0xFFFFF2E6) // Very light orange for backgrounds
val SfNavy700 = Color(0xFF1A365D) // Navy outline/border
val SfGray500 = Color(0xFF9E9E9E) // Unselected icons/text
val SfGray400 = Color(0xFFBDBDBD) // Unselected icons/text (dark mode)

// Surface Colors
val SurfaceDark = Color(0xFF1C1B1F)
val SurfaceLight = Color(0xFFFFFBFE)
val SurfaceVariantDark = Color(0xFF49454F)
val SurfaceVariantLight = Color(0xFFE7E0EC)

// Background Colors
val BackgroundDark = Color(0xFF141218)
val BackgroundLight = Color(0xFFFFFBFE)

// Premium Colors
val PremiumGold = Color(0xFFFFD700)
val PremiumGoldDark = Color(0xFFB8860B)
val PremiumCrystal = SfOrange500 // Turuncu seçili durum teması – SperrmüllFinder
val PremiumCrystalDark = SfOrange600 // Turuncu seçili durum teması – SperrmüllFinder

// Status Colors
val SuccessGreen = Color(0xFF4CAF50)
val SuccessGreenDark = Color(0xFF2E7D32)
val WarningOrange = Color(0xFFFF9800)
val WarningOrangeDark = Color(0xFFE65100)
val ErrorRed = Color(0xFFF44336)
val ErrorRedDark = Color(0xFFB71C1C)
val InfoBlue = Color(0xFF2196F3)
val InfoBlueDark = Color(0xFF0277BD)

// Content Colors
val OnSurfaceDark = Color(0xFFE6E1E5)
val OnSurfaceLight = Color(0xFF1C1B1F)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)
val OnSurfaceVariantLight = Color(0xFF49454F)

// Outline Colors
val OutlineDark = Color(0xFF938F99)
val OutlineLight = Color(0xFF79747E)
val OutlineVariantDark = Color(0xFF49454F)
val OutlineVariantLight = Color(0xFFCAC4D0)

// Social Media Colors
val LikeRed = Color(0xFFE91E63)
val LikeRedDark = Color(0xFFC2185B)
val ShareBlue = Color(0xFF1976D2)
val ShareBlueDark = Color(0xFF1565C0)
val CommentGray = Color(0xFF757575)
val CommentGrayDark = Color(0xFF424242)

// Post Category Colors
val CategoryFurniture = Color(0xFF8D6E63)
val CategoryElectronics = Color(0xFF607D8B)
val CategoryClothing = SfOrange500 // Turuncu seçili durum teması – SperrmüllFinder
val CategoryBooks = Color(0xFF795548)
val CategorySports = Color(0xFF4CAF50)
val CategoryKitchen = Color(0xFFFF5722)
val CategoryDecoration = Color(0xFFE91E63)
val CategoryOther = Color(0xFF9E9E9E)

// Map Colors
val MapMarkerDefault = Color(0xFF757575)
val MapMarkerPremium = Color(0xFFFFD700)
val MapMarkerRecent = Color(0xFF4CAF50)
val MapClusterBackground = Color(0xFF2196F3)
val MapClusterText = Color(0xFFFFFFFF)

// XP & Level Colors
val XpBarBackground = Color(0xFFE0E0E0)
val XpBarBackgroundDark = Color(0xFF424242)
val XpBarFill = Color(0xFF4CAF50)
val LevelBadgeBackground = Color(0xFF2196F3)
val LevelBadgeText = Color(0xFFFFFFFF)

// Honesty Score Colors
val HonestyHigh = Color(0xFF4CAF50) // 80-100
val HonestyMedium = Color(0xFFFF9800) // 50-79
val HonestyLow = Color(0xFFF44336) // 0-49

// Availability Colors
val AvailabilityHigh = Color(0xFF4CAF50) // 80-100%
val AvailabilityMedium = Color(0xFFFF9800) // 50-79%
val AvailabilityLow = Color(0xFFF44336) // 0-49%

// Chat/Comment Colors
val MyMessageBackground = Color(0xFF2196F3)
val MyMessageBackgroundDark = Color(0xFF1976D2)
val OtherMessageBackground = Color(0xFFE0E0E0)
val OtherMessageBackgroundDark = Color(0xFF424242)

// Shimmer Colors
val ShimmerHighlight = Color(0xFFE0E0E0)
val ShimmerHighlightDark = Color(0xFF424242)
val ShimmerBase = Color(0xFFF5F5F5)
val ShimmerBaseDark = Color(0xFF2C2C2C)

// Semantic Color Sets for Light Theme
object LightColors {
    val primary = BrandPrimary
    val onPrimary = Color.White
    val primaryContainer = Color(0xFFB8E6BB)
    val onPrimaryContainer = Color(0xFF002204)
    
    val secondary = BrandSecondary
    val onSecondary = Color.White
    val secondaryContainer = Color(0xFFDEF6DF)
    val onSecondaryContainer = Color(0xFF002204)
    
    val tertiary = BrandTertiary
    val onTertiary = Color.White
    val tertiaryContainer = Color(0xFFE8F5E8)
    val onTertiaryContainer = Color(0xFF0D1F0D)
    
    val error = ErrorRed
    val onError = Color.White
    val errorContainer = Color(0xFFFFDAD6)
    val onErrorContainer = Color(0xFF410002)
    
    val background = BackgroundLight
    val onBackground = OnSurfaceLight
    val surface = SurfaceLight
    val onSurface = OnSurfaceLight
    val surfaceVariant = SurfaceVariantLight
    val onSurfaceVariant = OnSurfaceVariantLight
    
    val outline = OutlineLight
    val outlineVariant = OutlineVariantLight
    val scrim = Color.Black
    val inverseSurface = Color(0xFF313033)
    val inverseOnSurface = Color(0xFFF4EFF4)
    val inversePrimary = Color(0xFF9CCC9F)
}

// Semantic Color Sets for Dark Theme
object DarkColors {
    val primary = Color(0xFF9CCC9F)
    val onPrimary = Color(0xFF003909)
    val primaryContainer = Color(0xFF005310)
    val onPrimaryContainer = Color(0xFFB8E6BB)
    
    val secondary = Color(0xFFC2E8C3)
    val onSecondary = Color(0xFF003909)
    val secondaryContainer = Color(0xFF1E4A20)
    val onSecondaryContainer = Color(0xFFDEF6DF)
    
    val tertiary = Color(0xFFBCCDBD)
    val onTertiary = Color(0xFF263527)
    val tertiaryContainer = Color(0xFF3C4B3D)
    val onTertiaryContainer = Color(0xFFE8F5E8)
    
    val error = Color(0xFFFFB4AB)
    val onError = Color(0xFF690005)
    val errorContainer = Color(0xFF93000A)
    val onErrorContainer = Color(0xFFFFDAD6)
    
    val background = BackgroundDark
    val onBackground = OnSurfaceDark
    val surface = SurfaceDark
    val onSurface = OnSurfaceDark
    val surfaceVariant = SurfaceVariantDark
    val onSurfaceVariant = OnSurfaceVariantDark
    
    val outline = OutlineDark
    val outlineVariant = OutlineVariantDark
    val scrim = Color.Black
    val inverseSurface = Color(0xFFE6E1E5)
    val inverseOnSurface = Color(0xFF313033)
    val inversePrimary = BrandPrimary
}

// Legacy Colors (for backward compatibility)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// SperrmüllFinder Orange Navigation Colors - Dark Mode Support
val SfOrange200Dark = Color(0xFFFFCC80) // Lighter orange for dark mode indicators
val SfGray300Dark = Color(0xFF757575) // Unselected icons/text in dark mode

// Convenience Extensions
val Color.Companion.SperrmullPrimary: Color get() = BrandPrimary
val Color.Companion.SperrmullSecondary: Color get() = BrandSecondary
val Color.Companion.PremiumGold: Color get() = com.omerkaya.sperrmuellfinder.core.ui.theme.PremiumGold
val Color.Companion.PremiumCrystal: Color get() = com.omerkaya.sperrmuellfinder.core.ui.theme.PremiumCrystal
val Color.Companion.Success: Color get() = SuccessGreen
val Color.Companion.Warning: Color get() = WarningOrange
val Color.Companion.Error: Color get() = ErrorRed
val Color.Companion.Info: Color get() = InfoBlue

// SperrmüllFinder Navigation Theme Extensions
val Color.Companion.SfOrangeSelected: Color get() = SfOrange500
val Color.Companion.SfGrayUnselected: Color get() = SfGray500

// Direct color access for easy imports
val SperrmullPrimary: Color get() = BrandPrimary
val SperrmullSecondary: Color get() = BrandSecondary

// Marker Colors (for MarkerUtils compatibility)
val MarkerBasic: Color get() = MapMarkerDefault
val MarkerPremium: Color get() = MapMarkerPremium
val MarkerPremiumCrystal: Color get() = PremiumCrystal
