package org.mobilenativefoundation.trails.screen.activity

import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.ui.trail.*
import org.mobilenativefoundation.trails.app.navigation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import kotlinx.datetime.TimeZone
import org.mobilenativefoundation.trails.data.trail.activity.CompletedActivity
import org.mobilenativefoundation.trails.feature.savetrail.*
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.app.navigation.ScrollPosition

@Inject
class ActivityUi : Ui<ActivityState> {
    @Composable
    override fun Content(state: ActivityState, modifier: Modifier) {
        RegisterActivityDeveloperRetry(state.send)
        val colors = TrailsTheme.colors
        val typography = TrailsTheme.typography
        val zone = TimeZone.currentSystemDefault()
        val history = state.history.data
        val scroll = rememberCheckpointedListState(state.initialScroll.index, state.initialScroll.offset, history != null) { index, offset ->
            state.send(ActivityIntent.ScrollChanged(ScrollPosition(index, offset)))
        }
        LazyColumn(
            modifier.fillMaxSize().background(colors.background).semantics { paneTitle = "Activity" }, state = scroll,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "heading") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Activity", style = typography.displayMedium, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                    Text("A little progress. A lot of fresh air.", style = typography.bodyLarge, color = colors.textSecondary)
                }
            }
            if (history == null && state.history.loading) item(key = "loading") { TrailsLoading("Opening your activity…") }
            if (history == null && !state.history.loading) item(key = "unavailable") {
                TrailsStatusLine(
                    if (state.history.offline) StatusKind.OFFLINE else StatusKind.FAILED,
                    if (state.history.offline) "Offline · Activity isn’t on this device yet" else "Couldn’t load your activity",
                    actionLabel = "Try again", onAction = { state.send(ActivityIntent.Retry) },
                )
            }
            if (history != null) {
                item(key = "month") { MonthCard(monthSummary(history, state.nowEpochMillis, zone)) }
                if (state.history.error != null) item(key = "refresh-error") {
                    TrailsStatusLine(StatusKind.FAILED, "Couldn’t refresh · Showing this device’s copy", actionLabel = if (state.history.loading) null else "Try again", onAction = { state.send(ActivityIntent.Retry) })
                }
                item(key = "recent") { Text("Your recent adventures", style = typography.titleLarge, color = colors.textPrimary, modifier = Modifier.semantics { heading() }) }
                if (history.isEmpty()) item(key = "empty") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Your hikes will show up here", style = typography.titleSmall, color = colors.textPrimary)
                        Text("Find a trail worth walking and it’ll be waiting for you here afterwards.", style = typography.bodyLarge, color = colors.textSecondary)
                        TrailsButton("Explore trails", { state.send(ActivityIntent.Explore) }, Modifier.fillMaxWidth(), tone = ButtonTone.Commit)
                    }
                }
                val sorted = history.sortedByDescending { it.completedAtEpochMillis }
                sorted.firstOrNull()?.let { latest -> item(key = "hero-${latest.id}") { ActivityHero(latest, state, zone) } }
                items(sorted.drop(1), key = { it.id }) { activity -> ActivityRow(activity, state, zone) }
            }
        }
    }
}

@Composable
private fun MonthCard(summary: MonthSummary) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(TrailsTheme.radii.card)).background(colors.dark).padding(20.dp).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(summary.monthLabel.uppercase(), style = typography.labelMedium, color = colors.citron)
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            MonthStat(kilometres(summary.distanceMeters), "km walked")
            MonthStat("${summary.trails}", if (summary.trails == 1) "trail" else "trails")
            MonthStat("${summary.minutesOutside / 60}h", "outside")
        }
        LastSevenDaysBars(summary.lastSevenDaysMeters)
    }
}

@Composable
private fun MonthStat(value: String, label: String) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = typography.headlineMedium, color = colors.onDark)
        Text(label, style = typography.bodySmall, color = colors.onDark.copy(alpha = 0.72f))
    }
}

/** Seven bars, oldest first; the most recent day with activity is citron. Decorative: the stats above carry the numbers. */
@Composable
private fun LastSevenDaysBars(values: List<Int>) {
    val colors = TrailsTheme.colors
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val highlight = values.indexOfLast { it > 0 }
    Canvas(Modifier.fillMaxWidth().height(56.dp)) {
        val gap = 8.dp.toPx()
        val width = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { index, meters ->
            val height = size.height * (0.16f + 0.84f * meters / max.toFloat())
            val color = when {
                index == highlight -> colors.citron
                meters > 0 -> colors.onDark.copy(alpha = 0.55f)
                else -> colors.onDark.copy(alpha = 0.18f)
            }
            drawRoundRect(color, topLeft = Offset(index * (width + gap), size.height - height), size = Size(width, height), cornerRadius = CornerRadius(6.dp.toPx()))
        }
    }
}

@Composable
private fun ActivityHero(activity: CompletedActivity, state: ActivityState, zone: TimeZone) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val trail = state.trails[activity.trailId]
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { state.send(ActivityIntent.OpenTrail(activity.trailId)) }, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(TrailsTheme.radii.lg))) {
            TrailPhoto(activity.trailId, Modifier.fillMaxSize())
            if (trail != null) TrailBookmark(trail.name, state.saved.data?.memberships?.get(trail.id)?.isNotEmpty(), { onDismiss -> state.send(ActivityIntent.SaveTrail(trail, onDismiss)) }, Modifier.align(Alignment.TopEnd).padding(12.dp))
        }
        Text(activity.trailName, style = typography.titleSmall.copy(fontSize = 18.sp, lineHeight = 24.sp), color = colors.textPrimary)
        Text(activitySummaryLine(activity, state.nowEpochMillis, zone), style = typography.bodyMedium, color = colors.textSecondary)
        syncStatus(state.saved.data?.syncByTrail?.get(activity.trailId), state.saved.data?.offline == true)?.let {
            TrailsStatusLine(it.kind, it.message)
        }
    }
}

@Composable
private fun ActivityRow(activity: CompletedActivity, state: ActivityState, zone: TimeZone) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val trail = state.trails[activity.trailId]
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { state.send(ActivityIntent.OpenTrail(activity.trailId)) }.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            TrailPhoto(activity.trailId, Modifier.size(64.dp).clip(RoundedCornerShape(TrailsTheme.radii.md)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(activity.trailName, style = typography.titleSmall, color = colors.textPrimary)
                Text(activitySummaryLine(activity, state.nowEpochMillis, zone), style = typography.bodyMedium, color = colors.textSecondary)
            }
            if (trail != null) TrailBookmark(trail.name, state.saved.data?.memberships?.get(trail.id)?.isNotEmpty(), { onDismiss -> state.send(ActivityIntent.SaveTrail(trail, onDismiss)) })
        }
        syncStatus(state.saved.data?.syncByTrail?.get(activity.trailId), state.saved.data?.offline == true)?.let {
            TrailsStatusLine(it.kind, it.message)
        }
        HorizontalDivider(color = colors.border)
    }
}
