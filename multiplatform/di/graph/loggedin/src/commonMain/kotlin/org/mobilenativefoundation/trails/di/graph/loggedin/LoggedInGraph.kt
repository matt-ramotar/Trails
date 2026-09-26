package org.mobilenativefoundation.trails.di.graph.loggedin

import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import org.mobilenativefoundation.trails.di.graph.active.ActiveGraph
import org.mobilenativefoundation.trails.di.graph.inactive.InactiveGraph
import org.mobilenativefoundation.trails.foundation.scope.LoggedInScope
import org.mobilenativefoundation.trails.model.domain.user.UserSession

@GraphExtension(
    scope = LoggedInScope::class
)
interface LoggedInGraph {

    val userSession: UserSession

    val inactive: InactiveGraph.Factory
    val active: ActiveGraph.Factory

    @ContributesTo(AppScope::class)
    @GraphExtension.Factory
    interface Factory {
        fun createLoggedInGraph(
            @Provides userSession: UserSession,
            @Provides backstack: SaveableBackStack,
            @Provides navigator: Navigator,
        ): LoggedInGraph
    }
}