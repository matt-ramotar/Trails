package org.mobilenativefoundation.trails.foundation.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font
import trails.multiplatform.foundation.designsystem.generated.resources.Res
import trails.multiplatform.foundation.designsystem.generated.resources.manrope_variable
import trails.multiplatform.foundation.designsystem.generated.resources.inter_variable


@Immutable
data class TrailsTextStyles(
    val message: TextStyle,
    val messageEmphasis: TextStyle,
    val timestamp: TextStyle,
    val codeInline: TextStyle,
    val codeBlock: TextStyle,
    val input: TextStyle,
    val button: TextStyle,
)

val LocalTextStyles = staticCompositionLocalOf {
    TrailsTextStyles(
        message = TextStyle.Default,
        messageEmphasis = TextStyle.Default,
        timestamp = TextStyle.Default,
        codeInline = TextStyle.Default,
        codeBlock = TextStyle.Default,
        input = TextStyle.Default,
        button = TextStyle.Default
    )
}

@Composable
internal expect fun trailsTypography(): Typography

@Composable
internal expect fun trailsTextStyles(): TrailsTextStyles

@OptIn(ExperimentalResourceApi::class, ExperimentalTextApi::class)
@Composable
internal fun trailsDisplayFontFamily(): FontFamily = FontFamily(
    *listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(
            Res.font.manrope_variable,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    }.toTypedArray()
)

@OptIn(ExperimentalResourceApi::class, ExperimentalTextApi::class)
@Composable
internal fun trailsBodyFontFamily(): FontFamily = FontFamily(
    *listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(
            Res.font.inter_variable,
            weight = weight,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight.weight),
                FontVariation.Setting("opsz", 14f),
            ),
        )
    }.toTypedArray()
)

/** Native semantic type sizes with Trails' display and body families. */
internal fun buildTrailsTypography(display: FontFamily, body: FontFamily): Typography = Typography(
    displayLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 40.sp),
    displayMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    displaySmall = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 28.sp),
    titleSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)
