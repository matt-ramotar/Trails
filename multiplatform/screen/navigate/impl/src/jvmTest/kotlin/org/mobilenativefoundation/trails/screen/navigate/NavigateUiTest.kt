package org.mobilenativefoundation.trails.screen.navigate

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
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

@OptIn(ExperimentalTestApi::class)
class NavigateUiTest {
    private val trail = Trail("half-dome", "Half Dome", "Yosemite National Park, USA", "Granite summit.", TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486, setOf(TrailFeature.WATERFALL, TrailFeature.SUMMIT), 2)

    @Test
    fun sheetActionsSendTheirIntentsAndTheToastRenders() = runDesktopComposeUiTest {
        val sent = mutableListOf<NavigateIntent>()
        setContent { TrailsTheme { NavigateUi().Content(NavigateState(LoadState(trail, loading = false), toast = "Recording is coming soon") { sent += it }, Modifier) } }
        onNodeWithText("Ready when you are").assertIsDisplayed()
        onNodeWithText("Route preview · schematic").assertIsDisplayed()
        onNodeWithText("Recording is coming soon").assertIsDisplayed()
        onNodeWithText("Start recording").performClick()
        onNodeWithText("Half Dome").performClick()
        assertEquals(listOf(NavigateIntent.StartRecording, NavigateIntent.OpenTrail), sent)
    }
    @Test
    fun loadingDoesNotOfferAStaleTrailOrRetry() = unavailable(LoadState(), "Finding your trail…", retry = false)

    @Test
    fun uncachedOfflineOffersOneRetryWithoutAStaleTrail() = unavailable(LoadState(loading = false, offline = true), "Not on this device yet")

    @Test
    fun failedLoadOffersOneRetryWithoutAStaleTrail() = unavailable(LoadState(loading = false, error = "Failed"), "Couldn’t load this trail")

    private fun unavailable(load: LoadState<Trail>, message: String, retry: Boolean = true) = runDesktopComposeUiTest {
        val sent = mutableListOf<NavigateIntent>()
        setContent { TrailsTheme { NavigateUi().Content(NavigateState(load) { sent += it }, Modifier) } }
        onNodeWithText(message).assertIsDisplayed()
        onNodeWithText(trail.name).assertDoesNotExist()
        if (retry) {
            onNodeWithText("Try again").performClick()
            assertEquals(listOf<NavigateIntent>(NavigateIntent.Retry), sent)
        } else {
            onNodeWithText("Try again").assertDoesNotExist()
            assertEquals(emptyList(), sent)
        }
    }
}
