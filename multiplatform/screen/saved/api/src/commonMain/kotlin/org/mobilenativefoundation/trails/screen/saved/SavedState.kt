package org.mobilenativefoundation.trails.screen.saved

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.screen.explore.M1ScrollPosition

data class SavedState(
    val content: LoadState<SavedSnapshot>,
    val allTrails: Boolean,
    val collectionsScroll: M1ScrollPosition = M1ScrollPosition(),
    val trailsScroll: M1ScrollPosition = M1ScrollPosition(),
    val send: (SavedIntent) -> Unit,
) : CircuitUiState

sealed interface SavedIntent : CircuitUiEvent {
    data class ScrollChanged(val allTrails: Boolean, val position: M1ScrollPosition) : SavedIntent
    data class SelectSegment(val allTrails: Boolean) : SavedIntent
    data class OpenCollection(val id: String) : SavedIntent
    data class OpenTrail(val trail: Trail) : SavedIntent
    data class SaveTrail(val trail: Trail, val onDismiss: () -> Unit) : SavedIntent
    data object Explore : SavedIntent
    data object Retry : SavedIntent
    data object RetrySync : SavedIntent
}
