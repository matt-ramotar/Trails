package org.mobilenativefoundation.trails.screen.traildetail

import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.ui.trail.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailFeature
import org.mobilenativefoundation.trails.feature.savetrail.*
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@Inject
class TrailDetailUi : Ui<TrailDetailState> {
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content(state: TrailDetailState, modifier: Modifier) {
        val colors = TrailsTheme.colors
        val typography = TrailsTheme.typography
        val trail = state.trail.data
        val snapshot = state.saved.data
        val saveButtonFocus = rememberTrailsFocusReturnTarget()
        val saved = trail?.let { current -> snapshot?.memberships?.get(current.id)?.isNotEmpty() }
        val scroll = rememberCheckpointedScrollState(state.initialScrollOffset, trail != null) { state.send(TrailDetailIntent.ScrollChanged(it)) }
        Column(modifier.fillMaxSize().background(colors.background)) {
            Column(Modifier.weight(1f).verticalScroll(scroll.state).then(scroll.contentModifier)) {
                if (trail == null) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        TrailsIconCircle(Icons.Outlined.ArrowLeft.painter, "Back", { state.send(TrailDetailIntent.Back) })
                        if (state.trail.loading) TrailsLoading("Opening this trail…")
                        else TrailsSurface(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                TrailsHeading("Trail unavailable")
                                TrailsStatusLine(StatusKind.FAILED, if (state.trail.offline) "Not on this device yet" else "Couldn’t open this trail")
                                TrailsButton("Try again", { state.send(TrailDetailIntent.Retry) }, tone = ButtonTone.Commit, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                } else {
                    Box(Modifier.fillMaxWidth()) {
                        TrailPhoto(trail.id, modifier = Modifier.fillMaxWidth().height(360.dp), describeImage = true)
                        TrailsIconCircle(Icons.Outlined.ArrowLeft.painter, "Back", { state.send(TrailDetailIntent.Back) }, Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 12.dp))
                        TrailBookmark(trail.name, saved, { onDismiss -> state.send(TrailDetailIntent.Save(onDismiss)) }, Modifier.align(Alignment.TopEnd).padding(end = 20.dp, top = 12.dp))
                        TrailsSurface(Modifier.fillMaxWidth().padding(top = 336.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                Text(trail.name, style = typography.headlineLarge, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                                TrailFactsRow(trail, trailing = trail.region)
                                if (state.trail.loading) TrailsLoading("Refreshing trail details…")
                                if (state.trail.error != null) TrailsStatusLine(StatusKind.FAILED, "Couldn’t refresh · Showing this device’s copy", actionLabel = if (state.trail.loading) null else "Try again", onAction = { state.send(TrailDetailIntent.Retry) })
                                TrailSyncNotice(snapshot?.syncByTrail?.get(trail.id), snapshot?.offline == true, snapshot?.syncing == true, { state.send(TrailDetailIntent.RetrySync) })
                                TrailFacts(trail)
                                ExpandableDescription(trail.description)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    trail.features.forEach { TrailTag(it.label()) }
                                }
                                trail.reviewExcerpt?.takeIf { it.isNotBlank() }?.let { excerpt ->
                                    Text("What hikers are saying", style = typography.titleLarge, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                                    Text("“$excerpt”", style = typography.bodyMedium, color = colors.textSecondary)
                                    Text("Sample review", style = typography.labelSmall, color = colors.textSecondary)
                                }
                                TrailPhotoCredit(trail.id)
                            }
                        }
                    }
                }
            }
            if (trail != null) TrailsButton(
                if (saved == true) "Edit saved collections" else "Save trail",
                { state.send(TrailDetailIntent.Save(saveButtonFocus::restore)) },
                saveButtonFocus.modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                tone = ButtonTone.Hero,
                leadingIcon = Icons.Outlined.Favorite.painter,
                accessibilityLabel = if (saved == true) "Edit saved collections" else "Save trail",
            )
        }
    }
}

@Composable
private fun TrailFacts(trail: Trail) {
    val facts = listOf(
        "Length" to trailDistance(trail.distanceMeters),
        "Elev. gain" to "${trail.elevationMeters} m",
        "Est. time" to trailDuration(trail.durationMinutes),
        "Route type" to if (TrailFeature.LOOP in trail.features) "Loop" else "Out & back",
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val separatorWidth = with(density) { 1.toDp() }
        val fourColumnWidth = (maxWidth - 72.dp - separatorWidth * 3) / 4
        val columns = if (fourColumnWidth >= 72.dp * density.fontScale) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            facts.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEachIndexed { index, (label, value) ->
                        if (index > 0) TrailsSeparator(vertical = true)
                        TrailFact(label, value, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TrailFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = TrailsTheme.typography.titleLarge, color = TrailsTheme.colors.textPrimary)
        Text(label, style = TrailsTheme.typography.bodySmall, color = TrailsTheme.colors.textSecondary)
    }
}

@Composable
private fun ExpandableDescription(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, style = TrailsTheme.typography.bodyLarge, color = TrailsTheme.colors.textPrimary, maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
        TrailsTextAction(if (expanded) "Show less" else "Show more", onClick = { expanded = !expanded })
    }
}

@Composable
private fun TrailTag(text: String) {
    TrailsChip(text)
}

private fun TrailFeature.label(): String = when (this) {
    TrailFeature.LAKE -> "Lakeside"
    TrailFeature.FOREST -> "Forest shade"
    TrailFeature.WATERFALL -> "Waterfall"
    TrailFeature.SUMMIT -> "Views"
    TrailFeature.LOOP -> "Loop"
    TrailFeature.DOG_FRIENDLY -> "Dog-friendly"
}
