package org.mobilenativefoundation.trails.app.runtime.bootstrap

import org.mobilenativefoundation.trails.app.navigation.*

import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.data.trail.catalog.*
import org.mobilenativefoundation.trails.data.trail.account.*
import org.mobilenativefoundation.trails.data.trail.saved.*
import org.mobilenativefoundation.trails.data.trail.activity.*
import org.mobilenativefoundation.trails.data.trail.recommendation.*
import org.mobilenativefoundation.trails.data.session.SampleAccounts
import org.mobilenativefoundation.trails.data.session.UserRepository
import org.mobilenativefoundation.trails.app.runtime.graph.active.ActiveGraph
import org.mobilenativefoundation.trails.app.navigation.AppNavigationController
import org.mobilenativefoundation.trails.app.runtime.graph.inactive.InactiveGraph
import org.mobilenativefoundation.trails.app.runtime.graph.loggedin.LoggedInGraph
import org.mobilenativefoundation.trails.app.runtime.graph.loggedout.LoggedOutGraph
import org.mobilenativefoundation.trails.feature.filters.FiltersFeature
import org.mobilenativefoundation.trails.feature.savetrail.SaveTrailFeature
import org.mobilenativefoundation.trails.data.session.model.*
import org.mobilenativefoundation.trails.data.backend.BackendConfig

@OptIn(ExperimentalCoroutinesApi::class)
class BootstrapRecoveryTest {
    @Test
    fun sourceEchoDoesNotRetryFailedAccountUntilExplicitRetry() = runTest {
        val account = TestAccount(SampleAccounts.primary.node.id)
        var attempts = 0
        val data = TestData {
            attempts++
            if (attempts == 1) error("Injected account open failure")
            account
        }
        val users = EchoingUserRepository()
        val coordinator = coordinator(backgroundScope, users, data)
        coordinator.start()
        runCurrent()
        assertIs<AppRoot.Failed>(coordinator.route.value)
        assertEquals(1, attempts)

        users.persist(SampleAccounts.primary)
        runCurrent()
        assertIs<AppRoot.Failed>(coordinator.route.value)
        assertEquals(1, attempts, "An equal source-of-truth echo must not retry the account lifecycle")

        coordinator.retry()
        runCurrent()
        assertSame(account, assertIs<AppRoot.Main>(coordinator.route.value).graph.account)
        assertEquals(2, attempts, "Explicit retry must replay the unchanged session")
    }

    @Test
    fun failedOpenRetriesRestoredAccountWithoutParallelAttempts() = runTest {
        val release = CompletableDeferred<Unit>()
        val account = TestAccount(SampleAccounts.primary.node.id)
        var attempts = 0
        val data = TestData {
            attempts++
            if (attempts == 1) error("Injected account open failure")
            release.await()
            account
        }
        val coordinator = coordinator(backgroundScope, TestUserRepository(), data)
        coordinator.start()
        runCurrent()
        assertIs<AppRoot.Failed>(coordinator.route.value)
        assertEquals(1, attempts)

        coordinator.retry()
        runCurrent()
        assertSame(AppRoot.Splash, coordinator.route.value)
        assertEquals(2, attempts)
        repeat(3) { coordinator.retry() }
        runCurrent()
        assertEquals(2, attempts, "Retries during an active open must not start another lifecycle")

        release.complete(Unit)
        runCurrent()
        assertSame(account, assertIs<AppRoot.Main>(coordinator.route.value).graph.account)
        assertEquals(listOf(account.accountId, account.accountId), data.opened)
    }

