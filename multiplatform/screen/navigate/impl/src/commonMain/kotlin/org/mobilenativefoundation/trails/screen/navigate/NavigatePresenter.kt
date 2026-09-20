package org.mobilenativefoundation.trails.screen.navigate

import androidx.compose.runtime.*
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.screen.explore.M1Navigation

const val RECORDING_COMING_SOON = "Recording is coming soon"
internal const val DEFAULT_NAVIGATE_TRAIL = "half-dome"

/** The trail Navigate previews: last opened, else the first saved trail in saved order, else Half Dome. */
internal fun navigateTrailId(lastOpened: String?, saved: SavedSnapshot?): String =
    lastOpened
        ?: saved?.let { snapshot -> snapshot.trails.firstOrNull { snapshot.memberships[it.id]?.isNotEmpty() == true }?.id }
        ?: DEFAULT_NAVIGATE_TRAIL

@Inject
class NavigatePresenter(
    private val repository: TrailRepository,
    private val savedRepository: SavedRepository,
    private val navigation: M1Navigation,
) : Presenter<NavigateState> {
    @Composable
    override fun present(): NavigateState {
        val saved by savedRepository.state.collectAsState()
        val trailId = navigateTrailId(navigation.lastOpenedTrailId, saved.data)
        val trail = key(trailId) {
            remember(trailId) { repository.observeTrail(trailId).catch { failure ->
                if (failure is CancellationException) throw failure
                emit(LoadState(loading = false, error = failure.message ?: "Couldn’t load this trail"))
            } }.collectAsState(initial = LoadState()).value
        }
        var toast by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        var refreshing by remember(trailId) { mutableStateOf(false) }
        return NavigateState(trail.copy(loading = trail.loading || refreshing), toast) { intent ->
            when (intent) {
                NavigateIntent.OpenTrail -> navigation.openTrail(trailId)
                NavigateIntent.StartRecording -> toast = RECORDING_COMING_SOON
                NavigateIntent.DismissToast -> toast = null
                NavigateIntent.Retry -> if (!refreshing) scope.launch {
                    refreshing = true
                    try { repository.refreshTrail(trailId) } finally { refreshing = false }
                }
            }
        }
    }
}
