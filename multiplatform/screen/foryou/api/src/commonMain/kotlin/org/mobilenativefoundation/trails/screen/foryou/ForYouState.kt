package org.mobilenativefoundation.trails.screen.foryou

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.screen.explore.M1ScrollPosition

data class ForYouState(
    val feed: LoadState<ForYouFeed>,
    val trails: Map<String, Trail>,
    val saved: LoadState<SavedSnapshot>,
    val initialScroll: M1ScrollPosition = M1ScrollPosition(),
    val send: (ForYouIntent) -> Unit,
) : CircuitUiState

sealed interface ForYouIntent : CircuitUiEvent {
    data class ScrollChanged(val position: M1ScrollPosition) : ForYouIntent
    data class OpenTrail(val trail: Trail) : ForYouIntent
    data class SaveTrail(val trail: Trail, val onDismiss: () -> Unit) : ForYouIntent
    data object Retry : ForYouIntent
}
