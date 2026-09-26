package org.mobilenativefoundation.trails.app.scaffold

import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.foundation.scope.ActiveScope
import org.mobilenativefoundation.trails.screen.home.HomeScreen
import org.mobilenativefoundation.trails.screen.profile.ProfileScreen

@ContributesBinding(ActiveScope::class)
@Inject
class RealScaffoldRouter(
    private val backStack: SaveableBackStack
) : ScaffoldRouter {

    private var current: Screen = HomeScreen

    override fun attachHome() {
        if (current != HomeScreen) {
            backStack.push(HomeScreen)
        }
        current = HomeScreen
    }

    override fun attachProfile() {
        val profileScreen = ProfileScreen() // Use constructor with default userId
        if (current !is ProfileScreen) {
            backStack.push(profileScreen)
        }
        current = profileScreen
    }
}