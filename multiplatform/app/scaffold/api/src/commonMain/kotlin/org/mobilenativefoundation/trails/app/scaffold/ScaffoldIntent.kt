package org.mobilenativefoundation.trails.app.scaffold

import com.slack.circuit.runtime.CircuitUiEvent

sealed interface ScaffoldIntent : CircuitUiEvent {
    data class SelectBottomNavItem(val item: TrailsBottomNavItem) : ScaffoldIntent
}