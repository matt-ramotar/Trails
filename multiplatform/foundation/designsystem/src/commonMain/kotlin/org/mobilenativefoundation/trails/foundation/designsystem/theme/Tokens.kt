package org.mobilenativefoundation.trails.foundation.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val lgPlus: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
)


val LocalSpacing = staticCompositionLocalOf { Spacing() }


@Immutable
data class Radii(
    val sm: Dp = 10.dp,
    val md: Dp = 14.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 30.dp,
    val pill: Dp = 999.dp,
    val card: Dp = 24.dp,
    val sheet: Dp = 28.dp,
)


val LocalRadii = staticCompositionLocalOf { Radii() }
