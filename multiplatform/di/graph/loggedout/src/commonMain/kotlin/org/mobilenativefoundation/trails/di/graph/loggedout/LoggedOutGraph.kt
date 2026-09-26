package org.mobilenativefoundation.trails.di.graph.loggedout

import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.*
import org.mobilenativefoundation.trails.foundation.scope.LoggedOutScope
import org.mobilenativefoundation.trails.screen.welcome.WelcomePresenter
import org.mobilenativefoundation.trails.screen.welcome.WelcomeScreen
import org.mobilenativefoundation.trails.screen.welcome.WelcomeState
import org.mobilenativefoundation.trails.screen.welcome.WelcomeUi

@GraphExtension(
    scope = LoggedOutScope::class
)
interface LoggedOutGraph {

    @Named("LoggedOutCircuit")
    val circuit: Circuit
    val navigator: Navigator
    val backStack: SaveableBackStack

    @Named("LoggedOutCircuit")
    @Provides
    @SingleIn(LoggedOutScope::class)
    fun provideCircuit(
        welcomeUi: WelcomeUi,
        welcomePresenter: WelcomePresenter
    ): Circuit {
        val builder = Circuit.Builder()
        builder.addUi<WelcomeScreen, WelcomeState> { state, modifier -> welcomeUi.Content(state, modifier) }
        builder.addPresenter<WelcomeScreen, WelcomeState>(welcomePresenter)
        return builder.build()
    }

    @ContributesTo(AppScope::class)
    @GraphExtension.Factory
    interface Factory {
        fun createLoggedOutGraph(
            @Provides backstack: SaveableBackStack,
            @Provides navigator: Navigator,
        ): LoggedOutGraph
    }
}