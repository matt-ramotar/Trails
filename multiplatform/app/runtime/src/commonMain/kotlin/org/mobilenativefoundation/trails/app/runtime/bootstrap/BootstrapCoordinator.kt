package org.mobilenativefoundation.trails.app.runtime.bootstrap

import kotlinx.coroutines.flow.StateFlow


internal interface BootstrapCoordinator {
    val route: StateFlow<AppRoot>
    fun start()
    /** Replays the current restored session after a startup failure. */
    fun retry() = Unit
}