    @Test
    fun switchHidesPrivateUiAndWaitsForRetirementBeforeOpeningNextAccount() = runTest {
        val release = CompletableDeferred<Unit>()
        val retirementStarted = CompletableDeferred<Unit>()
        val first = TestAccount(SampleAccounts.primary.node.id) {
            retirementStarted.complete(Unit)
            release.await()
        }
        val second = TestAccount(SampleAccounts.secondary.node.id)
        val users = TestUserRepository()
        val data = TestData { if (it == first.accountId) first else second }
        val coordinator = coordinator(backgroundScope, users, data)
        coordinator.start()
        runCurrent()
        assertSame(first, assertIs<AppRoot.Main>(coordinator.route.value).graph.account)

        users.persist(SampleAccounts.secondary)
        runCurrent()
        assertTrue(retirementStarted.isCompleted)
        assertSame(AppRoot.Splash, coordinator.route.value)
        assertEquals(listOf(first.accountId), data.opened)
        coordinator.retry()
        runCurrent()
        assertEquals(listOf(first.accountId), data.opened)

        release.complete(Unit)
        runCurrent()
        assertSame(second, assertIs<AppRoot.Main>(coordinator.route.value).graph.account)
        assertEquals(listOf(first.accountId, second.accountId), data.opened)
    }
}

private class TestAccount(override val accountId: String, private val retire: suspend () -> Unit = {}) : TrailAccount {
    override val saved: SavedRepository get() = error("Not needed by bootstrap")
    override val activities: ActivityRepository get() = error("Not needed by bootstrap")
    override val forYou: ForYouRepository get() = error("Not needed by bootstrap")
    override suspend fun close() = retire()
}

private class TestData(private val opener: suspend (String) -> TrailAccount) : TrailDataFactory {
    val opened = mutableListOf<String>()
    override val trails: TrailRepository get() = error("Not needed by bootstrap")
    override val backendConfig = MutableStateFlow<BackendConfig?>(BackendConfig())
    override suspend fun restoreBackendConfig() = requireNotNull(backendConfig.value)
    override suspend fun applyBackendConfig(config: BackendConfig) { backendConfig.value = config }
    override suspend fun open(accountId: String): TrailAccount { opened += accountId; return opener(accountId) }
    override suspend fun close() = Unit
}

private class TestUserRepository : UserRepository {
    private val user = MutableStateFlow<User>(SampleAccounts.primary)
    override val current: User get() = user.value
    override fun stream() = user
    override suspend fun persist(user: User) { this.user.value = user }
}

private class EchoingUserRepository : UserRepository {
    private val users = MutableSharedFlow<User>(replay = 1).apply { tryEmit(SampleAccounts.primary) }
    override val current: User get() = users.replayCache.last()
    override fun stream() = users
    override suspend fun persist(user: User) { users.emit(user) }
}

private fun coordinator(scope: CoroutineScope, users: UserRepository, data: TrailDataFactory) = RealBootstrapCoordinator(
    appScope = scope,
    loggedOutGraphFactory = object : LoggedOutGraph.Factory {
        override fun createLoggedOutGraph(backstack: SaveableBackStack, navigator: Navigator): LoggedOutGraph =
            error("Not needed by these active-account tests")
    },
    loggedInGraphFactory = object : LoggedInGraph.Factory {
        override fun createLoggedInGraph(userSession: UserSession, backstack: SaveableBackStack, navigator: Navigator): LoggedInGraph = object : LoggedInGraph {
            override val userSession = userSession
            override val inactive: InactiveGraph.Factory get() = error("Not needed by these active-account tests")
            override val active = object : ActiveGraph.Factory {
                override fun createActiveGraph(backstack: SaveableBackStack, navigator: Navigator, user: ActiveUser.Composite, account: TrailAccount, navigation: AppNavigationController): ActiveGraph = object : ActiveGraph {
                    override val circuit = Circuit.Builder().build()
                    override val user = user
                    override val account = account
                    override val navigation = navigation
                    override val filters: FiltersFeature get() = error("Not needed by bootstrap")
                    override val saves: SaveTrailFeature get() = error("Not needed by bootstrap")
                }
            }
        }
    },
    userRepository = users,
    trailData = data,
    navigationStorageFactory = org.mobilenativefoundation.trails.app.navigation.AppNavigationStorageFactory {
        org.mobilenativefoundation.trails.app.navigation.InMemoryAppNavigationStorage()
    },
    splashStateManager = object : SplashStateWriter { override fun write(value: SplashState) = Unit },
)
