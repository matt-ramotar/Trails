package org.mobilenativefoundation.trails.app.runtime.graph.inactive

import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.mobilenativefoundation.trails.foundation.scope.InactiveScope
import org.mobilenativefoundation.trails.foundation.scope.LoggedInScope
import org.mobilenativefoundation.trails.foundation.scope.LoggedOutScope
import org.mobilenativefoundation.trails.data.session.model.InactiveUser

@GraphExtension(
    scope = InactiveScope::class
)
internal interface InactiveGraph {
    val user: InactiveUser.Composite

    @Named("InactiveCircuit")
    val circuit: Circuit
    val navigator: Navigator
    val backStack: SaveableBackStack

    @Named("InactiveCircuit")
    @Provides
    @SingleIn(InactiveScope::class)
    fun provideCircuit(): Circuit {
        val builder = Circuit.Builder()
        return builder.build()
    }


    @ContributesTo(LoggedInScope::class)
    @GraphExtension.Factory
    interface Factory {
        fun createInactiveGraph(
            @Provides backstack: SaveableBackStack,
            @Provides navigator: Navigator,
            @Provides user: InactiveUser.Composite,
        ): InactiveGraph
    }
}
