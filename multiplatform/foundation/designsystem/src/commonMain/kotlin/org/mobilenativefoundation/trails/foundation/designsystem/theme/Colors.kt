package org.mobilenativefoundation.trails.foundation.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color


private val Azure5 = Color(0xFF060A12)
private val Azure6 = Color(0xFF06101A)
private val Azure7 = Color(0xFF0C101A)
private val Grey6 = Color(0xFF101010)

private val Azure59 = Color(0xFF2F8CFF)
private val Azure67 = Color(0xFF57A6FF)
private val Azure74 = Color(0xFF7AA7FF)

private val Cyan71 = Color(0xFF6AE4FF)
private val Cyan77 = Color(0xFF89F7FF)

private val SpringGreen53 = Color(0xFF38D6A6)
private val SpringGreen48 = Color(0xFF27D07A)

private val Blue56 = Color(0xFF1D4CFF)
private val Blue71 = Color(0xFF6B8BFF)

private val Orange70 = Color(0xFFFFD166)
private val Red68 = Color(0xFFFF5D5D)

private val White = Color(0xFFFFFFFF)
private val White92 = Color(0xEBFFFFFF)
private val White70 = Color(0xB2FFFFFF)
private val White60 = Color(0x99FFFFFF)
private val White12 = Color(0x1FFFFFFF)
private val White8 = Color(0x14FFFFFF)

private val Black55 = Color(0x8C000000)

private val ErrorContainer = Color(0xFF3A0B0B)
private val ErrorLight = Color(0xFFFEE2E2)

private val Forest = Color(0xFF1D4B35)
private val Citron = Color(0xFFA9F184)
private val Stone = Color(0xFFFFFFFF) // The default background is white.
private val Ink = Color(0xFF171E14)
private val Muted = Color(0xFF545A52)
private val Border = Color(0xFFE6E8E4)
private val Soft = Color(0xFFF4F5F4)
private val ForestDark = Color(0xFF0D1F18)
private val Clay = Color(0xFFBC624A)
private val Warning = Color(0xFF8A4B12)
private val Danger = Color(0xFFA5352B)
private val DifficultyEasy = Color(0xFF43A047)
private val DifficultyModerate = Color(0xFFF2B82E)
private val DifficultyHard = Color(0xFFEE6A45)
private val DifficultyStrenuous = Color(0xFF5E3A27)

val TrailsLightColorScheme = lightColorScheme(
    primary = Forest,
    onPrimary = White,
    primaryContainer = Soft,
    onPrimaryContainer = Forest,
    secondary = Forest,
    onSecondary = White,
    secondaryContainer = Citron,
    onSecondaryContainer = Forest,
    tertiary = Forest,
    onTertiary = White,
    tertiaryContainer = Soft,
    onTertiaryContainer = Ink,
    background = Stone,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Soft,
    onSurfaceVariant = Muted,
    surfaceTint = Forest,
    outline = Border,
    outlineVariant = Border,
    inverseSurface = ForestDark,
    inverseOnSurface = White,
    inversePrimary = Citron,
    // Clay is an accent; Ink preserves readable normal-sized error text.
    error = Ink,
    onError = White,
    errorContainer = Color(0xFFF8EAE5),
    onErrorContainer = Ink,
    surfaceContainerLowest = White,
    surfaceContainerLow = Stone,
    surfaceContainer = Stone,
    surfaceContainerHigh = Soft,
    surfaceContainerHighest = Soft,
    surfaceBright = White,
    surfaceDim = Stone,
    scrim = ForestDark.copy(alpha = 0.48f),
)


val TrailsDarkColorScheme = ColorScheme(

    primary = Azure59,
    onPrimary = White,
    primaryContainer = Blue56,
    onPrimaryContainer = White92,
    inversePrimary = Azure74,


    secondary = SpringGreen53,
    onSecondary = Azure5,
    secondaryContainer = SpringGreen48,
    onSecondaryContainer = Azure5,


    tertiary = Cyan71,
    onTertiary = Azure5,
    tertiaryContainer = Azure67,
    onTertiaryContainer = Azure5,


    background = Azure5,
    onBackground = White92,
    surface = Azure7,
    onSurface = White92,
    surfaceVariant = Grey6,
    onSurfaceVariant = White70,
    surfaceTint = Azure59,


    inverseSurface = White,
    inverseOnSurface = Azure5,


    error = Red68,
    onError = White,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorLight,


    outline = White12,
    outlineVariant = White8,
    scrim = Black55,


    surfaceBright = Azure6,
    surfaceDim = Color(0xFF05070D),
    surfaceContainer = Azure7,
    surfaceContainerHigh = Color(0xFF101625),
    surfaceContainerHighest = Color(0xFF151B26),
    surfaceContainerLow = Azure5,
    surfaceContainerLowest = Color(0xFF05070D),


    primaryFixed = Azure74,
    primaryFixedDim = Azure59,
    onPrimaryFixed = Azure5,
    onPrimaryFixedVariant = Azure7,

    secondaryFixed = SpringGreen53,
    secondaryFixedDim = SpringGreen48,
    onSecondaryFixed = Azure5,
    onSecondaryFixedVariant = Azure7,

    tertiaryFixed = Cyan77,
    tertiaryFixedDim = Cyan71,
    onTertiaryFixed = Azure5,
    onTertiaryFixedVariant = Azure7,
)

