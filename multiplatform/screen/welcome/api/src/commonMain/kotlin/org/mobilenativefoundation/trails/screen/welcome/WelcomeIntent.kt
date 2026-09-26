package org.mobilenativefoundation.trails.screen.welcome

import com.slack.circuit.runtime.CircuitUiEvent

sealed interface WelcomeIntent : CircuitUiEvent {
    data object ExploreSampleTrails : WelcomeIntent
}
