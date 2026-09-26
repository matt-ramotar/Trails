package org.mobilenativefoundation.trails.screen.foryou

import org.mobilenativefoundation.trails.app.navigation.*
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import org.mobilenativefoundation.trails.app.navigation.ScrollPosition
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.recommendation.ForYouFeed
import org.mobilenativefoundation.trails.data.trail.saved.SavedSnapshot

data class ForYouState(
    val feed: LoadState<ForYouFeed>,
    val trails: Map<String, Trail>,
    val saved: LoadState<SavedSnapshot>,
    val initialScroll: ScrollPosition = ScrollPosition(),
    val send: (ForYouIntent) -> Unit,
) : CircuitUiState

sealed interface ForYouIntent : CircuitUiEvent {
    data class ScrollChanged(val position: ScrollPosition) : ForYouIntent
    data class OpenTrail(val trail: Trail) : ForYouIntent
    data class SaveTrail(val trail: Trail, val onDismiss: () -> Unit) : ForYouIntent
    data object Retry : ForYouIntent
}