@Immutable
data class TrailsExtendedColors(

    val greenCircle: Color,
    val onGreenCircle: Color,
    val blueSquare: Color,
    val onBlueSquare: Color,
    val blackDiamond: Color,
    val onBlackDiamond: Color,
    val doubleBlack: Color,
    val onDoubleBlack: Color,
    val doubleBlackBorder: Color,


    val freshTrails: Color,
    val packedTrails: Color,
    val icy: Color,
    val groomed: Color,


    val liftOpen: Color,
    val liftClosed: Color,
    val crewOnline: Color,
    val crewOffline: Color,


    val badgeCommon: Color,
    val badgeRare: Color,
    val badgeEpic: Color,
    val badgeLegendary: Color,


    val glassBackground: Color,
    val glassBorder: Color,


    val gradientStart: Color,
    val gradientMid: Color,
    val gradientEnd: Color,
    val attention: Color,

    // Semantic roles used by trail browsing and synchronization controls.
    val background: Color = Stone,
    val surface: Color = White,
    val textPrimary: Color = Ink,
    val textSecondary: Color = Muted,
    val border: Color = Border,
    val accent: Color = Forest,
    val onAccent: Color = White,
    val citron: Color = Citron,
    val soft: Color = Soft,
    val dark: Color = ForestDark,
    val onCitron: Color = Ink,
    val onDark: Color = White,
    val warning: Color = Warning,
    val danger: Color = Danger,
    val difficultyEasy: Color = DifficultyEasy,
    val difficultyModerate: Color = DifficultyModerate,
    val difficultyHard: Color = DifficultyHard,
    val difficultyStrenuous: Color = DifficultyStrenuous,
)

val TrailsExtendedColorsDark = TrailsExtendedColors(

    greenCircle = SpringGreen53,
    onGreenCircle = Azure5,
    blueSquare = Azure59,
    onBlueSquare = White,
    blackDiamond = Azure7,
    onBlackDiamond = White92,
    doubleBlack = Color(0xFF05070D),
    onDoubleBlack = White92,
    doubleBlackBorder = Red68,


    freshTrails = SpringGreen53,
    packedTrails = Azure74,
    icy = Cyan77,
    groomed = Azure59,


    liftOpen = SpringGreen53,
    liftClosed = Red68,
    crewOnline = SpringGreen53,
    crewOffline = White60,


    badgeCommon = White60,
    badgeRare = Azure59,
    badgeEpic = Blue71,
    badgeLegendary = Orange70,


    glassBackground = Color(0xFF0EA5E9).copy(alpha = 0.1f),
    glassBorder = Color(0x1AFFFFFF),


    gradientStart = Azure59,
    gradientMid = SpringGreen53,
    gradientEnd = Azure5,
    attention = Red68,
    background = ForestDark,
    surface = Ink,
    textPrimary = White,
    textSecondary = Color(0xFFC4D1C7),
    border = Color(0xFF365044),
    accent = Citron,
    onAccent = ForestDark,
    soft = Color(0xFF243D30),
)

val TrailsExtendedColorsLight = TrailsExtendedColorsDark.copy(
    background = Stone,
    surface = White,
    textPrimary = Ink,
    textSecondary = Muted,
    border = Border,
    accent = Forest,
    onAccent = White,
    soft = Soft,
    attention = Clay,
)


val LocalTrailsExtendedColors = staticCompositionLocalOf {
    TrailsExtendedColorsLight
}

val MaterialTheme.trailsColors: TrailsExtendedColors
    @androidx.compose.runtime.Composable
    @androidx.compose.runtime.ReadOnlyComposable
    get() = LocalTrailsExtendedColors.current
