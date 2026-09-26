package org.mobilenativefoundation.trails.feature.filters

import org.mobilenativefoundation.trails.ui.trail.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import org.mobilenativefoundation.trails.ui.trail.displayName
import org.mobilenativefoundation.trails.ui.trail.markerKind
import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailActivity
import org.mobilenativefoundation.trails.data.trail.catalog.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.catalog.TrailFeature
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.data.trail.catalog.TrailRepository

@Inject
class RealFiltersFeature(private val repository: TrailRepository) : FiltersFeature {
    private class Request(val original: TrailQuery, val section: FilterSection, val result: CompletableDeferred<TrailQuery?>, val onDismiss: () -> Unit)
    private var request by mutableStateOf<Request?>(null)

    override suspend fun show(query: TrailQuery, section: FilterSection, onDismiss: () -> Unit): TrailQuery? {
        request?.result?.complete(null)
        val next = Request(query, section, CompletableDeferred(), onDismiss)
        request = next
        var completed = false
        return try { next.result.await().also { completed = true } } finally {
            if (request === next) {
                request = null
                // Account/screen cancellation or replacement never pulls focus to an old caller.
                if (completed) next.onDismiss()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val current = request ?: return
        key(current) {
            val colors = TrailsTheme.colors
            val typography = TrailsTheme.typography
            var draft by remember { mutableStateOf(current.original) }
            var count by remember { mutableStateOf(LoadState<Int>()) }
            var countFor by remember { mutableStateOf<TrailQuery?>(null) }
            var retry by remember { mutableIntStateOf(0) }
            val scroll = rememberScrollState()
            val sectionOffsets = remember { mutableStateMapOf<FilterSection, Int>() }
            fun update(next: TrailQuery) { draft = next; count = LoadState(); countFor = null }
            LaunchedEffect(draft, retry) {
                val selected = draft
                delay(180)
                val result = try { repository.count(selected.normalized()) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { LoadState<Int>(loading = false, error = failure.message ?: "Couldn’t count trails") }
                if (draft == selected) { count = result; countFor = selected }
            }
            val sectionTarget = sectionOffsets[current.section]
            LaunchedEffect(current.section, sectionTarget) {
                if (current.section != FilterSection.ALL && sectionTarget != null) scroll.animateScrollTo(sectionTarget)
            }
            val matchingCount = countFor == draft && !count.loading
            val rangeColors = SliderDefaults.colors(thumbColor = colors.textPrimary, activeTrackColor = colors.textPrimary, inactiveTrackColor = colors.border)
            ModalBottomSheet(
                onDismissRequest = { current.result.complete(null) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = colors.surface,
                shape = RoundedCornerShape(topStart = TrailsTheme.radii.sheet, topEnd = TrailsTheme.radii.sheet),
            ) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(scroll).padding(horizontal = 24.dp)
                        .navigationBarsPadding().imePadding().padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Filters", style = typography.headlineMedium, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                        TrailsIconCircle(Icons.Outlined.Cancel.painter, "Close filters", { current.result.complete(null) }, container = colors.soft)
                    }

                    SectionHeading("Difficulty", FilterSection.DIFFICULTY, sectionOffsets)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrailDifficulty.entries.forEach { difficulty ->
                            TrailsFilterChip(
                                difficulty.displayName(), difficulty in draft.difficulties,
                                { update(draft.copy(difficulties = if (difficulty in draft.difficulties) draft.difficulties - difficulty else draft.difficulties + difficulty)) },
                                leadingIcon = { DifficultyMarker(difficulty.markerKind()) },
                            )
                        }
                    }

                    SectionHeading("Length", FilterSection.LENGTH, sectionOffsets)
                    val minimumKm = draft.minMeters / 1000
                    val maximumKm = (draft.maxMeters ?: MAX_LENGTH_METERS) / 1000
                    Text("$minimumKm–${draft.maxMeters?.let { "$maximumKm" } ?: "${MAX_LENGTH_METERS / 1000}+"} km", style = typography.bodyMedium, color = colors.textSecondary)
                    Column {
                        Text("Minimum length · $minimumKm km", style = typography.bodyMedium, color = colors.textPrimary)
                        Slider(
                            value = minimumKm.toFloat(),
                            onValueChange = { value -> update(draft.copy(minMeters = value.roundToInt().coerceIn(0, maximumKm) * 1000)) },
                            enabled = maximumKm > 0,
                            valueRange = 0f..maximumKm.toFloat(),
                            steps = (maximumKm - 1).coerceAtLeast(0),
                            colors = rangeColors,
                            track = { StepTrack(it, rangeColors, maximumKm > 0) },
                            modifier = Modifier.semantics { text = AnnotatedString("Minimum length"); stateDescription = "$minimumKm kilometers" },
                        )
                    }
                    Column {
                        Text("Maximum length · ${draft.maxMeters?.let { "$maximumKm km" } ?: "No maximum"}", style = typography.bodyMedium, color = colors.textPrimary)
                        Slider(
                            value = maximumKm.toFloat(),
                            onValueChange = { value ->
                                val maximum = value.roundToInt().coerceIn(minimumKm, MAX_LENGTH_METERS / 1000) * 1000
                                update(draft.copy(maxMeters = maximum.takeIf { it < MAX_LENGTH_METERS }))
                            },
                            enabled = minimumKm < MAX_LENGTH_METERS / 1000,
                            valueRange = minimumKm.toFloat()..(MAX_LENGTH_METERS / 1000).toFloat(),
                            steps = (MAX_LENGTH_METERS / 1000 - minimumKm - 1).coerceAtLeast(0),
                            colors = rangeColors,
                            track = { StepTrack(it, rangeColors, minimumKm < MAX_LENGTH_METERS / 1000) },
                            modifier = Modifier.semantics { text = AnnotatedString("Maximum length"); stateDescription = draft.maxMeters?.let { "$maximumKm kilometers" } ?: "No maximum length" },
                        )
                    }

                    SectionHeading("Elevation gain", FilterSection.ELEVATION, sectionOffsets)
                    MaximumSlider(
                        label = "Maximum elevation gain", value = draft.maxElevationGain, top = MAX_GAIN_METERS, step = 100, colors = rangeColors,
                        onChange = { update(draft.copy(maxElevationGain = it)) },
                    )

                    SwitchRow("Dog-friendly", draft.dogFriendly) { update(draft.copy(dogFriendly = it)) }

                    Text("Activity", style = typography.titleLarge, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrailActivity.entries.forEach { activity ->
                            TrailsFilterChip(activity.displayName(), activity in draft.activities, {
                                update(draft.copy(activities = if (activity in draft.activities) draft.activities - activity else draft.activities + activity))
                            })
                        }
                    }

                    Text("Trail features", style = typography.titleLarge, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                    listOf(TrailFeature.LAKE to "Lakes & water", TrailFeature.FOREST to "Forest shade", TrailFeature.SUMMIT to "Big views").forEach { (feature, label) ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleableRow(feature in draft.features) {
                                update(draft.copy(features = if (feature in draft.features) draft.features - feature else draft.features + feature))
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = feature in draft.features, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = colors.accent))
                            Spacer(Modifier.width(12.dp))
                            Text(label, style = typography.bodyLarge, color = colors.textPrimary)
                        }
                    }

                    if (matchingCount && count.error != null) TrailsStatusLine(StatusKind.FAILED, "Couldn’t count trails · Filters are kept", actionLabel = "Retry count", onAction = { count = LoadState(); countFor = null; retry++ })
                    TrailsButton(
                        text = when { !matchingCount -> "Counting trails…"; count.data != null -> "Show ${count.data} ${if (count.data == 1) "trail" else "trails"}"; else -> "Apply filters" },
                        onClick = { current.result.complete(draft) },
                        tone = ButtonTone.Commit,
                        enabled = matchingCount,
                        loading = !matchingCount,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TrailsButton("Cancel", { current.result.complete(null) }, tone = ButtonTone.Secondary, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private const val MAX_LENGTH_METERS = 50_000
private const val MAX_GAIN_METERS = 2_000

private fun metres(value: Int): String = value.toString().reversed().chunked(3).joinToString(",").reversed() + " m"

internal fun TrailActivity.displayName(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

@Composable
private fun SectionHeading(text: String, section: FilterSection, offsets: MutableMap<FilterSection, Int>) {
    Text(
        text, style = TrailsTheme.typography.titleLarge, color = TrailsTheme.colors.textPrimary,
        modifier = Modifier.onGloballyPositioned { offsets[section] = it.positionInParent().y.roundToInt() }.semantics { heading() },
    )
}

/** Labelled native slider whose top stop means no maximum. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaximumSlider(label: String, value: Int?, top: Int, step: Int, colors: SliderColors, onChange: (Int?) -> Unit) {
    val typography = TrailsTheme.typography
    val palette = TrailsTheme.colors
    val shown = value?.let { metres(it) } ?: "No maximum"
    Column {
        Text("$label · $shown", style = typography.bodyMedium, color = palette.textPrimary)
        Slider(
            value = (value ?: top).toFloat(),
            onValueChange = { raw -> val rounded = (raw / step).roundToInt() * step; onChange(rounded.takeIf { it < top }) },
            valueRange = 0f..top.toFloat(),
            steps = (top / step - 1).coerceAtLeast(0),
            colors = colors,
            track = { StepTrack(it, colors) },
            modifier = Modifier.semantics { text = AnnotatedString(label); stateDescription = shown },
        )
    }
}

/** The stepped values stay for keyboard and accessibility increments; dense tick dots are omitted from the filter sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepTrack(state: SliderState, colors: SliderColors, enabled: Boolean = true) {
    SliderDefaults.Track(sliderState = state, enabled = enabled, colors = colors, drawTick = { _, _ -> })
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = TrailsTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = TrailsTheme.typography.bodyLarge, color = colors.textPrimary)
        Switch(checked = checked, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = colors.dark, checkedThumbColor = colors.surface))
    }
}

private fun Modifier.toggleableRow(checked: Boolean, onClick: () -> Unit): Modifier =
    toggleable(value = checked, role = Role.Checkbox, onValueChange = { onClick() })
