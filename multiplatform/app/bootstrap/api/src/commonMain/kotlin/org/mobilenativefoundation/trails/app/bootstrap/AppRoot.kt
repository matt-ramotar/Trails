package org.mobilenativefoundation.trails.app.bootstrap

import org.mobilenativefoundation.trails.di.graph.active.ActiveGraph
import org.mobilenativefoundation.trails.di.graph.inactive.InactiveGraph
import org.mobilenativefoundation.trails.di.graph.loggedout.LoggedOutGraph

sealed interface AppRoot {
    data object Splash : AppRoot
    data class Failed(val message: String) : AppRoot
    data class Welcome(val graph: LoggedOutGraph) : AppRoot
    data class PreLanding(val graph: org.mobilenativefoundation.trails.di.graph.inactive.InactiveGraph) : AppRoot
    data class Main(val graph: ActiveGraph) : AppRoot
}
