package org.mobilenativefoundation.trails.screen.collection

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.screen.explore.M1ScrollPosition

data class CollectionState(
    val collectionId: String,
    val content: LoadState<SavedSnapshot>,
    val initialScroll: M1ScrollPosition = M1ScrollPosition(),
    val send: (CollectionIntent) -> Unit,
) : CircuitUiState

sealed interface CollectionIntent : CircuitUiEvent {
    data class ScrollChanged(val position: M1ScrollPosition) : CollectionIntent
    data object Back : CollectionIntent
    data class OpenTrail(val trail: Trail) : CollectionIntent
    data class SaveTrail(val trail: Trail, val onDismiss: () -> Unit) : CollectionIntent
    data object Explore : CollectionIntent
    data object Retry : CollectionIntent
    data object RetrySync : CollectionIntent
}
