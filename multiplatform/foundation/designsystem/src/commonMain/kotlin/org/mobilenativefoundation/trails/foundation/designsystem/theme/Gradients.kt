package org.mobilenativefoundation.trails.foundation.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val Azure59 = Color(0xFF2F8CFF)
private val Azure74 = Color(0xFF7AA7FF)
private val Cyan71 = Color(0xFF6AE4FF)
private val Cyan77 = Color(0xFF89F7FF)
private val SpringGreen53 = Color(0xFF38D6A6)
private val SpringGreen48 = Color(0xFF27D07A)
private val Blue56 = Color(0xFF1D4CFF)
private val Blue71 = Color(0xFF6B8BFF)
private val Blue85 = Color(0xFFB0B7FF)
private val Orange70 = Color(0xFFFFD166)
private val Red68 = Color(0xFFFF5D5D)


@Immutable
data class TrailsGradients(
    val sunset: Brush,
    val ocean: Brush,
    val mint: Brush,
    val rose: Brush,
    val brand: Brush,
    val location: Brush,
    val friends: Brush,
    val quickSnap: Brush,
    val activeTabIndicator: Brush,
    val deepPurple: Brush,
)


val LocalGradients = staticCompositionLocalOf {
    TrailsGradients(
        sunset = Brush.linearGradient(
            colors = listOf(Orange70, Red68, Blue71),
            start = Offset.Zero, end = Offset.Infinite
        ),
        ocean = Brush.linearGradient(
            listOf(Cyan77, Azure59), Offset.Zero, Offset.Infinite
        ),
        mint = Brush.linearGradient(
            listOf(SpringGreen53, Cyan71), Offset.Zero, Offset.Infinite
        ),
        rose = Brush.linearGradient(
            listOf(Blue85, Blue71), Offset.Zero, Offset.Infinite
        ),

        brand = Brush.linearGradient(
            colors = listOf(Azure59, SpringGreen53),
            start = Offset.Zero,
            end = Offset.Infinite
        ),
        location = Brush.linearGradient(
            colors = listOf(Azure74, Azure59),
            start = Offset.Zero,
            end = Offset.Infinite
        ),
        friends = Brush.linearGradient(
            colors = listOf(SpringGreen53, SpringGreen48),
            start = Offset.Zero,
            end = Offset.Infinite
        ),
        quickSnap = Brush.linearGradient(
            colors = listOf(Blue71, Blue56),
            start = Offset.Zero,
            end = Offset.Infinite
        ),
        activeTabIndicator = Brush.linearGradient(
            colors = listOf(Azure59, Azure74),
            start = Offset.Zero,
            end = Offset.Infinite
        ),
        deepPurple = Brush.linearGradient(
            colors = listOf(Blue85, Blue56),
            start = Offset.Zero,
            end = Offset.Infinite
        ),
    )
}
