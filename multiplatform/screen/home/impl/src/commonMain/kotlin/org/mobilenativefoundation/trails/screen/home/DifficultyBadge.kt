package org.mobilenativefoundation.trails.screen.home

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icon
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.model.domain.feed.TrailDifficulty

/**
 * A badge that displays the trail difficulty level with appropriate colors and icons.
 */
@Composable
fun DifficultyBadge(
    difficulty: TrailDifficulty,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val spec =
        when (difficulty) {
            TrailDifficulty.GREEN_CIRCLE -> DifficultyBadgeSpec(
                gradient = Brush.linearGradient(
                    colors = listOf(
                        TrailsTheme.colors.greenCircle,
                        TrailsTheme.colors.greenCircle.copy(alpha = 0.8f)
                    )
                ),
                icon = Icons.Solid.Circle,
                iconCount = 1,
                iconTint = TrailsTheme.colors.greenCircle,
                label = "Green"
            )
            TrailDifficulty.BLUE_SQUARE -> DifficultyBadgeSpec(
                gradient = Brush.linearGradient(
                    colors = listOf(
                        TrailsTheme.colors.blueSquare,
                        TrailsTheme.colors.blueSquare.copy(alpha = 0.8f)
                    )
                ),
                icon = Icons.Solid.Square,
                iconCount = 1,
                iconTint = TrailsTheme.colors.blueSquare,
                label = "Blue"
            )
            TrailDifficulty.BLACK_DIAMOND -> DifficultyBadgeSpec(
                gradient = Brush.linearGradient(
                    colors = listOf(TrailsTheme.colors.onBlackDiamond.copy(alpha = 0.1f), Color.Transparent),
                ),
                icon = Icons.Solid.Diamond,
                iconCount = 1,
                iconTint = TrailsTheme.colors.blackDiamond,
                label = "Black Diamond"
            )
            TrailDifficulty.DOUBLE_BLACK_DIAMOND -> DifficultyBadgeSpec(
                gradient = Brush.linearGradient(
                    colors = listOf(TrailsTheme.colors.onBlackDiamond.copy(alpha = 0.1f), Color.Transparent),
                ),
                icon = Icons.Solid.Diamond01,
                iconCount = 2,
                iconTint = TrailsTheme.colors.doubleBlack,
                label = "Double Black"
            )
        }

    val infiniteTransition = rememberInfiniteTransition(label = "difficulty_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (animated) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "difficulty_scale"
    )

    Row(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(999.dp))
            .background(spec.gradient, RoundedCornerShape(999.dp))
            .outlineIfBlackDiamond(difficulty)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(spec.iconCount) {
                M3Icon(
                    painter = spec.icon.painter,
                    contentDescription = spec.icon.contentDescription,
                    tint = spec.iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = spec.label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

private data class DifficultyBadgeSpec(
    val gradient: Brush,
    val icon: Icon,
    val iconCount: Int,
    val iconTint: Color,
    val label: String,
)

@Composable
fun Modifier.outlineIfBlackDiamond(difficulty: TrailDifficulty): Modifier {
    return if (difficulty in listOf(TrailDifficulty.BLACK_DIAMOND, TrailDifficulty.DOUBLE_BLACK_DIAMOND)) {
        this.border(2.dp, TrailsTheme.colorScheme.error, RoundedCornerShape(999.dp))
    } else {
        this
    }
}
