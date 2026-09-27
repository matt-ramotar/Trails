package org.mobilenativefoundation.trails.screen.collection

import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.ui.trail.*
import org.mobilenativefoundation.trails.app.navigation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.feature.savetrail.*
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.app.navigation.ScrollPosition

@Inject
class CollectionUi : Ui<CollectionState> {
    @Composable
    override fun Content(state: CollectionState, modifier: Modifier) {
        val colors = TrailsTheme.colors
        val typography = TrailsTheme.typography
        val snapshot = state.content.data
        val collection = snapshot?.collections?.firstOrNull { it.id == state.collectionId }
        val scroll = rememberCheckpointedListState(state.initialScroll.index, state.initialScroll.offset, collection != null) { index, offset ->
            state.send(CollectionIntent.ScrollChanged(ScrollPosition(index, offset)))
        }
        LazyColumn(
            modifier.fillMaxSize().background(colors.background), state = scroll,
            contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "back") {
                TrailsIconCircle(
                    icon = Icons.Outlined.ArrowLeft.painter,
                    contentDescription = "Back to saved",
                    onClick = { state.send(CollectionIntent.Back) },
                    container = colors.soft,
                )
            }
            item(key = "heading") { TrailsHeading(collection?.name ?: "Your collection") }
            if (state.content.loading) item(key = "loading") { TrailsLoading("Opening your collection…") }
            if (collection == null && !state.content.loading) item(key = "unavailable") {
                TrailsSurface(Modifier.fillMaxWidth()) {
                    TrailsStatusLine(StatusKind.FAILED, "This collection isn’t available", actionLabel = "Try again", onAction = { state.send(CollectionIntent.Retry) })
                }
            }
            if (snapshot != null && collection != null) {
                val ids = snapshot.memberships.filterValues { collection.id in it }.keys
                val trails = snapshot.trails.filter { it.id in ids }.distinctBy { it.id }
                item(key = "count") { Text("${ids.size} ${if (ids.size == 1) "trail" else "trails"} · Your collection", style = typography.bodyMedium, color = colors.textSecondary) }
                if (state.content.error != null) item(key = "error") {
                    TrailsStatusLine(StatusKind.FAILED, "Couldn’t refresh · Your trails are still here",
                        actionLabel = if (state.content.loading) null else "Try again", onAction = { state.send(CollectionIntent.Retry) })
                }
                item(key = "sync") {
                    SavedSyncNotice(snapshot.copy(syncByTrail = snapshot.syncByTrail.filterKeys { it in ids })) { state.send(CollectionIntent.RetrySync) }
                }
                if (ids.isEmpty()) item(key = "empty") {
                    TrailsSurface(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("A little room for adventure", style = typography.titleLarge, color = colors.textPrimary)
                            Text("Save a trail to this collection when something catches your eye.", style = typography.bodyLarge, color = colors.textSecondary)
                            TrailsControlsButton("Explore trails", { state.send(CollectionIntent.Explore) }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                if (trails.size < ids.size) item(key = "missing-details") { TrailsStatusLine(StatusKind.INFO, "Some trail details aren’t on this device yet") }
                items(trails, key = { it.id }) { trail ->
                    TrailSaveCard(trail, snapshot, { state.send(CollectionIntent.OpenTrail(trail)) }, { onDismiss -> state.send(CollectionIntent.SaveTrail(trail, onDismiss)) })
                }
            }
        }
    }
}
