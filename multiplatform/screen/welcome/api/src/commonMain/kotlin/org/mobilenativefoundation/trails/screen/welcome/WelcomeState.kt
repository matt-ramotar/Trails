package org.mobilenativefoundation.trails.screen.welcome

import com.slack.circuit.runtime.CircuitUiState

data class WelcomeState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val eventSink: (WelcomeIntent) -> Unit
) : CircuitUiState
