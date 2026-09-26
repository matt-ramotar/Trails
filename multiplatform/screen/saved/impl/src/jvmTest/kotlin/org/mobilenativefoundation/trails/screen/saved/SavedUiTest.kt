package org.mobilenativefoundation.trails.screen.saved

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.catalog.TrailFeature
import org.mobilenativefoundation.trails.data.trail.saved.SavedSnapshot
import org.mobilenativefoundation.trails.data.trail.saved.TrailCollection

@OptIn(ExperimentalTestApi::class)
class SavedUiTest {
    private val trail = Trail("half-dome", "Half Dome", "Yosemite National Park, USA", "Granite summit.", TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486, setOf(TrailFeature.WATERFALL, TrailFeature.SUMMIT), 2)

    @Test
    fun tilesShowCountsPlaceholdersAndTheAllTrailsTab() = runDesktopComposeUiTest {
        val sent = mutableListOf<SavedIntent>()
        val snapshot = SavedSnapshot(
            collections = listOf(TrailCollection("weekend", "Weekend adventures"), TrailCollection("favorites", "Favorites")),
            memberships = mapOf(trail.id to setOf("weekend")), trails = listOf(trail), syncByTrail = emptyMap(),
        )
        val state = SavedState(LoadState(snapshot, loading = false), allTrails = false) { sent += it }
        setContent { TrailsTheme { SavedUi().Content(state, Modifier) } }
        onNodeWithText("1 trail").assertIsDisplayed()
        onNodeWithText("0 saved").assertIsDisplayed()
        onNodeWithContentDescription("Empty list").assertIsDisplayed()
        onNodeWithText("All trails").performClick()
        assertEquals(SavedIntent.SelectSegment(true), sent.filterIsInstance<SavedIntent.SelectSegment>().single())
    }

    @Test
    fun allTrailsWithoutCachedDetailsShowsTheMissingDetailsLineNotTheEmptyState() = runDesktopComposeUiTest {
        val sent = mutableListOf<SavedIntent>()
        val snapshot = SavedSnapshot(
            collections = listOf(TrailCollection("weekend", "Weekend adventures")),
            memberships = mapOf("half-dome" to setOf("weekend")), trails = emptyList(), syncByTrail = emptyMap(),
        )
        val state = SavedState(LoadState(snapshot, loading = false), allTrails = true) { sent += it }
        setContent { TrailsTheme { SavedUi().Content(state, Modifier) } }
        onNodeWithText("Some trail details aren’t on this device yet").assertIsDisplayed()
        onNodeWithText("Keep your next escape").assertDoesNotExist()
    }
}
