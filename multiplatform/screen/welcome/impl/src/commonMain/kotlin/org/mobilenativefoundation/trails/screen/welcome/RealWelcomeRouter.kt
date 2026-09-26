package org.mobilenativefoundation.trails.screen.welcome

import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.foundation.scope.LoggedOutScope

@ContributesBinding(LoggedOutScope::class)
@Inject
internal class RealWelcomeRouter(
    private val navigator: Navigator
) : WelcomeRouter {
    override fun routeToSignup() {
        // TODO
    }

    override fun routeToLogin() {
        // TODO
    }

    override fun routeToGoogleOAuth() {
        // TODO
    }

}