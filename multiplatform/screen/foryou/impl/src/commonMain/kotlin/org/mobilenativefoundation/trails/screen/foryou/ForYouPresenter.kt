package org.mobilenativefoundation.trails.screen.foryou

import org.mobilenativefoundation.trails.app.navigation.*
import androidx.compose.runtime.*
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.mobilenativefoundation.trails.feature.savetrail.SaveTrailFeature
import org.mobilenativefoundation.trails.app.navigation.AppNavigation
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.data.trail.catalog.TrailRepository
import org.mobilenativefoundation.trails.data.trail.recommendation.ForYouRepository
import org.mobilenativefoundation.trails.data.trail.saved.SavedRepository

@Inject
class ForYouPresenter(
    private val feed: ForYouRepository,
    private val trails: TrailRepository,
    private val savedRepository: SavedRepository,
    private val navigation: AppNavigation,
    private val saves: SaveTrailFeature,
) : Presenter<ForYouState> {
    @Composable
    override fun present(): ForYouState {
        var retryAttempt by remember { mutableIntStateOf(0) }
        val content = remember(retryAttempt) {
            feed.observe().retainDataOnFailure("Couldn’t load your picks")
        }.collectAsState(initial = LoadState()).value
        val catalog = remember(retryAttempt) {
            trails.observeQuery(TrailQuery()).retainDataOnFailure("Couldn’t load this trail")
        }.collectAsState(initial = LoadState()).value
        val saved by savedRepository.state.collectAsState()
        val scope = rememberCoroutineScope()
        var refreshing by remember { mutableStateOf(false) }
        var refreshErrors by remember { mutableStateOf<List<String>>(emptyList()) }
        // The existing feed load state describes readiness and failures of the whole root.
        // Keep its cached payload while the independently observed catalog loads or fails.
        val root = content.copy(
            loading = content.loading || catalog.loading || refreshing,
            error = (listOfNotNull(content.error, catalog.error) + refreshErrors).distinct().takeIf { it.isNotEmpty() }?.joinToString(" · "),
            offline = content.offline || catalog.offline,
        )
        return ForYouState(root, catalog.data.orEmpty().associateBy { it.id }, saved, navigation.scrollPosition("FOR_YOU/foryou")) { intent ->
            when (intent) {
                is ForYouIntent.ScrollChanged -> navigation.checkpointScroll("FOR_YOU/foryou", intent.position)
                is ForYouIntent.OpenTrail -> navigation.openTrail(intent.trail.id)
                is ForYouIntent.SaveTrail -> saves.open(intent.trail, intent.onDismiss)
                ForYouIntent.Retry -> if (!refreshing) scope.launch {
                    refreshing = true
                    refreshErrors = emptyList()
                    retryAttempt++ // Also reconnect an observer that terminated with an exception.
                    suspend fun refresh(action: suspend () -> Unit) {
                        try { action() }
                        catch (failure: Exception) {
                            if (failure is CancellationException) throw failure
                            refreshErrors = refreshErrors + (failure.message ?: "Couldn’t load your picks")
                        }
                    }
                    try {
                        refresh { feed.refresh() }
                        refresh { trails.refreshQuery(TrailQuery()) }
                    } finally { refreshing = false }
                }
            }
        }
    }
}

/** An exceptional stream termination must not erase its last cached content or offline state. */
private fun <T> Flow<LoadState<T>>.retainDataOnFailure(fallback: String): Flow<LoadState<T>> {
    var latest = LoadState<T>()
    return onEach { latest = it }.catch { failure ->
        if (failure is CancellationException) throw failure
        emit(latest.copy(loading = false, error = failure.message ?: fallback))
    }
}
