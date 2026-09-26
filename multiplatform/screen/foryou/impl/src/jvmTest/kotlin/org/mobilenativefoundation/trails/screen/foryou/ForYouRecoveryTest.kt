package org.mobilenativefoundation.trails.screen.foryou

import org.mobilenativefoundation.trails.app.navigation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.feature.savetrail.SaveTrailFeature
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.app.navigation.AppNavigation
import org.mobilenativefoundation.trails.app.navigation.ScrollPosition
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.catalog.TrailFeature
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.data.trail.catalog.TrailRepository
import org.mobilenativefoundation.trails.data.trail.recommendation.ForYouFeed
import org.mobilenativefoundation.trails.data.trail.recommendation.ForYouRepository
import org.mobilenativefoundation.trails.data.trail.saved.SaveOutcome
import org.mobilenativefoundation.trails.data.trail.saved.SavedRepository
import org.mobilenativefoundation.trails.data.trail.saved.SavedSnapshot
import org.mobilenativefoundation.trails.data.trail.saved.SetCollectionsCommand

@OptIn(ExperimentalTestApi::class)
class ForYouRecoveryTest {
    private val trail = Trail("trolltunga", "Trolltunga", "Norway", "Ledge.", TrailDifficulty.HARD, 27000, 800, 600, 4.9, 1964, setOf(TrailFeature.LAKE), 2)
    private val feed = ForYouFeed(trail.id, "A weekend worth the walk", "Discover a quieter side of outside.", trail.id, listOf(trail.id))

    @Test
    fun feedBeforeCatalogPreservesCheckpointUntilRecommendationRowsArrive() = runDesktopComposeUiTest {
        val rows = (0..23).map { trail.copy(id = "row-$it", name = "Recommendation $it") }
        val repository = TestFeed(LoadState(feed.copy(recommendedTrailIds = rows.map { it.id }), loading = false))
        val catalog = TestCatalog(LoadState(), rows + trail)
        val checkpoint = ScrollPosition(12, 17)
        val navigation = TestNavigation(checkpoint)
        val presenter = ForYouPresenter(repository, catalog, TestSaved(), navigation, TestSaves())
        var latest: ForYouState? = null
        setContent {
            val state = presenter.present()
            SideEffect { latest = state }
            TrailsTheme { ForYouUi().Content(state, Modifier) }
        }
        waitUntil(timeoutMillis = 5_000) { latest?.feed?.data != null }
        waitForIdle()
        runOnIdle {
            assertTrue(navigation.checkpoints.isEmpty(), "No clamped checkpoint may be published while the catalog is pending: ${navigation.checkpoints}")
            assertTrue(latest!!.feed.loading)
            catalog.states.value = LoadState(rows + trail, loading = false)
        }
        waitUntil(timeoutMillis = 5_000) { navigation.checkpoints.contains(checkpoint) }
        runOnIdle { assertEquals(checkpoint, navigation.checkpoints.first()) }
        onNodeWithText("Recommendation 9").assertIsDisplayed()
    }

    @Test
    fun cachedFeedAndFailedCatalogExposeRetryAndRecoverBothRepositories() = runDesktopComposeUiTest {
        val repository = TestFeed(LoadState(feed, loading = false))
        val catalog = TestCatalog(LoadState(loading = false, error = "Catalog unavailable", offline = true), listOf(trail))
        val navigation = TestNavigation()
        val presenter = ForYouPresenter(repository, catalog, TestSaved(), navigation, TestSaves())
        var latest: ForYouState? = null
        setContent {
            val state = presenter.present()
            SideEffect { latest = state }
            TrailsTheme { ForYouUi().Content(state, Modifier) }
        }
        waitUntil(timeoutMillis = 5_000) { latest?.feed?.data != null }
        runOnIdle {
            val state = requireNotNull(latest)
            assertEquals("Catalog unavailable", state.feed.error)
            assertTrue(state.feed.offline)
            assertEquals(feed, state.feed.data)
        }
        onNodeWithText("Trail details aren’t on this device yet").performScrollTo().assertIsDisplayed()
        onNodeWithText("Try again").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { latest?.let { it.trails[trail.id] == trail && !it.feed.loading } == true }
        runOnIdle {
            assertEquals(1, repository.refreshes)
            assertEquals(listOf(TrailQuery()), catalog.refreshes)
            val state = requireNotNull(latest)
            assertEquals(null, state.feed.error)
            assertEquals(false, state.feed.offline)
        }
        onNodeWithText("A weekend worth the walk").performScrollTo().performClick()
        runOnIdle { assertEquals(listOf(trail.id), navigation.opened) }
    }

    private class TestFeed(initial: LoadState<ForYouFeed>) : ForYouRepository {
        val states = MutableStateFlow(initial)
        var refreshes = 0
        override fun observe() = states
        override suspend fun refresh() { refreshes++; states.value = states.value.copy(loading = false, error = null, offline = false) }
    }

    private class TestCatalog(initial: LoadState<List<Trail>>, private val recovered: List<Trail>) : TrailRepository {
        val states = MutableStateFlow(initial)
        val refreshes = mutableListOf<TrailQuery>()
        override fun observeQuery(query: TrailQuery) = states
        override suspend fun refreshQuery(query: TrailQuery) { refreshes += query; states.value = LoadState(recovered, loading = false) }
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
        override fun scrollPosition(key: String): ScrollPosition { assertEquals("FOR_YOU/foryou", key); return initial }
        override fun checkpointScroll(key: String, position: ScrollPosition) { assertEquals("FOR_YOU/foryou", key); checkpoints += position }
        override fun openTrail(trailId: String) { opened += trailId }
        override fun selectExplore() = Unit
        override fun selectSaved(collectionId: String?) = Unit
        override fun back() = Unit
    }

    private class TestSaves : SaveTrailFeature {
        override fun open(trail: Trail, onDismiss: () -> Unit) = Unit
        @Composable override fun Content(onViewSaved: (String?) -> Unit) = Unit
        @Composable override fun Toast(modifier: Modifier) = Unit
    }
}
