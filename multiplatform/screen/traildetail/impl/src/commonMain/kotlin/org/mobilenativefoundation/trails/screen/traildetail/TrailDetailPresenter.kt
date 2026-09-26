package org.mobilenativefoundation.trails.screen.traildetail

import org.mobilenativefoundation.trails.app.navigation.*
import androidx.compose.runtime.*
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.mobilenativefoundation.trails.feature.savetrail.SaveTrailFeature
import org.mobilenativefoundation.trails.app.navigation.AppNavigation
import org.mobilenativefoundation.trails.app.navigation.ScrollPosition
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.catalog.TrailRepository
import org.mobilenativefoundation.trails.data.trail.saved.SavedRepository

@Inject
class TrailDetailPresenter(
    private val screen: TrailDetailScreen,
    private val repository: TrailRepository,
    private val savedRepository: SavedRepository,
    private val navigation: AppNavigation,
    private val saves: SaveTrailFeature,
) : Presenter<TrailDetailState> {
    @Composable
    override fun present(): TrailDetailState {
        val viewKey = remember(screen.trailId) { navigation.viewKey("trail:${screen.trailId}") }
        val trail = key(screen.trailId) {
            remember(screen.trailId) { repository.observeTrail(screen.trailId).catch { failure ->
                if (failure is CancellationException) throw failure
                emit(LoadState(loading = false, error = failure.message ?: "Couldn’t load this trail"))
            } }.collectAsState(initial = LoadState()).value
        }
        val saved by savedRepository.state.collectAsState()
        val scope = rememberCoroutineScope()
        var refreshing by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        return TrailDetailState(trail.copy(loading = trail.loading || refreshing, error = error ?: trail.error), saved, navigation.scrollPosition(viewKey).offset) { intent ->
            when (intent) {
                is TrailDetailIntent.ScrollChanged -> navigation.checkpointScroll(viewKey, ScrollPosition(offset = intent.offset))
                TrailDetailIntent.Back -> navigation.back()
                is TrailDetailIntent.Save -> trail.data?.let { saves.open(it, intent.onDismiss) }
                TrailDetailIntent.OpenSaved -> navigation.selectSaved()
                TrailDetailIntent.Retry, TrailDetailIntent.RetrySync -> if (!refreshing) scope.launch {
                    refreshing = true; error = null
                    try {
                        if (intent == TrailDetailIntent.Retry) repository.refreshTrail(screen.trailId) else savedRepository.retryPending()
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (failure: Exception) { error = failure.message ?: "Couldn’t refresh this trail" }
                    finally { refreshing = false }
                }
            }
        }
    }
}
