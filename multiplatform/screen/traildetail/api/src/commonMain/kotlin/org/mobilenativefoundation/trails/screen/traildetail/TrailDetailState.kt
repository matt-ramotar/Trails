package org.mobilenativefoundation.trails.screen.traildetail

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import org.mobilenativefoundation.trails.data.trail.*

data class TrailDetailState(
    val trail: LoadState<Trail>,
    val saved: LoadState<SavedSnapshot>,
    val initialScrollOffset: Int = 0,
    val send: (TrailDetailIntent) -> Unit,
) : CircuitUiState

sealed interface TrailDetailIntent : CircuitUiEvent {
    data class ScrollChanged(val offset: Int) : TrailDetailIntent
    data object Back : TrailDetailIntent
    data class Save(val onDismiss: () -> Unit) : TrailDetailIntent
    data object Retry : TrailDetailIntent
    data object RetrySync : TrailDetailIntent
    data object OpenSaved : TrailDetailIntent
}
