package org.mobilenativefoundation.trails.screen.home.child.post

import com.slack.circuit.runtime.CircuitUiState
import kotlinx.serialization.Serializable

@Serializable
data class PostState(
    val id: String
) : CircuitUiState

