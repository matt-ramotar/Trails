package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

// HeroUI Native 1.0.10 PressableFeedback: Easing.out(Easing.ease).
internal val TrailsEaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
internal val TrailsEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

@Composable
internal fun trailsMotionEnabled(): Boolean =
    (rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f) > 0f

/** Draw native keyboard focus inside the full target without changing its press behavior. */
@Composable
fun Modifier.trailsFocusRing(
    interactionSource: MutableInteractionSource,
    shape: Shape,
    enabled: Boolean = true,
): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    val color = TrailsTheme.colors.accent
    return drawWithContent {
        drawContent()
        if (focused && enabled) {
            val stroke = 2.dp.toPx()
            inset(stroke / 2f) {
                drawOutline(shape.createOutline(size, layoutDirection, this), color, style = Stroke(stroke))
            }
        }
    }
}

/** Share Native press feedback while the caller retains its own selection and action semantics. */
@Composable
fun Modifier.trailsPressFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    highlight: Boolean = false,
    shape: Shape = RectangleShape,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val motionEnabled = trailsMotionEnabled()
    val density = LocalDensity.current
    var widthDp by remember { mutableFloatStateOf(300f) }
    val scale by animateFloatAsState(
        if (pressed && enabled && motionEnabled) 1f - 0.015f * (300f / widthDp.coerceAtLeast(1f)) else 1f,
        tween(if (motionEnabled) 300 else 0, easing = TrailsEaseOut),
    )
    val highlightAlpha by animateFloatAsState(
        if (pressed && enabled && motionEnabled && highlight) 0.1f else 0f,
        tween(if (motionEnabled) 200 else 0),
    )
    val highlightColor = TrailsTheme.colors.textPrimary
    return onSizeChanged { widthDp = with(density) { it.width.toDp().value } }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .drawWithContent {
            drawContent()
            if (highlightAlpha > 0f) {
                drawOutline(shape.createOutline(size, layoutDirection, this), highlightColor.copy(alpha = highlightAlpha))
            }
        }
        .trailsFocusRing(interactionSource, shape, enabled)
}

@Composable
fun TrailsPressable(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .trailsPressFeedback(interaction, enabled)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
