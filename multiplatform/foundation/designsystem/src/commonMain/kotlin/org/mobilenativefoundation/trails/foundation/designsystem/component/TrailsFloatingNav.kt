package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** The five application roots. Each root retains an independent navigation stack. */
enum class TrailsDestination(val label: String) {
    EXPLORE("Explore"), FOR_YOU("For You"), NAVIGATE("Navigate"), SAVED("Saved"), ACTIVITY("Activity"),
}

@Composable
private fun TrailsDestination.icon(): Painter = when (this) {
    TrailsDestination.EXPLORE -> Icons.Outlined.Search.painter
    TrailsDestination.FOR_YOU -> Icons.Outlined.Sparkles.painter
    TrailsDestination.NAVIGATE -> Icons.Outlined.DiscoverCircle.painter
    TrailsDestination.SAVED -> Icons.Outlined.Favorite.painter
    TrailsDestination.ACTIVITY -> Icons.Outlined.ChartIncrease.painter
}

/** Tabs/Surface composition with compact labels and wrapping for scaled native text. */
@Composable
fun TrailsFloatingNav(
    items: List<TrailsDestination>,
    selected: TrailsDestination,
    onSelect: (TrailsDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val shape = RoundedCornerShape(24.dp)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Compact Native text-xs labels preserve a usable content viewport at 200% scale.
    val labelStyle = typography.labelSmall
    val labelWidth = items.maxOf { textMeasurer.measure(it.label, labelStyle, softWrap = false).size.width }
    val minimumItemWidth = with(density) { labelWidth.toDp() + 8.dp }.coerceAtLeast(48.dp)
    val bounds = remember { mutableStateMapOf<TrailsDestination, Rect>() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val target = bounds[selected]
    val motion = trailsMotionEnabled()
    val indicatorSpec = if (motion) spring<Dp>(dampingRatio = 1.732f, stiffness = 1200f) else snap()
    val left by animateDpAsState(with(density) { ((target?.left ?: origin.x) - origin.x).toDp() }, indicatorSpec)
    val top by animateDpAsState(with(density) { ((target?.top ?: origin.y) - origin.y).toDp() }, indicatorSpec)
    val width by animateDpAsState(with(density) { (target?.width ?: 0f).toDp() }, indicatorSpec)
    val height by animateDpAsState(with(density) { (target?.height ?: 0f).toDp() }, indicatorSpec)
    BoxWithConstraints(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        val columns = ((maxWidth - 6.dp + 4.dp) / (minimumItemWidth + 4.dp)).toInt().coerceIn(1, items.size)
        Box(
            Modifier.fillMaxWidth().trailsShadow(shape).clip(shape).background(colors.soft)
                .onGloballyPositioned { origin = it.positionInRoot() },
            contentAlignment = AbsoluteAlignment.TopLeft,
        ) {
            if (target != null) Box(
                Modifier.absoluteOffset { IntOffset(left.roundToPx(), top.roundToPx()) }.size(width, height)
                    .testTag("Trails root navigation indicator")
                    .trailsShadow(shape).background(colors.surface, shape),
            )
            Column(Modifier.fillMaxWidth().padding(3.dp).selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        row.forEach { item ->
                            val active = item == selected
                            val interaction = remember(item) { MutableInteractionSource() }
                            Column(
                                Modifier.weight(1f).heightIn(min = 48.dp).clip(shape)
                                    .trailsFocusRing(interaction, shape)
                                    .selectable(active, interactionSource = interaction, indication = null, role = Role.Tab, onClick = { onSelect(item) })
                                    .onGloballyPositioned { bounds[item] = it.boundsInRoot() }
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                val tint = if (active) colors.textPrimary else colors.textSecondary
                                Icon(item.icon(), contentDescription = null, Modifier.size(24.dp), tint = tint)
                                Text(item.label, style = labelStyle, color = tint, softWrap = false)
                            }
                        }
                    }
                }
            }
        }
    }
}
