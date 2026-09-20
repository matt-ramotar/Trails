package org.mobilenativefoundation.trails.app.scaffold

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class ScaffoldState(
    val selectedBottomNavItem: TrailsBottomNavItem,
    val bottomNavItems: List<TrailsBottomNavItem>,
    val send: (ScaffoldIntent) -> Unit,
) : CircuitUiState