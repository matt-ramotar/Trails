package org.mobilenativefoundation.trails.screen.foryou

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.data.trail.SavedSnapshot
import org.mobilenativefoundation.trails.data.trail.ForYouFeed
import org.mobilenativefoundation.trails.data.trail.Trail
import org.mobilenativefoundation.trails.feat.savetrail.*
import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.screen.explore.M1ScrollPosition

@Inject
class ForYouUi : Ui<ForYouState> {
    @Composable
    override fun Content(state: ForYouState, modifier: Modifier) {
        val colors = TrailsTheme.colors
        val typography = TrailsTheme.typography
        val feed = state.feed.data
        val rows = feed?.recommendedTrailIds.orEmpty().mapNotNull { state.trails[it] }
        val missingDetails = feed != null && (rows.isEmpty() || state.trails[feed.featuredTrailId] == null)
        // A feed can arrive before the catalog. Do not consume or publish the initial
        // checkpoint against placeholders, including a failed uncached catalog.
        val contentReady = feed != null && !state.feed.loading && state.trails.isNotEmpty()
        val scroll = rememberCheckpointedListState(state.initialScroll.index, state.initialScroll.offset, contentReady) { index, offset ->
            state.send(ForYouIntent.ScrollChanged(M1ScrollPosition(index, offset)))
        }
        LazyColumn(
            modifier.fillMaxSize().background(colors.background).semantics { paneTitle = "For you" }, state = scroll,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "heading") { Text("For you", style = typography.displayMedium, color = colors.textPrimary, modifier = Modifier.semantics { heading() }) }
            if (feed == null && state.feed.loading) item(key = "loading") { M1Loading("Finding your picks…") }
            if (feed == null && !state.feed.loading) item(key = "unavailable") {
                TrailsStatusLine(
                    if (state.feed.offline) StatusKind.OFFLINE else StatusKind.FAILED,
                    if (state.feed.offline) "Offline · Picks aren’t on this device yet" else "Couldn’t load your picks",
                    actionLabel = "Try again", onAction = { state.send(ForYouIntent.Retry) },
                )
            }
            if (feed != null) {
                item(key = "feature") { FeatureCard(feed, state.trails[feed.featuredTrailId]) { trail -> state.send(ForYouIntent.OpenTrail(trail)) } }
                if (state.feed.error != null && !missingDetails) item(key = "refresh-error") {
                    TrailsStatusLine(StatusKind.FAILED, "Couldn’t refresh · Showing this device’s copy", actionLabel = if (state.feed.loading) null else "Try again", onAction = { state.send(ForYouIntent.Retry) })
                }
                item(key = "section") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Based on your activity", style = typography.titleLarge, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                        Text("More like ${state.trails[feed.anchorTrailId]?.name ?: "your last hike"}", style = typography.bodyMedium, color = colors.textSecondary)
                    }
                }
                if (missingDetails) item(key = "rows-missing") {
                    TrailsStatusLine(
                        StatusKind.INFO, "Trail details aren’t on this device yet",
                        actionLabel = if (state.feed.loading) null else "Try again",
                        onAction = { state.send(ForYouIntent.Retry) },
                    )
                }
                items(rows, key = { it.id }) { trail ->
                    RecommendationRow(trail, state.saved.data,
                        onOpen = { state.send(ForYouIntent.OpenTrail(trail)) }, onSave = { onDismiss -> state.send(ForYouIntent.SaveTrail(trail, onDismiss)) })
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(feed: ForYouFeed, trail: Trail?, onOpen: (Trail) -> Unit) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Box(
        Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(TrailsTheme.radii.card))
            .clickable(enabled = trail != null, role = Role.Button) { trail?.let(onOpen) }
            .semantics(mergeDescendants = true) {},
    ) {
        TrailPhoto(feed.featuredTrailId, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.dark.copy(alpha = 0.10f), colors.dark.copy(alpha = 0.60f)))))
        Row(Modifier.align(Alignment.TopStart).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            TrailsCompass(Modifier.size(22.dp), tint = colors.onDark)
            Text("trails", style = typography.displayLarge.copy(fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.96).sp), color = colors.onDark)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(feed.headline, style = typography.headlineMedium, color = colors.onDark)
            Text(feed.subline, style = typography.bodyMedium, color = colors.onDark.copy(alpha = 0.78f))
        }
    }
}

@Composable
private fun RecommendationRow(trail: Trail, saved: SavedSnapshot?, onOpen: () -> Unit, onSave: (() -> Unit) -> Unit) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            TrailPhoto(trail.id, Modifier.size(64.dp).clip(RoundedCornerShape(TrailsTheme.radii.md)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(trail.name, style = typography.titleSmall, color = colors.textPrimary)
                TrailFactsRow(trail, showCount = false)
            }
            TrailBookmark(trail.name, saved?.memberships?.get(trail.id)?.isNotEmpty(), onSave)
        }
        syncStatus(saved?.syncByTrail?.get(trail.id), saved?.offline == true)?.let {
            TrailsStatusLine(it.kind, it.message)
        }
        HorizontalDivider(color = colors.border)
    }
}
