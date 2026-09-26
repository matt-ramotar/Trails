package org.mobilenativefoundation.trails.screen.collection

import org.mobilenativefoundation.trails.app.navigation.*
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import org.mobilenativefoundation.trails.app.navigation.ScrollPosition
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.saved.SavedSnapshot

data class CollectionState(
    val collectionId: String,
    val content: LoadState<SavedSnapshot>,
    val initialScroll: ScrollPosition = ScrollPosition(),
    val send: (CollectionIntent) -> Unit,
) : CircuitUiState

sealed interface CollectionIntent : CircuitUiEvent {
    data class ScrollChanged(val position: ScrollPosition) : CollectionIntent
    data object Back : CollectionIntent
    data class OpenTrail(val trail: Trail) : CollectionIntent
    data class SaveTrail(val trail: Trail, val onDismiss: () -> Unit) : CollectionIntent
    data object Explore : CollectionIntent
    data object Retry : CollectionIntent
    data object RetrySync : CollectionIntent
}
