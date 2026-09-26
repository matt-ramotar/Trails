package org.mobilenativefoundation.trails.screen.navigate

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.catalog.Trail

data class NavigateState(
    val trail: LoadState<Trail>,
    val toast: String? = null,
    val send: (NavigateIntent) -> Unit,
) : CircuitUiState

sealed interface NavigateIntent : CircuitUiEvent {
    data object OpenTrail : NavigateIntent
    data object StartRecording : NavigateIntent
    data object DismissToast : NavigateIntent
    data object Retry : NavigateIntent
}
