package org.mobilenativefoundation.trails.screen.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.data.trail.Trail
import org.mobilenativefoundation.trails.data.trail.TrailCollection
import org.mobilenativefoundation.trails.feat.savetrail.*
import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.screen.explore.M1ScrollPosition

@Inject
class SavedUi : Ui<SavedState> {
    @Composable
    override fun Content(state: SavedState, modifier: Modifier) {
        val colors = TrailsTheme.colors
        val typography = TrailsTheme.typography
        val collectionsScroll = rememberCheckpointedListState(state.collectionsScroll.index, state.collectionsScroll.offset, state.content.data != null && !state.allTrails) { index, offset ->
            state.send(SavedIntent.ScrollChanged(false, M1ScrollPosition(index, offset)))
        }
        val trailsScroll = rememberCheckpointedListState(state.trailsScroll.index, state.trailsScroll.offset, state.content.data != null && state.allTrails) { index, offset ->
            state.send(SavedIntent.ScrollChanged(true, M1ScrollPosition(index, offset)))
        }
        val snapshot = state.content.data
        LazyColumn(
            modifier.fillMaxSize().background(colors.background),
            state = if (state.allTrails) trailsScroll else collectionsScroll,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "heading") { Text("Saved", style = typography.displayMedium, color = colors.textPrimary, modifier = Modifier.semantics { heading() }) }
            item(key = "segments") { SavedTabs(state.allTrails) { state.send(SavedIntent.SelectSegment(it)) } }
            if (state.content.loading) item(key = "loading") { M1Loading(if (snapshot == null) "Opening your saved trails…" else "Refreshing saved trails…") }
            if (snapshot == null && !state.content.loading) item(key = "unavailable") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TrailsStatusLine(StatusKind.FAILED, "Couldn’t load your saved trails", actionLabel = "Try again", onAction = { state.send(SavedIntent.Retry) })
                    TrailsButton("Explore trails", { state.send(SavedIntent.Explore) }, tone = ButtonTone.Secondary, modifier = Modifier.fillMaxWidth())
                }
            }
            if (snapshot != null) {
                if (state.content.error != null) item(key = "read-error") {
                    TrailsStatusLine(StatusKind.FAILED, "Couldn’t refresh · Your saved trails are still here", actionLabel = if (state.content.loading) null else "Try again", onAction = { state.send(SavedIntent.Retry) })
                }
                item(key = "sync") { SavedSyncNotice(snapshot) { state.send(SavedIntent.RetrySync) } }
                val savedIds = snapshot.memberships.filterValues { it.isNotEmpty() }.keys
                val trails = snapshot.trails.filter { it.id in savedIds }.distinctBy { it.id }
                if (trails.size < savedIds.size) item(key = "missing-details") {
                    TrailsStatusLine(StatusKind.INFO, "Some trail details aren’t on this device yet", actionLabel = if (state.content.loading) null else "Try again", onAction = { state.send(SavedIntent.Retry) })
                }
                if (state.allTrails) {
                    if (savedIds.isEmpty()) item(key = "empty") { SavedEmpty { state.send(SavedIntent.Explore) } }
                    items(trails, key = { "trail-${it.id}" }) { trail ->
                        TrailSaveCard(trail, snapshot, { state.send(SavedIntent.OpenTrail(trail)) }, { onDismiss -> state.send(SavedIntent.SaveTrail(trail, onDismiss)) })
                    }
                } else {
                    if (snapshot.collections.isEmpty()) item(key = "empty") { SavedEmpty { state.send(SavedIntent.Explore) } }
                    items(snapshot.collections.chunked(2), key = { row -> "row-${row.first().id}" }) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            row.forEach { collection ->
                                val members = snapshot.memberships.filterValues { collection.id in it }.keys
                                CollectionTile(collection, snapshot.trails.firstOrNull { it.id in members }, members.size, Modifier.weight(1f)) {
                                    state.send(SavedIntent.OpenCollection(collection.id))
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedTabs(allTrails: Boolean, onSelect: (Boolean) -> Unit) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        listOf("Lists" to false, "All trails" to true).forEach { (label, value) ->
            val selected = allTrails == value
            Column(
                Modifier.width(IntrinsicSize.Max).heightIn(min = 48.dp).selectable(selected = selected, role = Role.Tab, onClick = { onSelect(value) }),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(label, style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = if (selected) colors.textPrimary else colors.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(if (selected) colors.textPrimary else colors.border))
            }
        }
    }
}

@Composable
private fun CollectionTile(collection: TrailCollection, cover: Trail?, count: Int, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Column(modifier.clickable(role = Role.Button, onClick = onOpen), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(TrailsTheme.radii.lg)).background(colors.soft), contentAlignment = Alignment.Center) {
            if (cover != null) TrailPhoto(cover.id, modifier = Modifier.fillMaxSize())
            else Icon(Icons.Outlined.Favorite.painter, contentDescription = if (count == 0) "Empty list" else null, Modifier.size(28.dp), tint = colors.textPrimary)
        }
        Text(collection.name, style = typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 21.sp), color = colors.textPrimary)
        Text(if (count == 0) "0 saved" else "$count ${if (count == 1) "trail" else "trails"}", style = typography.bodyMedium, color = colors.textSecondary)
    }
}

@Composable
private fun SavedEmpty(onExplore: () -> Unit) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 16.dp)) {
        Text("Keep your next escape", style = typography.titleLarge, color = colors.textPrimary)
        Text("Save a trail that catches your eye. You’ll find it here when you’re ready.", style = typography.bodyLarge, color = colors.textSecondary)
        TrailsButton("Explore trails", onExplore, tone = ButtonTone.Commit, modifier = Modifier.fillMaxWidth())
    }
}
