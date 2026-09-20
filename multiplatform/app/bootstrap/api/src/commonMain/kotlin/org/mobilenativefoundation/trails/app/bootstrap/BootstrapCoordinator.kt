package org.mobilenativefoundation.trails.app.bootstrap

import kotlinx.coroutines.flow.StateFlow


interface BootstrapCoordinator {
    val route: StateFlow<org.mobilenativefoundation.trails.app.bootstrap.AppRoot>
    fun start()
    /** Replays the current restored session after a startup failure. */
    fun retry() = Unit
}
