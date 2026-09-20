package org.mobilenativefoundation.trails.screen.signup

import com.slack.circuit.runtime.CircuitUiState

data class SignupState(
    val email: String?,
    val password: String?,
    val step: SignupStep,
    val eventSink: (SignupIntent) -> Unit
) : CircuitUiState

