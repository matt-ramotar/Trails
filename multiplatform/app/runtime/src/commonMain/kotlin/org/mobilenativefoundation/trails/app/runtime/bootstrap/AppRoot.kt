package org.mobilenativefoundation.trails.app.runtime.bootstrap

import org.mobilenativefoundation.trails.app.runtime.graph.active.ActiveGraph
import org.mobilenativefoundation.trails.app.runtime.graph.inactive.InactiveGraph
import org.mobilenativefoundation.trails.app.runtime.graph.loggedout.LoggedOutGraph

internal sealed interface AppRoot {
    data object Splash : AppRoot
    data class Failed(val message: String) : AppRoot
    data class Welcome(val graph: LoggedOutGraph) : AppRoot
    data class PreLanding(val graph: org.mobilenativefoundation.trails.app.runtime.graph.inactive.InactiveGraph) : AppRoot
    data class Main(val graph: ActiveGraph) : AppRoot
}
