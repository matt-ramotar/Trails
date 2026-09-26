package org.mobilenativefoundation.trails.screen.explore

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.feat.filters.FiltersFeature
import org.mobilenativefoundation.trails.feat.savetrail.SaveTrailFeature

@Inject
class ExplorePresenter(
    private val repository: TrailRepository,
    private val savedRepository: SavedRepository,
    private val navigation: M1Navigation,
    private val filters: FiltersFeature,
    private val saves: SaveTrailFeature,
) : Presenter<ExploreState> {
    @Composable
    override fun present(): ExploreState {
        var text by rememberSaveable { mutableStateOf(navigation.exploreView.text) }
        var submittedText by rememberSaveable { mutableStateOf(navigation.exploreView.query.text) }
        var selectorsJson by rememberSaveable { mutableStateOf(Json.encodeToString(TrailQuery.serializer(), restoredExploreSelectors(navigation.exploreView.query))) }
        val selectors = remember(selectorsJson) { restoredExploreSelectors(Json.decodeFromString(TrailQuery.serializer(), selectorsJson)) }
        val query = remember(selectors, submittedText) { selectors.copy(text = submittedText).normalized() }
        fun setSelectors(value: TrailQuery) { selectorsJson = Json.encodeToString(TrailQuery.serializer(), value.copy(text = "")) }
        LaunchedEffect(text) { delay(300); submittedText = text }
        SideEffect { navigation.checkpointExplore(M1ExploreView(text, query)) }
        val results = key(query) {
            val flow = remember(query) { repository.observeQuery(query).catch { failure ->
                if (failure is CancellationException) throw failure
                emit(LoadState(loading = false, error = failure.message ?: "Couldn’t load trails"))
            } }
            flow.collectAsState(initial = LoadState()).value
        }
        val saved by savedRepository.state.collectAsState()
        val scope = rememberCoroutineScope()
        var refreshing by remember(query) { mutableStateOf(false) }
        var actionError by remember(query) { mutableStateOf<String?>(null) }
        return ExploreState(text, query, results.copy(loading = results.loading || refreshing, error = actionError ?: results.error), saved, navigation.scrollPosition("EXPLORE/explore")) { intent ->
            when (intent) {
                is ExploreIntent.ScrollChanged -> navigation.checkpointScroll("EXPLORE/explore", intent.position)
                is ExploreIntent.QueryChanged -> text = intent.text
                ExploreIntent.SubmitSearch -> submittedText = text
                ExploreIntent.ClearQuery -> { text = ""; submittedText = "" }
                is ExploreIntent.Filters -> scope.launch {
                    filters.show(query.copy(text = text), intent.section, intent.onDismiss)?.let { setSelectors(it); submittedText = text }
                }
                ExploreIntent.ClearFilters -> setSelectors(clearedExploreSelectors(selectors))
                is ExploreIntent.Sort -> setSelectors(selectors.copy(sort = intent.sort))
                is ExploreIntent.OpenTrail -> navigation.openTrail(intent.trail.id)
                is ExploreIntent.SaveTrail -> saves.open(intent.trail, intent.onDismiss)
                ExploreIntent.OpenSaved -> navigation.selectSaved()
                ExploreIntent.Retry -> if (!refreshing) scope.launch {
                    refreshing = true
                    actionError = null
                    try { repository.refreshQuery(query) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (failure: Exception) { actionError = failure.message ?: "Couldn’t refresh trails" }
                    finally { refreshing = false }
                }
            }
        }
    }
}

/** Clearing filters clears filters; the chosen order is not one of them. */
internal fun clearedExploreSelectors(current: TrailQuery): TrailQuery = TrailQuery(sort = current.sort)

/** Quick area selectors are retired with R2: a checkpointed region is cleared; text is re-entered from the field. */
internal fun restoredExploreSelectors(query: TrailQuery): TrailQuery = query.normalized().copy(text = "", region = null)
