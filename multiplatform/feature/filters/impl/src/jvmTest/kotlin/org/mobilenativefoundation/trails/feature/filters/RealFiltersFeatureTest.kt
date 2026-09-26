package org.mobilenativefoundation.trails.feature.filters

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.data.trail.catalog.TrailRepository

@OptIn(ExperimentalTestApi::class)
class RealFiltersFeatureTest {
    private class CountingRepository : TrailRepository {
        override fun observeQuery(query: TrailQuery): Flow<LoadState<List<Trail>>> = flowOf(LoadState(emptyList(), loading = false))
        override fun observeTrail(id: String): Flow<LoadState<Trail>> = flowOf(LoadState(loading = false))
        override suspend fun refreshQuery(query: TrailQuery) {}
        override suspend fun refreshTrail(id: String) {}
        override suspend fun count(query: TrailQuery): LoadState<Int> = LoadState(3, loading = false)
    }

    @Test
    fun dogFriendlySwitchAndStrenuousChipReachTheAppliedQuery() = runDesktopComposeUiTest {
        val feature = RealFiltersFeature(CountingRepository())
        val applied = CoroutineScope(Dispatchers.Unconfined).async { feature.show(TrailQuery()) }
        setContent { TrailsTheme { feature.Content() } }
        waitForIdle()
        onNodeWithText("Dog-friendly").performClick()
        onNodeWithText("Strenuous").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Show 3 trails").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Show 3 trails").performScrollTo().performClick()
        val result = runBlocking { withTimeout(5_000) { applied.await() } }!!
        assertTrue(result.dogFriendly)
        assertEquals(setOf(TrailDifficulty.STRENUOUS), result.difficulties)
    }
}
