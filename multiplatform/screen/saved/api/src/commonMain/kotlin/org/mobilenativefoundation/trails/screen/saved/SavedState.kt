package org.mobilenativefoundation.trails.screen.saved

import org.mobilenativefoundation.trails.app.navigation.*
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import org.mobilenativefoundation.trails.app.navigation.ScrollPosition
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.saved.SavedSnapshot

data class SavedState(
    val content: LoadState<SavedSnapshot>,
    val allTrails: Boolean,
    val collectionsScroll: ScrollPosition = ScrollPosition(),
    val trailsScroll: ScrollPosition = ScrollPosition(),
    val send: (SavedIntent) -> Unit,
) : CircuitUiState

sealed interface SavedIntent : CircuitUiEvent {
    data class ScrollChanged(val allTrails: Boolean, val position: ScrollPosition) : SavedIntent
    data class SelectSegment(val allTrails: Boolean) : SavedIntent
    data class OpenCollection(val id: String) : SavedIntent
    data class OpenTrail(val trail: Trail) : SavedIntent
    data class SaveTrail(val trail: Trail, val onDismiss: () -> Unit) : SavedIntent
    data object Explore : SavedIntent
    data object Retry : SavedIntent
    data object RetrySync : SavedIntent
}
