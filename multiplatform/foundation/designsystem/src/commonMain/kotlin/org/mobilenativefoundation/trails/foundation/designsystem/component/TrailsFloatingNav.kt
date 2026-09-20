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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** The five R2 roots. Hosts pass only the destinations that are functional in the current milestone. */
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

/** White floating pill; wraps destinations into rows when scaled labels need more room. */
@Composable
fun TrailsFloatingNav(
    items: List<TrailsDestination>,
    selected: TrailsDestination,
    onSelect: (TrailsDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val shape = RoundedCornerShape(TrailsTheme.radii.pill)
    val expandedShape = RoundedCornerShape(TrailsTheme.radii.xl)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val selectedLabelStyle = typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
    // Reserve the widest selected label so choosing a tab cannot rearrange the rows.
    val labelWidth = items.maxOfOrNull { textMeasurer.measure(it.label, selectedLabelStyle, softWrap = false).size.width } ?: 0
    val minimumItemWidth = with(density) { labelWidth.toDp() + 8.dp }.coerceAtLeast(48.dp)
    BoxWithConstraints(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        val columns = ((maxWidth - 16.dp) / minimumItemWidth).toInt().coerceIn(1, items.size.coerceAtLeast(1))
        val surfaceShape = if (columns < items.size) expandedShape else shape
        Column(
            Modifier.fillMaxWidth()
                .shadow(12.dp, surfaceShape, ambientColor = Color.Black.copy(alpha = 0.14f), spotColor = Color.Black.copy(alpha = 0.14f))
                .clip(surfaceShape).background(colors.surface).padding(8.dp).selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    row.forEach { item ->
                        val active = item == selected
                        Column(
                            Modifier.weight(1f).heightIn(min = 56.dp).clip(shape).background(if (active) colors.soft else Color.Transparent)
                                .selectable(selected = active, role = Role.Tab, onClick = { onSelect(item) }).padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(item.icon(), contentDescription = null, Modifier.size(24.dp), tint = colors.textPrimary)
                            Text(item.label, style = if (active) selectedLabelStyle else typography.labelSmall, color = colors.textPrimary)
                        }
                    }
                }
            }
        }
    }
}
