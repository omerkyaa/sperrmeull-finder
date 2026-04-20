package com.omerkaya.sperrmuellfinder.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkColors.primary,
    onPrimary = DarkColors.onPrimary,
    primaryContainer = DarkColors.primaryContainer,
    onPrimaryContainer = DarkColors.onPrimaryContainer,
    
    secondary = DarkColors.secondary,
    onSecondary = DarkColors.onSecondary,
    secondaryContainer = DarkColors.secondaryContainer,
    onSecondaryContainer = DarkColors.onSecondaryContainer,
    
    tertiary = DarkColors.tertiary,
    onTertiary = DarkColors.onTertiary,
    tertiaryContainer = DarkColors.tertiaryContainer,
    onTertiaryContainer = DarkColors.onTertiaryContainer,
    
    error = DarkColors.error,
    onError = DarkColors.onError,
    errorContainer = DarkColors.errorContainer,
    onErrorContainer = DarkColors.onErrorContainer,
    
    background = DarkColors.background,
    onBackground = DarkColors.onBackground,
    surface = DarkColors.surface,
    onSurface = DarkColors.onSurface,
    surfaceVariant = DarkColors.surfaceVariant,
    onSurfaceVariant = DarkColors.onSurfaceVariant,
    
    outline = DarkColors.outline,
    outlineVariant = DarkColors.outlineVariant,
    scrim = DarkColors.scrim,
    inverseSurface = DarkColors.inverseSurface,
    inverseOnSurface = DarkColors.inverseOnSurface,
    inversePrimary = DarkColors.inversePrimary
)

private val LightColorScheme = lightColorScheme(
    primary = LightColors.primary,
    onPrimary = LightColors.onPrimary,
    primaryContainer = LightColors.primaryContainer,
    onPrimaryContainer = LightColors.onPrimaryContainer,
    
    secondary = LightColors.secondary,
    onSecondary = LightColors.onSecondary,
    secondaryContainer = LightColors.secondaryContainer,
    onSecondaryContainer = LightColors.onSecondaryContainer,
    
    tertiary = LightColors.tertiary,
    onTertiary = LightColors.onTertiary,
    tertiaryContainer = LightColors.tertiaryContainer,
    onTertiaryContainer = LightColors.onTertiaryContainer,
    
    error = LightColors.error,
    onError = LightColors.onError,
    errorContainer = LightColors.errorContainer,
    onErrorContainer = LightColors.onErrorContainer,
    
    background = LightColors.background,
    onBackground = LightColors.onBackground,
    surface = LightColors.surface,
    onSurface = LightColors.onSurface,
    surfaceVariant = LightColors.surfaceVariant,
    onSurfaceVariant = LightColors.onSurfaceVariant,
    
    outline = LightColors.outline,
    outlineVariant = LightColors.outlineVariant,
    scrim = LightColors.scrim,
    inverseSurface = LightColors.inverseSurface,
    inverseOnSurface = LightColors.inverseOnSurface,
    inversePrimary = LightColors.inversePrimary
)

/**
 * Enum for theme modes
 */
enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

/**
 * Main theme composable for SperrmüllFinder
 */
@Composable
fun SperrmullFinderTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use surface color for status bar for better integration
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            
            val windowsInsetsController = WindowCompat.getInsetsController(window, view)
            windowsInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowsInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

/**
 * Legacy theme composable for backward compatibility
 */
@Composable
fun SperrmullFinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT
    SperrmullFinderTheme(
        themeMode = themeMode,
        dynamicColor = dynamicColor,
        content = content
    )
}

/**
 * Extension properties for commonly used colors in components
 */
val MaterialTheme.customColors: CustomColors
    @Composable
    get() = if (isSystemInDarkTheme()) {
        CustomColors(
            success = SuccessGreenDark,
            onSuccess = Color.White,
            warning = WarningOrangeDark,
            onWarning = Color.White,
            info = InfoBlueDark,
            onInfo = Color.White,
            premiumGold = PremiumGoldDark,
            premiumCrystal = PremiumCrystalDark,
            shimmerBase = ShimmerBaseDark,
            shimmerHighlight = ShimmerHighlightDark
        )
    } else {
        CustomColors(
            success = SuccessGreen,
            onSuccess = Color.White,
            warning = WarningOrange,
            onWarning = Color.White,
            info = InfoBlue,
            onInfo = Color.White,
            premiumGold = PremiumGold,
            premiumCrystal = PremiumCrystal,
            shimmerBase = ShimmerBase,
            shimmerHighlight = ShimmerHighlight
        )
    }

data class CustomColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val info: Color,
    val onInfo: Color,
    val premiumGold: Color,
    val premiumCrystal: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color
)
