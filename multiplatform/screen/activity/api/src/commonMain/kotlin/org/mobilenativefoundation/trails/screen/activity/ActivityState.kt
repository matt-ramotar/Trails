package org.mobilenativefoundation.trails.screen.activity

import org.mobilenativefoundation.trails.app.navigation.*
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import org.mobilenativefoundation.trails.app.navigation.ScrollPosition
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.activity.CompletedActivity
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.saved.SavedSnapshot

data class ActivityState(
    val history: LoadState<List<CompletedActivity>>,
    val trails: Map<String, Trail>,
    val saved: LoadState<SavedSnapshot>,
    val nowEpochMillis: Long,
    val initialScroll: ScrollPosition = ScrollPosition(),
    val send: (ActivityIntent) -> Unit,
) : CircuitUiState

sealed interface ActivityIntent : CircuitUiEvent {
    data class ScrollChanged(val position: ScrollPosition) : ActivityIntent
    data class OpenTrail(val trailId: String) : ActivityIntent
    data class SaveTrail(val trail: Trail, val onDismiss: () -> Unit) : ActivityIntent
    data object Explore : ActivityIntent
    data object Retry : ActivityIntent
}
