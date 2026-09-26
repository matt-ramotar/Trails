package org.mobilenativefoundation.trails.screen.collection

import org.mobilenativefoundation.trails.app.navigation.*
import androidx.compose.runtime.*
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.mobilenativefoundation.trails.data.trail.saved.SavedRepository
import org.mobilenativefoundation.trails.feature.savetrail.SaveTrailFeature
import org.mobilenativefoundation.trails.app.navigation.AppNavigation

@Inject
class CollectionPresenter(
    private val screen: CollectionScreen,
    private val repository: SavedRepository,
    private val navigation: AppNavigation,
    private val saves: SaveTrailFeature,
) : Presenter<CollectionState> {
    @Composable
    override fun present(): CollectionState {
        val content by repository.state.collectAsState()
        val viewKey = remember(screen.collectionId) { navigation.viewKey("collection:${screen.collectionId}") }
        val scope = rememberCoroutineScope()
        var refreshing by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        return CollectionState(screen.collectionId, content.copy(loading = content.loading || refreshing, error = error ?: content.error), navigation.scrollPosition(viewKey)) { intent ->
            when (intent) {
                is CollectionIntent.ScrollChanged -> navigation.checkpointScroll(viewKey, intent.position)
                CollectionIntent.Back -> navigation.back()
                is CollectionIntent.OpenTrail -> navigation.openTrail(intent.trail.id)
                is CollectionIntent.SaveTrail -> saves.open(intent.trail, intent.onDismiss)
                CollectionIntent.Explore -> navigation.selectExplore()
                CollectionIntent.Retry, CollectionIntent.RetrySync -> if (!refreshing) scope.launch {
                    refreshing = true; error = null
                    try { if (intent == CollectionIntent.Retry) repository.refresh() else repository.retryPending() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (failure: Exception) { error = failure.message ?: "This collection is unavailable" }
                    finally { refreshing = false }
                }
            }
        }
    }
}
