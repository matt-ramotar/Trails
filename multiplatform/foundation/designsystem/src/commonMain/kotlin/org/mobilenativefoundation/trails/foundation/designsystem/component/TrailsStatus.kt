package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** Mirrors the Figma set `Trails / Status indicator`. */
enum class StatusKind { PENDING, OFFLINE, FAILED, ATTENTION, INFO }

@Composable
private fun StatusIndicator(kind: StatusKind) {
    val colors = TrailsTheme.colors
    when (kind) {
        StatusKind.OFFLINE -> Box(Modifier.size(6.dp).clip(CircleShape).background(colors.textSecondary))
        StatusKind.PENDING -> Icon(Icons.Outlined.Sync.painter, contentDescription = null, Modifier.size(14.dp), tint = colors.textSecondary)
        StatusKind.FAILED -> Icon(Icons.Outlined.Alert.painter, contentDescription = null, Modifier.size(14.dp), tint = colors.danger)
        StatusKind.ATTENTION -> Icon(Icons.Outlined.Alert.painter, contentDescription = null, Modifier.size(14.dp), tint = colors.warning)
        StatusKind.INFO -> Icon(Icons.Outlined.Circle.painter, contentDescription = null, Modifier.size(14.dp), tint = colors.textSecondary)
    }
}

/** One sentence beside the content it describes. Failed and Attention use foreground text; the action is a separate button node. */
@Composable
fun TrailsStatusLine(
    kind: StatusKind,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val emphasis = kind == StatusKind.FAILED || kind == StatusKind.ATTENTION
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f, fill = false).semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusIndicator(kind)
            Text(message, style = typography.bodySmall, color = if (emphasis) colors.textPrimary else colors.textSecondary)
        }
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = typography.titleSmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = colors.textPrimary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(role = Role.Button, onClick = onAction).heightIn(min = 48.dp).wrapContentHeight(),
            )
        }
    }
}

data class TrailsToastData(val message: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null)

/** Dark one-line completion toast; announced once as a polite live region. A surface so its body absorbs taps instead of passing them to the content underneath. */
@Composable
fun TrailsToast(data: TrailsToastData, modifier: Modifier = Modifier, showCheck: Boolean = true) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Surface(
        modifier.fillMaxWidth().heightIn(min = 52.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(14.dp),
        color = colors.dark,
        contentColor = colors.onDark,
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showCheck) Icon(Icons.Outlined.Tick.painter, contentDescription = null, Modifier.size(16.dp), tint = colors.citron)
            Text(data.message, style = typography.titleSmall, color = colors.onDark, modifier = Modifier.weight(1f))
            if (data.actionLabel != null && data.onAction != null) {
                Text(
                    data.actionLabel, style = typography.titleSmall, color = colors.citron,
                    modifier = Modifier.clickable(role = Role.Button, onClick = data.onAction).heightIn(min = 48.dp).wrapContentHeight(),
                )
            }
        }
    }
}

/** Hosts one toast at the bottom of its container and clears it after [durationMillis]. Place it inside the scaffold content so it sits above the navigation. */
@Composable
fun TrailsToastHost(toast: TrailsToastData?, onDismissed: () -> Unit, modifier: Modifier = Modifier, durationMillis: Long = 5_000, showCheck: Boolean = true) {
    LaunchedEffect(toast) { if (toast != null) { delay(durationMillis); onDismissed() } }
    if (toast != null) Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        TrailsToast(toast, Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp), showCheck = showCheck)
    }
}
