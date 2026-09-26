package org.mobilenativefoundation.trails.foundation.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider


object TrailsTheme {
    val colorScheme @Composable get() = MaterialTheme.colorScheme
    val colors @Composable get() = MaterialTheme.trailsColors
    val textStyles @Composable get() = LocalTextStyles.current
    val shapes @Composable get() = MaterialTheme.shapes
    val typography @Composable get() = MaterialTheme.typography
    val gradients @Composable get() = LocalGradients.current
    val spacing @Composable get() = LocalSpacing.current
    val radii @Composable get() = LocalRadii.current
}

@Composable
fun TrailsTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) TrailsDarkColorScheme else TrailsLightColorScheme,
        typography = trailsTypography(),
        shapes = TrailsShapes
    ) {
        CompositionLocalProvider(
            LocalTrailsExtendedColors provides if (darkTheme) TrailsExtendedColorsDark else TrailsExtendedColorsLight,
            LocalTextStyles provides trailsTextStyles(),
            content = content
        )
    }
}
