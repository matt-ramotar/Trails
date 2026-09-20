package org.mobilenativefoundation.trails.foundation.designsystem.theme


import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
internal actual fun trailsTextStyles(): TrailsTextStyles {
    val base = trailsTypography()
    return TrailsTextStyles(
        message = base.bodyLarge,
        messageEmphasis = base.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        timestamp = base.labelSmall.copy(
            letterSpacing = 0.sp,
            color = TrailsTheme.colorScheme.onSurfaceVariant
        ),
        codeInline = base.bodyMedium.copy(fontFamily = Mono, fontSize = 14.sp, lineHeight = 20.sp),
        codeBlock = base.bodyMedium.copy(fontFamily = Mono, fontSize = 13.sp, lineHeight = 20.sp),
        input = base.bodyLarge,
        button = base.labelLarge
    )
}

internal val Mono = FontFamily.Monospace


@Composable
internal actual fun trailsTypography(): Typography {
    val base = buildTrailsTypography(trailsDisplayFontFamily(), trailsBodyFontFamily())
    return base.copy(
        bodyLarge = base.bodyLarge.copy(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        bodyMedium = base.bodyMedium.copy(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        bodySmall = base.bodySmall.copy(platformStyle = PlatformTextStyle(includeFontPadding = false))
    )
}
