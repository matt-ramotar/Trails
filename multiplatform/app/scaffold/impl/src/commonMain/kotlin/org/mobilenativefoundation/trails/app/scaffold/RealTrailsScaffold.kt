package org.mobilenativefoundation.trails.app.scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.mobilenativefoundation.trails.foundation.designsystem.component.glassEffect
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.foundation.scope.ActiveScope

@Inject
@SingleIn(ActiveScope::class)
@ContributesBinding(ActiveScope::class)
class RealTrailsScaffold : TrailsScaffold {
    @Composable
    override fun Content(
        state: ScaffoldState,
        backStack: SaveableBackStack,
        navigator: Navigator,
        modifier: Modifier
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            NavigableCircuitContent(
                backStack = backStack,
                navigator = navigator,
                modifier = Modifier.padding(
                    bottom = innerPadding.calculateBottomPadding(),
                    start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                    end = innerPadding.calculateEndPadding(LayoutDirection.Ltr)
                )
            )
        }
    }
}


@Composable
private fun BottomTabs(
    items: List<TrailsBottomNavItem>,
    selected: TrailsBottomNavItem,
    onSelect: (TrailsBottomNavItem) -> Unit,
) {
    val colorScheme = TrailsTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(
                shape = RoundedCornerShape(0.dp),
                backgroundColor = TrailsTheme.colors.glassBackground.copy(alpha = 0.72f),
                borderColor = Color.White.copy(alpha = 0.08f)
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 11.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        items.forEach { item ->
            val isSelected = item == selected
            val tint = if (isSelected) {
                colorScheme.primary
            } else {
                Color.White.copy(alpha = 0.56f)
            }

            TabItem(
                icon = if (isSelected) item.icons.selected.painter else item.icons.unselected.painter,
                label = item.label,
                tint = tint,
                onClick = { onSelect(item) }
            )
        }
    }
}


@Composable
private fun TabItem(
    icon: Painter,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    onClick: () -> Unit,
) {

    IconButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        Icon(
            painter = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}
