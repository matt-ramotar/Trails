package org.mobilenativefoundation.trails.screen.activity

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.screen.explore.M1ScrollPosition

data class ActivityState(
    val history: LoadState<List<CompletedActivity>>,
    val trails: Map<String, Trail>,
    val saved: LoadState<SavedSnapshot>,
    val nowEpochMillis: Long,
    val initialScroll: M1ScrollPosition = M1ScrollPosition(),
    val send: (ActivityIntent) -> Unit,
) : CircuitUiState

sealed interface ActivityIntent : CircuitUiEvent {
    data class ScrollChanged(val position: M1ScrollPosition) : ActivityIntent
    data class OpenTrail(val trailId: String) : ActivityIntent
    data class SaveTrail(val trail: Trail, val onDismiss: () -> Unit) : ActivityIntent
    data object Explore : ActivityIntent
    data object Retry : ActivityIntent
}
