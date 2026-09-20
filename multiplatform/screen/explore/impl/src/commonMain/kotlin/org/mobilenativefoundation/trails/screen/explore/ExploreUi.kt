package org.mobilenativefoundation.trails.screen.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.data.trail.TrailQuery
import org.mobilenativefoundation.trails.data.trail.TrailSort
import org.mobilenativefoundation.trails.feat.filters.FilterSection
import org.mobilenativefoundation.trails.feat.savetrail.*
import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

fun TrailSort.displayName(): String = when (this) {
    TrailSort.MOST_POPULAR -> "Most popular"
    TrailSort.HIGHEST_RATED -> "Highest rated"
    TrailSort.SHORTEST -> "Shortest"
    TrailSort.LONGEST -> "Longest"
}

@Inject
class ExploreUi : Ui<ExploreState> {
    @Composable
    override fun Content(state: ExploreState, modifier: Modifier) {
        val colors = TrailsTheme.colors
        val typography = TrailsTheme.typography
        val keyboard = LocalSoftwareKeyboardController.current
        val scroll = rememberCheckpointedListState(state.initialScroll.index, state.initialScroll.offset, state.results.data != null) { index, offset ->
            state.send(ExploreIntent.ScrollChanged(M1ScrollPosition(index, offset)))
        }
        val queryIdentity = state.query.toString()
        var displayedQuery by rememberSaveable { mutableStateOf(queryIdentity) }
        LaunchedEffect(queryIdentity) {
            if (displayedQuery != queryIdentity) { scroll.scrollToItem(0); displayedQuery = queryIdentity }
        }
        LazyColumn(
            modifier.fillMaxSize().background(colors.background).semantics { paneTitle = "Explore" }, state = scroll,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "search") {
                TrailsM1SearchField(state.text, { state.send(ExploreIntent.QueryChanged(it)) }, {
                    state.send(ExploreIntent.SubmitSearch); keyboard?.hide()
                }, modifier = Modifier.fillMaxWidth())
            }
            item(key = "filter-chips") {
                val filtersFocus = rememberTrailsFocusReturnTarget()
                fun open(section: FilterSection) { keyboard?.hide(); state.send(ExploreIntent.Filters(section, filtersFocus::restore)) }
                Column {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrailsM1Chip(
                            "All", selected = false, onClick = { open(FilterSection.ALL) },
                            modifier = filtersFocus.modifier.semantics { contentDescription = "Filters" },
                            leadingIcon = { Icon(Icons.Outlined.FilterHorizontal.painter, contentDescription = null, Modifier.size(16.dp), tint = colors.textPrimary) },
                        )
                        TrailsM1Chip("Difficulty ⌄", state.query.difficulties.isNotEmpty(), { open(FilterSection.DIFFICULTY) })
                        TrailsM1Chip("Length ⌄", state.query.minMeters > 0 || state.query.maxMeters != null, { open(FilterSection.LENGTH) })
                        TrailsM1Chip("Elevation gain ⌄", state.query.minElevationGain > 0 || state.query.maxElevationGain != null, { open(FilterSection.ELEVATION) })
                    }
                    appliedSummary(state.query)?.let {
                        Text(it, style = typography.labelSmall, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            val trails = state.results.data
            if (state.results.loading) item(key = "loading") { M1Loading(if (trails == null) "Finding trails…" else "Refreshing trails…") }
            if (state.results.error != null) item(key = "error") {
                TrailsStatusLine(
                    if (trails != null) StatusKind.FAILED else if (state.results.offline) StatusKind.OFFLINE else StatusKind.FAILED,
                    if (trails != null) "Couldn’t refresh · Showing saved trails"
                    else if (state.results.offline) "Offline · Saved trails are still here"
                    else "Couldn’t load trails",
                    actionLabel = if (state.results.loading) null else if (trails == null && state.results.offline) "Open Saved" else "Try again",
                    onAction = { if (trails == null && state.results.offline) state.send(ExploreIntent.OpenSaved) else state.send(ExploreIntent.Retry) },
                )
            }
            if (trails != null) {
                if (trails.isEmpty()) item(key = "empty") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        M1Heading("No trails match yet")
                        Text("Try a wider length or fewer filters.", style = typography.bodyLarge, color = colors.textSecondary)
                        TrailsButton("Clear filters", { state.send(ExploreIntent.ClearFilters) }, tone = ButtonTone.Commit, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    item(key = "count") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${trails.size} ${if (trails.size == 1) "trail" else "trails"}", style = typography.labelLarge, color = colors.textPrimary)
                            SortMenu(state.query.sort) { state.send(ExploreIntent.Sort(it)) }
                        }
                    }
                    items(trails, key = { it.id }) { trail ->
                        TrailSaveCard(trail, state.saved.data, { state.send(ExploreIntent.OpenTrail(trail)) }, { onDismiss -> state.send(ExploreIntent.SaveTrail(trail, onDismiss)) })
                    }
                }
            }
        }
    }
}

/** Applied selectors stay visible beside the chips; the chips themselves open the sheet. */
internal fun appliedSummary(query: TrailQuery): String? {
    val parts = buildList {
        if (query.difficulties.isNotEmpty()) add(query.difficulties.joinToString(", ") { it.displayName() })
        if (query.minMeters > 0 || query.maxMeters != null) add("${query.minMeters / 1000}–${query.maxMeters?.div(1000)?.toString() ?: "50+"} km")
        if (query.minElevationGain > 0 || query.maxElevationGain != null) add("${query.minElevationGain}–${query.maxElevationGain?.toString() ?: "2,000+"} m gain")
        if (query.dogFriendly) add("Dog-friendly")
        if (query.activities.isNotEmpty()) add(query.activities.joinToString(", ") { it.name.lowercase().replace('_', ' ').replaceFirstChar { c -> c.uppercase() } })
        if (query.features.isNotEmpty()) add(query.features.joinToString(", ") { it.name.lowercase().replace('_', ' ').replaceFirstChar { c -> c.uppercase() } })
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun SortMenu(selected: TrailSort, onSelect: (TrailSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, modifier = Modifier.semantics { text = AnnotatedString("Sort by ${selected.displayName()}") }) {
            Text("${selected.displayName()} ⌄", modifier = Modifier.clearAndSetSemantics {}, style = TrailsTheme.typography.labelMedium, color = TrailsTheme.colors.textPrimary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            TrailSort.entries.forEach { sort ->
                DropdownMenuItem(text = { Text(sort.displayName()) }, onClick = { open = false; onSelect(sort) })
            }
        }
    }
}
