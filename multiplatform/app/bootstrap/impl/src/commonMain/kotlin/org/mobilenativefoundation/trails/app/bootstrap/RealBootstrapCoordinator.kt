package org.mobilenativefoundation.trails.app.bootstrap

import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.foundation.Navigator
import dev.zacsweers.metro.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mobilenativefoundation.trails.data.trail.TrailAccount
import org.mobilenativefoundation.trails.data.trail.TrailDataFactory
import org.mobilenativefoundation.trails.data.user.UserRepository
import org.mobilenativefoundation.trails.di.graph.active.ActiveGraph
import org.mobilenativefoundation.trails.di.graph.active.M1NavigationStorageFactory
import org.mobilenativefoundation.trails.di.graph.active.M1NavigationController
import org.mobilenativefoundation.trails.di.graph.inactive.InactiveGraph
import org.mobilenativefoundation.trails.di.graph.loggedin.LoggedInGraph
import org.mobilenativefoundation.trails.di.graph.loggedout.LoggedOutGraph
import org.mobilenativefoundation.trails.model.domain.user.*
import org.mobilenativefoundation.trails.screen.prelanding.PreLanding
import org.mobilenativefoundation.trails.screen.welcome.WelcomeScreen

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class RealBootstrapCoordinator(
    @param:Named("AppCoroutineScope") private val appScope: CoroutineScope,
    private val loggedOutGraphFactory: LoggedOutGraph.Factory,
    private val loggedInGraphFactory: LoggedInGraph.Factory,
    private val userRepository: UserRepository,
    private val trailData: TrailDataFactory,
    private val navigationStorageFactory: M1NavigationStorageFactory,
    private val splashStateManager: SplashStateWriter,
) : BootstrapCoordinator {
    private val mutableRoute = MutableStateFlow<AppRoot>(AppRoot.Splash)
    override val route: StateFlow<AppRoot> = mutableRoute.asStateFlow()
    private var owner: Job? = null
    private val retries = MutableStateFlow(0L)
    private var loggedOutGraph: LoggedOutGraph? = null
    private var loggedInGraph: LoggedInGraph? = null
    private var inactiveGraph: InactiveGraph? = null
    private var activeGraph: ActiveGraph? = null
    private var trailAccount: TrailAccount? = null

    override fun start() {
        if (owner != null) return
        // The native launch screen can yield to our loading/error UI and developer settings.
        splashStateManager.write(SplashState.READY)
        owner = appScope.launch {
            // No M1 account fetch or drain can run under a synthetic initial Online setting.
            trailData.backendConfig.filterNotNull().first()
            while (currentCoroutineContext().isActive) {
                try {
                    // collect (not collectLatest) completes retirement before a new account opens.
                    combine(userRepository.stream(), retries) { user, _ -> user }.collect { user ->
                        if (mutableRoute.value is AppRoot.Failed) mutableRoute.value = AppRoot.Splash
                        try {
                            when (user) {
                                LoggedOutUser -> showLoggedOut()
                                is LoggedInUser -> showLoggedIn(user)
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            mutableRoute.value = AppRoot.Failed("We couldn’t open your saved trails. Try again to restore this account.")
                        }
                    }
                    return@launch
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    val failedAttempt = retries.value
                    mutableRoute.value = AppRoot.Failed("We couldn’t read the account saved on this device. Please try again.")
                    retries.first { it != failedAttempt }
                }
            }
        }
    }

    override fun retry() {
        if (mutableRoute.value is AppRoot.Failed) retries.update { it + 1L }
    }

    private suspend fun retireAccount() {
        // Remove private UI before cancellation/join or database closure can suspend.
        mutableRoute.value = AppRoot.Splash
        activeGraph = null
        inactiveGraph = null
        val previous = trailAccount
        previous?.close()
        // Keep a failed retirement reachable so a retry cannot skip it and open another account.
        trailAccount = null
    }

    private suspend fun showLoggedOut() {
        retireAccount()
        loggedInGraph = null
        val graph = loggedOutGraph ?: run {
            val stack = SaveableBackStack(WelcomeScreen)
            loggedOutGraphFactory.createLoggedOutGraph(stack, Navigator(stack) {})
        }
        loggedOutGraph = graph
        mutableRoute.value = AppRoot.Welcome(graph)
    }

    private suspend fun showLoggedIn(user: LoggedInUser) {
        loggedOutGraph = null
        if (loggedInGraph?.userSession != user.node.properties.session ||
            (trailAccount != null && trailAccount?.accountId != user.node.id)) {
            retireAccount()
            val stack = SaveableBackStack(WelcomeScreen)
            loggedInGraph = loggedInGraphFactory.createLoggedInGraph(
                user.node.properties.session, stack, Navigator(stack) {},
            )
        }
        when (user) {
            is ActiveUser.Composite -> showActive(user)
            is InactiveUser.Composite -> showInactive(user)
        }
    }

    private suspend fun showActive(user: ActiveUser.Composite) {
        inactiveGraph = null
        val graph = activeGraph ?: run {
            val account = trailData.open(user.node.id)
            trailAccount = account
            val navigation = M1NavigationController(navigationStorageFactory.forAccount(user.node.id))
            requireNotNull(loggedInGraph).active.createActiveGraph(
                navigation.exploreStack, navigation.navigator, user, account, navigation,
            )
        }
        activeGraph = graph
        mutableRoute.value = AppRoot.Main(graph)
    }

    private suspend fun showInactive(user: InactiveUser.Composite) {
        if (trailAccount != null) retireAccount()
        val graph = inactiveGraph ?: run {
            val stack = SaveableBackStack(PreLanding)
            requireNotNull(loggedInGraph).inactive.createInactiveGraph(stack, Navigator(stack) {}, user)
        }
        inactiveGraph = graph
        mutableRoute.value = AppRoot.PreLanding(graph)
    }
}
