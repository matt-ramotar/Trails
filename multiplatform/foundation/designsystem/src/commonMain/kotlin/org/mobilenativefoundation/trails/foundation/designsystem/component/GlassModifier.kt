package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme


fun Modifier.glassEffect(
    shape: Shape,
    backgroundColor: Color,
    borderColor: Color,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(backgroundColor, shape)
    .border(borderWidth, borderColor, shape)

@Composable
fun Modifier.glassCard(
    shape: Shape = TrailsTheme.shapes.medium
): Modifier = this.glassEffect(
    shape = shape,
    backgroundColor = TrailsTheme.colors.glassBackground,
    borderColor = TrailsTheme.colors.glassBorder
)
