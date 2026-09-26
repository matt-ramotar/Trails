package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsExtendedColors
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** Mirrors the Figma set `Trails / Difficulty marker`: circle, rounded square, triangle, diamond. */
enum class DifficultyMarkerKind { EASY, MODERATE, HARD, STRENUOUS }

fun TrailsExtendedColors.difficulty(kind: DifficultyMarkerKind): Color = when (kind) {
    DifficultyMarkerKind.EASY -> difficultyEasy
    DifficultyMarkerKind.MODERATE -> difficultyModerate
    DifficultyMarkerKind.HARD -> difficultyHard
    DifficultyMarkerKind.STRENUOUS -> difficultyStrenuous
}

/** Decorative. The adjacent difficulty text carries the meaning, so the marker adds no semantics node. */
@Composable
fun DifficultyMarker(kind: DifficultyMarkerKind, modifier: Modifier = Modifier) {
    val color = TrailsTheme.colors.difficulty(kind)
    Canvas(modifier.size(12.dp)) {
        val w = size.width
        val h = size.height
        when (kind) {
            DifficultyMarkerKind.EASY -> drawCircle(color)
            DifficultyMarkerKind.MODERATE -> drawRoundRect(color, cornerRadius = CornerRadius(w * 0.22f))
            DifficultyMarkerKind.HARD -> drawPath(Path().apply { moveTo(w / 2f, 0f); lineTo(w, h); lineTo(0f, h); close() }, color)
            DifficultyMarkerKind.STRENUOUS -> drawPath(Path().apply { moveTo(w / 2f, 0f); lineTo(w, h / 2f); lineTo(w / 2f, h); lineTo(0f, h / 2f); close() }, color)
        }
    }
}
