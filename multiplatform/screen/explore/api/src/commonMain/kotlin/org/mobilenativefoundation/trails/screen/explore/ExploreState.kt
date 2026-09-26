package org.mobilenativefoundation.trails.screen.explore

import org.mobilenativefoundation.trails.app.navigation.*
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import org.mobilenativefoundation.trails.feature.filters.FilterSection
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.data.trail.catalog.TrailSort
import org.mobilenativefoundation.trails.data.trail.saved.SavedSnapshot

data class ExploreState(
    val text: String,
    val query: TrailQuery,
    val results: LoadState<List<Trail>>,
    val saved: LoadState<SavedSnapshot>,
    val initialScroll: ScrollPosition = ScrollPosition(),
    val send: (ExploreIntent) -> Unit,
) : CircuitUiState

sealed interface ExploreIntent : CircuitUiEvent {
    data class ScrollChanged(val position: ScrollPosition) : ExploreIntent
    data class QueryChanged(val text: String) : ExploreIntent
    data object SubmitSearch : ExploreIntent
    data object ClearQuery : ExploreIntent
    data class Filters(val section: FilterSection = FilterSection.ALL, val onDismiss: () -> Unit) : ExploreIntent
    data object ClearFilters : ExploreIntent
    data class Sort(val sort: TrailSort) : ExploreIntent
    data class OpenTrail(val trail: Trail) : ExploreIntent
    data class SaveTrail(val trail: Trail, val onDismiss: () -> Unit) : ExploreIntent
    data object Retry : ExploreIntent
    data object OpenSaved : ExploreIntent
}
