package org.mobilenativefoundation.trails.screen.navigate

import org.mobilenativefoundation.trails.ui.trail.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.data.trail.catalog.TrailFeature
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsLoading
import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@Inject
class NavigateUi : Ui<NavigateState> {
    @Composable
    override fun Content(state: NavigateState, modifier: Modifier) {
        val colors = TrailsTheme.colors
        val typography = TrailsTheme.typography
        val trail = state.trail.data
        var sheetHeight by remember { mutableStateOf(0) }
        val sheetPadding = with(LocalDensity.current) { sheetHeight.toDp() }
        Box(modifier.fillMaxSize().background(colors.soft).semantics { paneTitle = "Navigate" }) {
            if (trail != null) TrailRouteSchematic(trail.id, TrailFeature.LOOP in trail.features, Modifier.fillMaxSize().padding(bottom = sheetPadding))
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    trail != null -> TrailNamePill(trail.name) { state.send(NavigateIntent.OpenTrail) }
                    state.trail.loading -> TrailsLoading("Finding your trail…")
                    else -> TrailsSurface(Modifier.fillMaxWidth()) {
                        TrailsStatusLine(
                            StatusKind.FAILED, if (state.trail.offline) "Not on this device yet" else "Couldn’t load this trail",
                            actionLabel = "Try again", onAction = { state.send(NavigateIntent.Retry) },
                        )
                    }
                }
                TrailsChip("Route preview · schematic", size = ChipSize.Small)
            }
            TrailsSurface(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged { sheetHeight = it.height },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ready when you are", style = typography.titleMedium, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                    Text("Choose a trail or record your own route.", style = typography.bodyLarge, color = colors.textSecondary)
                    TrailsButton("Start recording", { state.send(NavigateIntent.StartRecording) }, Modifier.fillMaxWidth().padding(top = 8.dp), tone = ButtonTone.Hero)
                }
            }
            TrailsToastHost(
                state.toast?.let { TrailsToastData(it) },
                onDismissed = { state.send(NavigateIntent.DismissToast) },
                modifier = Modifier.padding(bottom = sheetPadding),
                showCheck = false,
            )
        }
    }
}

@Composable
private fun TrailNamePill(name: String, onClick: () -> Unit) {
    val colors = TrailsTheme.colors
    TrailsPressable(Modifier.fillMaxWidth(), onClick = onClick) {
        TrailsSurface(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Location.painter, contentDescription = null, Modifier.size(20.dp), tint = colors.textPrimary)
                Text(name, style = TrailsTheme.typography.titleSmall, color = colors.textPrimary)
            }
        }
    }
}
