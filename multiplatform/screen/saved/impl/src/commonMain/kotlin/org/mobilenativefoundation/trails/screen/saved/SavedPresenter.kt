package org.mobilenativefoundation.trails.screen.saved

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.mobilenativefoundation.trails.data.trail.SavedRepository
import org.mobilenativefoundation.trails.feat.savetrail.SaveTrailFeature
import org.mobilenativefoundation.trails.screen.explore.M1Navigation

@Inject
class SavedPresenter(
    private val repository: SavedRepository,
    private val navigation: M1Navigation,
    private val saves: SaveTrailFeature,
) : Presenter<SavedState> {
    @Composable
    override fun present(): SavedState {
        val content by repository.state.collectAsState()
        var allTrails by rememberSaveable { mutableStateOf(navigation.savedAllTrails) }
        val scope = rememberCoroutineScope()
        var refreshing by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        return SavedState(content.copy(loading = content.loading || refreshing, error = error ?: content.error), allTrails,
            navigation.scrollPosition("SAVED/saved:collections"), navigation.scrollPosition("SAVED/saved:trails")) { intent ->
            when (intent) {
                is SavedIntent.ScrollChanged -> navigation.checkpointScroll(if (intent.allTrails) "SAVED/saved:trails" else "SAVED/saved:collections", intent.position)
                is SavedIntent.SelectSegment -> { allTrails = intent.allTrails; navigation.checkpointSavedSegment(allTrails) }
                is SavedIntent.OpenCollection -> navigation.selectSaved(intent.id)
                is SavedIntent.OpenTrail -> navigation.openTrail(intent.trail.id)
                is SavedIntent.SaveTrail -> saves.open(intent.trail, intent.onDismiss)
                SavedIntent.Explore -> navigation.selectExplore()
                SavedIntent.Retry, SavedIntent.RetrySync -> if (!refreshing) scope.launch {
                    refreshing = true; error = null
                    try { if (intent == SavedIntent.Retry) repository.refresh() else repository.retryPending() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (failure: Exception) { error = failure.message ?: "Your saved trails are unavailable" }
                    finally { refreshing = false }
                }
            }
        }
    }
}
