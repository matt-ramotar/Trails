package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** Domain synchronization meanings, independent of the visual feedback component. */
enum class StatusKind { PENDING, OFFLINE, FAILED, ATTENTION, INFO }

@Composable
private fun StatusIndicator(kind: StatusKind) {
    val colors = TrailsTheme.colors
    val icon = when (kind) {
        StatusKind.PENDING -> Icons.Outlined.Sync.painter
        StatusKind.FAILED, StatusKind.ATTENTION -> Icons.Outlined.Alert.painter
        else -> Icons.Outlined.Circle.painter
    }
    Icon(icon, contentDescription = null, Modifier.size(16.dp), tint = when (kind) {
        StatusKind.FAILED -> colors.danger
        StatusKind.ATTENTION -> colors.warning
        else -> colors.textSecondary
    })
}

/** Display-only Chip or Alert; recovery remains a separate accessible action. */
@Composable
fun TrailsStatusLine(
    kind: StatusKind,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    if (kind == StatusKind.FAILED || kind == StatusKind.ATTENTION) {
        TrailsAlert(message, kind, modifier, actionLabel, onAction)
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TrailsChip(message, size = ChipSize.Small, leadingContent = { StatusIndicator(kind) })
            if (actionLabel != null && onAction != null) TrailsTextAction(actionLabel, onAction)
        }
    }
}

data class TrailsToastData(val message: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null)

enum class ToastTone { Default, Accent, Success, Warning, Danger }

/** Native Toast surface. The non-clickable Surface consumes body taps without adding an action. */
@Composable
fun TrailsToast(
    data: TrailsToastData,
    modifier: Modifier = Modifier,
    showCheck: Boolean = true,
    tone: ToastTone = if (showCheck) ToastTone.Success else ToastTone.Default,
) {
    val colors = TrailsTheme.colors
    val foreground = when (tone) {
        ToastTone.Default -> colors.textPrimary
        ToastTone.Accent, ToastTone.Success -> colors.accent
        ToastTone.Warning -> colors.warning
        ToastTone.Danger -> colors.danger
    }
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier.fillMaxWidth().trailsShadow(shape, TrailsShadow.Overlay)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = shape, color = colors.surface, contentColor = colors.textPrimary,
        tonalElevation = 0.dp, shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (showCheck) Icon(Icons.Outlined.Tick.painter, contentDescription = null, Modifier.size(20.dp), tint = foreground)
                Text(data.message, style = TrailsTheme.typography.titleSmall, color = foreground, modifier = Modifier.weight(1f))
            }
            if (data.actionLabel != null && data.onAction != null) TrailsTextAction(data.actionLabel, data.onAction)
        }
    }
}

/** Keeps the existing five-second lifetime; native-style exit finishes after dismissal. */
@Composable
fun TrailsToastHost(
    toast: TrailsToastData?,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    durationMillis: Long = 5_000,
    showCheck: Boolean = true,
) {
    LaunchedEffect(toast) { if (toast != null) { delay(durationMillis); onDismissed() } }
    val motion = trailsMotionEnabled()
    val travel = with(LocalDensity.current) { 100.dp.roundToPx() }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedContent(
            targetState = toast,
            contentAlignment = Alignment.BottomCenter,
            transitionSpec = {
                if (motion) {
                    slideInVertically(spring(dampingRatio = 0.289f, stiffness = 100f / 3f)) { travel }
                        .togetherWith(
                            slideOutVertically(tween(150, easing = CubicBezierEasing(0.4f, 0f, 1f, 1f))) { travel } +
                                fadeOut(tween(150), targetAlpha = 0.5f) + scaleOut(tween(150), targetScale = 0.97f),
                        )
                } else EnterTransition.None.togetherWith(ExitTransition.None)
            },
            label = "Toast presentation",
        ) { current ->
            if (current != null) {
                TrailsToast(current, Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp), showCheck)
            }
        }
    }
}

/** Native Alert surface and title colors; recovery has its own target beneath the message. */
@Composable
fun TrailsAlert(
    message: String,
    kind: StatusKind = StatusKind.INFO,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = TrailsTheme.colors
    val statusColor = when (kind) {
        StatusKind.FAILED -> colors.danger
        StatusKind.ATTENTION -> colors.warning
        else -> colors.textPrimary
    }
    TrailsSurface(modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, contentPadding = PaddingValues(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 3.5.dp)) { StatusIndicator(kind) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message, style = TrailsTheme.typography.titleSmall, color = statusColor)
                if (actionLabel != null && onAction != null) TrailsTextAction(actionLabel, onAction)
            }
        }
    }
}
