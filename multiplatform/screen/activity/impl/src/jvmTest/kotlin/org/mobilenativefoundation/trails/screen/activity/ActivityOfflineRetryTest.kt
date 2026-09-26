package org.mobilenativefoundation.trails.screen.activity

import org.mobilenativefoundation.trails.data.trail.storage.PlatformTrailDatabaseDriverFactory

import org.mobilenativefoundation.trails.app.navigation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.nio.file.Files
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.*
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mobilenativefoundation.trails.feature.savetrail.SaveTrailFeature
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.app.navigation.AppNavigation
import org.mobilenativefoundation.trails.data.backend.BackendConfig
import org.mobilenativefoundation.trails.data.backend.NetworkMode
import org.mobilenativefoundation.trails.data.trail.account.RealTrailDataFactory
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.data.trail.catalog.TrailRepository

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class ActivityOfflineRetryTest {
    @BeforeTest fun mainDispatcher() { Dispatchers.setMain(Dispatchers.Unconfined) }
    @AfterTest fun resetDispatcher() { Dispatchers.resetMain() }

    @Test fun realCachedOfflineRetryRetainsHistoryWithoutFailure() = verifyRetry(offline = true)
    @Test fun realOnlineCachedFailureStillOffersRetry() = verifyRetry(offline = false)

    @Test fun realUncachedOfflineRetryKeepsRecoveryAndLoadsAfterReconnect() {
        val directory = Files.createTempDirectory("activity-cold-retry").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val factory = RealTrailDataFactory(PlatformTrailDatabaseDriverFactory(directory), scope)
        val online = BackendConfig(latencyRange = 0.milliseconds..0.milliseconds)
        try {
            val account = runBlocking {
                factory.applyBackendConfig(online.copy(networkMode = NetworkMode.OFFLINE))
                factory.open("bob")
            }
            runDesktopComposeUiTest {
                val presenter = ActivityPresenter(account.activities, factory.trails, account.saved, Navigation, Saves)
                val owner = object : LifecycleOwner { override val lifecycle = LifecycleRegistry.createUnsafe(this) }
                var latest: ActivityState? = null
                setContent {
                    CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                        val state = presenter.present()
                        SideEffect { latest = state }
                        TrailsTheme { ActivityUi().Content(state, Modifier) }
                    }
                }
                waitUntil(timeoutMillis = 10_000) { latest?.let { !it.history.loading && it.history.error != null } == true }
                onNodeWithText("Offline · Activity isn’t on this device yet").assertIsDisplayed()
                onNodeWithText("Try again").performClick()
                waitUntil(timeoutMillis = 10_000) { latest?.let { !it.history.loading && it.history.error != null } == true }
                runOnIdle { assertNull(requireNotNull(latest).history.data) }
                onNodeWithText("Offline · Activity isn’t on this device yet").assertIsDisplayed()
                runBlocking { factory.applyBackendConfig(online) }
                waitUntil(timeoutMillis = 5_000) { latest?.history?.offline == false }
                onNodeWithText("Try again").performClick()
                waitUntil(timeoutMillis = 10_000) { latest?.let { it.history.data?.size == 6 && it.trails.isNotEmpty() && !it.history.loading && it.history.error == null } == true }
            }
        } finally {
            runBlocking { factory.close() }
            scope.cancel()
            directory.deleteRecursively()
        }
    }

    @Test fun realUncachedPresenterRetriesTheAccountAndRecoversAfterReconnect() {
        val directory = Files.createTempDirectory("activity-cold-retry").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val factory = RealTrailDataFactory(PlatformTrailDatabaseDriverFactory(directory), scope)
        val online = BackendConfig(latencyRange = 0.milliseconds..0.milliseconds)
        try {
            val account = runBlocking {
                factory.applyBackendConfig(online.copy(networkMode = NetworkMode.OFFLINE))
                factory.open("bob")
            }
            runDesktopComposeUiTest {
                var refreshesCompleted = 0
                val catalog = object : TrailRepository by factory.trails {
                    override suspend fun refreshQuery(query: TrailQuery) {
                        factory.trails.refreshQuery(query)
                        refreshesCompleted++
                    }
                }
                val presenter = ActivityPresenter(account.activities, catalog, account.saved, Navigation, Saves)
                val owner = object : LifecycleOwner { override val lifecycle = LifecycleRegistry.createUnsafe(this) }
                var latest: ActivityState? = null
                setContent {
                    CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                        val state = presenter.present()
                        SideEffect { latest = state }
                    }
                }
                waitUntil(timeoutMillis = 10_000) { latest?.let { !it.history.loading && it.history.error != null } == true }
                runOnIdle { assertTrue(requireNotNull(latest).history.offline); assertNull(requireNotNull(latest).history.data) }
                runOnIdle { requireNotNull(latest).send(ActivityIntent.Retry) }
                waitUntil(timeoutMillis = 10_000) { refreshesCompleted == 1 && latest?.let { !it.history.loading && it.history.error != null } == true }
                runOnIdle { assertNull(requireNotNull(latest).history.data) }
                runOnIdle { assertTrue(requireNotNull(latest).history.offline); assertNull(requireNotNull(latest).history.data) }
                runBlocking { factory.applyBackendConfig(online) }
                waitUntil(timeoutMillis = 5_000) { latest?.history?.offline == false }
                runOnIdle { requireNotNull(latest).send(ActivityIntent.Retry) }
                waitUntil(timeoutMillis = 10_000) { refreshesCompleted == 2 && latest?.let { it.history.data?.size == 6 && it.trails.isNotEmpty() && !it.history.loading && it.history.error == null } == true }
            }
        } finally {
            runBlocking { factory.close() }
            scope.cancel()
            directory.deleteRecursively()
        }
    }

    private fun verifyRetry(offline: Boolean) {
        val directory = Files.createTempDirectory("activity-retry").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val factory = RealTrailDataFactory(PlatformTrailDatabaseDriverFactory(directory), scope)
        val online = BackendConfig(latencyRange = 0.milliseconds..0.milliseconds)
        try {
            val account = runBlocking { factory.applyBackendConfig(online); factory.open("alice") }
            runDesktopComposeUiTest {
                val presenter = ActivityPresenter(account.activities, factory.trails, account.saved, Navigation, Saves)
                val owner = object : LifecycleOwner { override val lifecycle = LifecycleRegistry.createUnsafe(this) }
                var latest: ActivityState? = null
                var observedRefresh = false
                setContent {
                    CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                        val state = presenter.present()
                        SideEffect { latest = state; if (state.history.loading) observedRefresh = true }
                        TrailsTheme { ActivityUi().Content(state, Modifier) }
                    }
                }
                waitUntil(timeoutMillis = 10_000) { latest?.let { it.history.data?.size == 6 && it.trails.isNotEmpty() && !it.history.loading } == true }
                val before = runOnIdle { requireNotNull(latest).history.data }
                runBlocking { factory.applyBackendConfig(if (offline) online.copy(networkMode = NetworkMode.OFFLINE) else online.copy(errorRate = 1f)) }
                if (offline) waitUntil(timeoutMillis = 5_000) { latest?.history?.offline == true }
                runOnIdle { observedRefresh = false; requireNotNull(latest).send(ActivityIntent.Retry) }
                waitUntil(timeoutMillis = 10_000) { observedRefresh && latest?.history?.loading == false }
                runOnIdle {
                    assertEquals(before, requireNotNull(latest).history.data)
                    if (offline) assertNull(requireNotNull(latest).history.error)
                    else assertNotNull(requireNotNull(latest).history.error)
                }
                if (offline) onNodeWithText("Couldn’t refresh · Showing this device’s copy").assertDoesNotExist()
                else {
                    onNodeWithText("Couldn’t refresh · Showing this device’s copy").performScrollTo().assertIsDisplayed()
                    runBlocking { factory.applyBackendConfig(online.copy(networkMode = NetworkMode.OFFLINE)) }
                    waitUntil(timeoutMillis = 5_000) { latest?.history?.offline == true }
                    runOnIdle { assertNotNull(requireNotNull(latest).history.error, "Changing mode must not erase an earlier online failure") }
                }
            }
        } finally {
            runBlocking { factory.close() }
            scope.cancel()
            directory.deleteRecursively()
        }
    }

    private object Navigation : AppNavigation {
        override fun openTrail(trailId: String) = Unit
        override fun selectExplore() = Unit
        override fun selectSaved(collectionId: String?) = Unit
        override fun back() = Unit
    }
    private object Saves : SaveTrailFeature {
        override fun open(trail: Trail, onDismiss: () -> Unit) = Unit
        @Composable override fun Content(onViewSaved: (String?) -> Unit) = Unit
        @Composable override fun Toast(modifier: Modifier) = Unit
    }
}
