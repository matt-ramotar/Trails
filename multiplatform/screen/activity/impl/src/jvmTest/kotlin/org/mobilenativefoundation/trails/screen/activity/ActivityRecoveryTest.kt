package org.mobilenativefoundation.trails.screen.activity

import org.mobilenativefoundation.trails.app.navigation.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.feature.savetrail.SaveTrailFeature
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.app.navigation.AppNavigation
import org.mobilenativefoundation.trails.app.navigation.ScrollPosition
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.activity.ActivityRepository
import org.mobilenativefoundation.trails.data.trail.activity.CompletedActivity
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.catalog.TrailFeature
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.data.trail.catalog.TrailRepository
import org.mobilenativefoundation.trails.data.trail.saved.SaveOutcome
import org.mobilenativefoundation.trails.data.trail.saved.SavedRepository
import org.mobilenativefoundation.trails.data.trail.saved.SavedSnapshot
import org.mobilenativefoundation.trails.data.trail.saved.SetCollectionsCommand

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class ActivityRecoveryTest {
    @BeforeTest fun setMainDispatcher() { Dispatchers.setMain(Dispatchers.Unconfined) }
    @AfterTest fun resetMainDispatcher() { Dispatchers.resetMain() }
    private val trail = Trail("trolltunga", "Trolltunga", "Norway", "Ledge.", TrailDifficulty.HARD, 27000, 800, 600, 4.9, 1964, setOf(TrailFeature.LAKE), 2)
    private val history = listOf(CompletedActivity("sample", trail.id, trail.name, 1_789_905_600_000L, 27000, 600, 800))

    @Test
    fun historyBeforeFailedCatalogRetainsHistoryAndScrollAndRecoversHeart() = verifyRecovery(throws = false)

    @Test
    fun terminatedCatalogObserverReconnectsAndRecoversHeart() = verifyRecovery(throws = true)

    @Test
    fun terminatedCatalogRetainsItsLastTrailData() = verifyRecovery(throws = true, cached = true)

    @Test
    fun offlineProjectionOnlySuppressesRecognizedFailuresWithUsableSourceCaches() = runDesktopComposeUiTest {
        val feed = TestFeed(LoadState(history, loading = false))
        val catalog = TestCatalog(false)
        val presenter = ActivityPresenter(feed, catalog, TestSaved(), TestNavigation(), TestSaves())
        var latest: ActivityState? = null
        val owner = object : LifecycleOwner { override val lifecycle = LifecycleRegistry.createUnsafe(this) }
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                val state = presenter.present()
                SideEffect { latest = state }
            }
        }
        val offlineError = "Fetch failed for key 'activities/alice': The fake backend Offline setting is applied. The fetcher threw; inspect the cause for the underlying failure."
        val cases = listOf(
            // A history cache cannot stand in for missing/partial catalog data.
            Triple(LoadState(history, false, offlineError, true), LoadState<List<Trail>>(null, false, offlineError, true), offlineError),
            Triple(LoadState(history, false, offlineError, true), LoadState(emptyList(), false, offlineError, true), offlineError),
            // Earlier online errors and unrelated failures remain visible after switching Offline.
            Triple(LoadState(history, false, "Earlier online failure", true), LoadState(listOf(trail), false), "Earlier online failure"),
            Triple(LoadState(history, false), LoadState(listOf(trail), false, "Catalog unavailable", true), "Catalog unavailable"),
            Triple(LoadState(history, false), LoadState(listOf(trail), false, "Persistence failed: $offlineError", true), "Persistence failed: $offlineError"),
            Triple(LoadState(history, false, offlineError.replace("Fetch failed", "Fetch unavailable"), true), LoadState(listOf(trail), false), offlineError.replace("Fetch failed", "Fetch unavailable")),
            Triple(LoadState(history, false, offlineError, false), LoadState(listOf(trail), false), offlineError),
            Triple(LoadState(history, false, offlineError, true), LoadState(listOf(trail), false, offlineError, true), null),
        )
        for ((historyState, catalogState, expectedError) in cases) {
            runOnIdle { feed.states.value = historyState; catalog.states.value = catalogState }
            waitUntil(timeoutMillis = 5_000) { latest?.let { !it.history.loading && it.history.error == expectedError } == true }
            runOnIdle { assertEquals(history, requireNotNull(latest).history.data) }
        }
    }

    @Test
    fun cancelledActivityRetryDoesNotRefreshCatalogOrBecomeFailure() = runDesktopComposeUiTest {
        val feed = TestFeed(LoadState(history, loading = false)).apply { refreshFailure = CancellationException("screen closed") }
        val catalog = TestCatalog(false).apply { states.value = LoadState(listOf(trail), loading = false) }
        val presenter = ActivityPresenter(feed, catalog, TestSaved(), TestNavigation(), TestSaves())
        var latest: ActivityState? = null
        val owner = object : LifecycleOwner { override val lifecycle = LifecycleRegistry.createUnsafe(this) }
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                val state = presenter.present()
                SideEffect { latest = state }
            }
        }
        waitUntil(timeoutMillis = 5_000) { latest?.history?.loading == false }
        runOnIdle { requireNotNull(latest).send(ActivityIntent.Retry) }
        waitUntil(timeoutMillis = 5_000) { feed.refreshes == 1 && latest?.history?.loading == false }
        runOnIdle {
            assertTrue(catalog.refreshes.isEmpty())
            assertEquals(null, requireNotNull(latest).history.error)
            assertEquals(history, requireNotNull(latest).history.data)
        }
    }

    private fun verifyRecovery(throws: Boolean, cached: Boolean = false) = runDesktopComposeUiTest {
        val repository = TestFeed(LoadState(history, loading = false))
        val catalog = TestCatalog(throws)
        val checkpoint = ScrollPosition(0, 17)
        val navigation = TestNavigation(checkpoint)
        val saves = TestSaves()
        val presenter = ActivityPresenter(repository, catalog, TestSaved(), navigation, saves)
        var latest: ActivityState? = null
        val owner = object : LifecycleOwner { override val lifecycle = LifecycleRegistry.createUnsafe(this) }
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                val state = presenter.present()
                SideEffect { latest = state }
                TrailsTheme { ActivityUi().Content(state, Modifier) }
            }
        }
        waitUntil(timeoutMillis = 5_000) { latest?.history?.data != null }
        runOnIdle {
            assertTrue(requireNotNull(latest).history.loading)
            catalog.states.value = LoadState(data = if (cached) listOf(trail) else null, loading = false, error = "Catalog unavailable", offline = true)
        }
        waitUntil(timeoutMillis = 5_000) { latest?.history?.loading == false }
        runOnIdle {
            assertEquals("Catalog unavailable", requireNotNull(latest).history.error)
            assertEquals(history, requireNotNull(latest).history.data)
            assertTrue(requireNotNull(latest).history.offline)
            assertEquals(checkpoint, requireNotNull(latest).initialScroll)
            if (cached) assertEquals(trail, requireNotNull(latest).trails[trail.id])
        }
        onNodeWithText("Couldn’t refresh · Showing this device’s copy").performScrollTo().assertIsDisplayed()
        onNodeWithText("Try again").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { latest?.let { it.trails[trail.id] == trail && !it.history.loading } == true }
        runOnIdle {
            assertEquals(1, repository.refreshes)
            assertEquals(listOf(TrailQuery()), catalog.refreshes)
            assertEquals(history, requireNotNull(latest).history.data)
            assertEquals(null, requireNotNull(latest).history.error)
            assertEquals(false, requireNotNull(latest).history.offline)
            assertEquals(checkpoint, requireNotNull(latest).initialScroll)
            assertTrue(navigation.checkpoints.all { it.index == 0 }, "Recovery must not reset the retained list position")
            if (throws) assertTrue(catalog.observations >= 2)
        }
        onNodeWithText("Save Trolltunga").performScrollTo().performClick()
        runOnIdle {
            assertEquals(listOf(trail), saves.opened)
            assertTrue(navigation.opened.isEmpty())
        }
    }

    private class TestFeed(initial: LoadState<List<CompletedActivity>>) : ActivityRepository {
        val states = MutableStateFlow(initial)
        var refreshes = 0
        var refreshFailure: Exception? = null
        override fun observe() = states
        override suspend fun refresh() { refreshes++; refreshFailure?.let { throw it }; states.value = states.value.copy(loading = false, error = null, offline = false) }
    }

    private inner class TestCatalog(private val throws: Boolean) : TrailRepository {
        val states = MutableStateFlow(LoadState<List<Trail>>())
        val refreshes = mutableListOf<TrailQuery>()
        var observations = 0
        override fun observeQuery(query: TrailQuery) = flow {
            observations++
            states.collect {
                emit(it)
                if (throws && it.error != null) error("Catalog unavailable")
            }
        }
        override suspend fun refreshQuery(query: TrailQuery) { refreshes += query; states.value = LoadState(listOf(trail), loading = false) }
        override fun observeTrail(id: String) = error("Not used")
        override suspend fun refreshTrail(id: String) = error("Not used")
        override suspend fun count(query: TrailQuery): LoadState<Int> = error("Not used")
    }

    private class TestSaved : SavedRepository {
        override val state = MutableStateFlow(LoadState<SavedSnapshot>(loading = false))
        override suspend fun save(command: SetCollectionsCommand): SaveOutcome = error("Not used")
        override suspend fun reconcile(command: SetCollectionsCommand): SaveOutcome = error("Not used")
        override suspend fun refresh() = Unit
        override suspend fun retryPending() = Unit
    }

    private class TestNavigation(private val initial: ScrollPosition = ScrollPosition()) : AppNavigation {
        val checkpoints = mutableListOf<ScrollPosition>()
        val opened = mutableListOf<String>()
        override fun scrollPosition(key: String): ScrollPosition { assertEquals("ACTIVITY/activity", key); return initial }
        override fun checkpointScroll(key: String, position: ScrollPosition) { assertEquals("ACTIVITY/activity", key); checkpoints += position }
        override fun openTrail(trailId: String) { opened += trailId }
        override fun selectExplore() = Unit
        override fun selectSaved(collectionId: String?) = Unit
        override fun back() = Unit
    }

    private class TestSaves : SaveTrailFeature {
        val opened = mutableListOf<Trail>()
        override fun open(trail: Trail, onDismiss: () -> Unit) { opened += trail }
        @Composable override fun Content(onViewSaved: (String?) -> Unit) = Unit
        @Composable override fun Toast(modifier: Modifier) = Unit
    }
}
