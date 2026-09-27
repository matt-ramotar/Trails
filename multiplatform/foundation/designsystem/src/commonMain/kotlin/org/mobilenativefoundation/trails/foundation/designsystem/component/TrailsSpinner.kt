package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/**
 * Two gradient-filled paths adapted to Compose from HeroUI Native's spinner-icon.tsx.
 * Source: heroui-inc/heroui-native at 122f63db3159f2192a9de38c0e85e5ae9004473a.
 * Copyright 2025 NextUI Inc. Licensed under Apache-2.0. Modified for native Compose drawing and motion.
 */
@Composable
fun TrailsSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = TrailsTheme.colors.accent,
    visible: Boolean = true,
) {
    val motionEnabled = trailsMotionEnabled()
    val visibility = remember { MutableTransitionState(!motionEnabled && visible) }
    visibility.targetState = visible
    AnimatedVisibility(
        visibleState = visibility,
        enter = fadeIn(tween(if (motionEnabled) 200 else 0, easing = TrailsEaseOut)),
        exit = fadeOut(tween(if (motionEnabled) 100 else 0)),
    ) {
        val rotation = if (motionEnabled) {
            val transition = rememberInfiniteTransition()
            val angle by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(909, easing = LinearEasing), RepeatMode.Restart))
            angle
        } else 0f
        val first = remember { PathParser().parsePathString("M8.749.021a1.5 1.5 0 0 1 .497 2.958A7.5 7.5 0 0 0 3 10.375a7.5 7.5 0 0 0 7.5 7.5v3c-5.799 0-10.5-4.7-10.5-10.5C0 5.23 3.726.865 8.749.021").toPath() }
        val second = remember { PathParser().parsePathString("M15.392 2.673a1.5 1.5 0 0 1 2.119-.115A10.48 10.48 0 0 1 21 10.375c0 5.8-4.701 10.5-10.5 10.5v-3a7.5 7.5 0 0 0 5.007-13.084a1.5 1.5 0 0 1-.115-2.118").toPath() }
        Canvas(modifier.size(size).semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }) {
            rotate(rotation) {
                scale(this.size.width / 24f, this.size.height / 24f, pivot = Offset.Zero) {
                    translate(1.5f, 1.625f) {
                        val a = first.getBounds()
                        val b = second.getBounds()
                        drawPath(first, Brush.linearGradient(
                            colors = listOf(color, color.copy(alpha = color.alpha * 0.55f)),
                            start = Offset(a.center.x, a.top + a.height * 0.05271f),
                            end = Offset(a.center.x, a.top + a.height * 0.91793f),
                        ))
                        drawPath(second, Brush.linearGradient(
                            colors = listOf(color.copy(alpha = 0f), color.copy(alpha = color.alpha * 0.55f)),
                            start = Offset(b.center.x, b.top + b.height * 0.1524f),
                            end = Offset(b.center.x, b.top + b.height * 0.8715f),
                        ))
                    }
                }
            }
        }
    }
}
