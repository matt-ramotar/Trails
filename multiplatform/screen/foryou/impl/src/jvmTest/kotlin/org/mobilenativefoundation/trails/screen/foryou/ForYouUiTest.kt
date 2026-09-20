package org.mobilenativefoundation.trails.screen.foryou

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class ForYouUiTest {
    private val trolltunga = Trail("trolltunga", "Trolltunga", "Hardangerfjord, Norway", "Ledge.", TrailDifficulty.HARD, 27000, 800, 600, 4.9, 1964, setOf(TrailFeature.LAKE), 2)
    private val halfDome = Trail("half-dome", "Half Dome", "Yosemite National Park, USA", "Granite.", TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486, setOf(TrailFeature.SUMMIT), 2)
    private val mist = Trail("mist-trail-to-nevada-fall", "Mist Trail to Nevada Fall", "Yosemite National Park, USA", "Falls.", TrailDifficulty.HARD, 8700, 610, 330, 4.8, 1200, setOf(TrailFeature.WATERFALL), 0)
    private val feed = ForYouFeed("trolltunga", "A weekend worth the walk", "Discover a quieter side of outside.", "half-dome", listOf("mist-trail-to-nevada-fall", "missing-trail"))

    @Test
    fun featureCardSectionAndRowsRenderAndOpenTheirTrails() = runDesktopComposeUiTest {
        val sent = mutableListOf<ForYouIntent>()
        val state = ForYouState(LoadState(feed, loading = false), listOf(trolltunga, halfDome, mist).associateBy { it.id }, LoadState(loading = false)) { sent += it }
        setContent { TrailsTheme { ForYouUi().Content(state, Modifier) } }
        onNodeWithText("For you").assertIsDisplayed()
        onNodeWithText("A weekend worth the walk").assertIsDisplayed()
        onNodeWithText("More like Half Dome").assertIsDisplayed()
        onNodeWithText("Mist Trail to Nevada Fall").performScrollTo().performClick()
        onNodeWithText("A weekend worth the walk").performScrollTo().performClick()
        assertEquals(listOf(mist, trolltunga), sent.filterIsInstance<ForYouIntent.OpenTrail>().map { it.trail })
    }

    @Test
    fun bookmarkDispatchesSaveWithFocusReturnCallback() = runDesktopComposeUiTest {
        val sent = mutableListOf<ForYouIntent>()
        val state = ForYouState(LoadState(feed, loading = false), listOf(trolltunga, halfDome, mist).associateBy { it.id }, LoadState(loading = false)) { sent += it }
        setContent { TrailsTheme { ForYouUi().Content(state, Modifier) } }
        onNodeWithText("Save Mist Trail to Nevada Fall").performScrollTo().performClick()
        runOnIdle {
            val save = sent.filterIsInstance<ForYouIntent.SaveTrail>().single()
            assertEquals(mist, save.trail)
            assertEquals(emptyList(), sent.filterIsInstance<ForYouIntent.OpenTrail>())
            save.onDismiss()
        }
    }

    @Test
    fun cachedFeedFailureRetainsContentAndOffersRetry() = runDesktopComposeUiTest {
        val sent = mutableListOf<ForYouIntent>()
        val state = ForYouState(LoadState(feed, loading = false, error = "Refresh failed"), listOf(trolltunga, halfDome, mist).associateBy { it.id }, LoadState(loading = false)) { sent += it }
        setContent { TrailsTheme { ForYouUi().Content(state, Modifier) } }
        onNodeWithText("A weekend worth the walk").assertIsDisplayed()
        onNodeWithText("Couldn’t refresh · Showing this device’s copy").performScrollTo().assertIsDisplayed()
        onNodeWithText("Try again").performScrollTo().performClick()
        assertEquals(listOf(ForYouIntent.Retry), sent.filterIsInstance<ForYouIntent.Retry>())
    }

    @Test
    fun uncachedOfflineFeedOffersRetry() = runDesktopComposeUiTest {
        val sent = mutableListOf<ForYouIntent>()
        val state = ForYouState(LoadState(loading = false, offline = true), emptyMap(), LoadState(loading = false)) { sent += it }
        setContent { TrailsTheme { ForYouUi().Content(state, Modifier) } }
        onNodeWithText("Offline · Picks aren’t on this device yet").assertIsDisplayed()
        onNodeWithText("Try again").performClick()
        assertEquals(listOf(ForYouIntent.Retry), sent.filterIsInstance<ForYouIntent.Retry>())
    }

    @Test
    fun missingRecommendationDetailsOfferRetry() = runDesktopComposeUiTest {
        val sent = mutableListOf<ForYouIntent>()
        val state = ForYouState(LoadState(feed, loading = false), mapOf(trolltunga.id to trolltunga), LoadState(loading = false)) { sent += it }
        setContent { TrailsTheme { ForYouUi().Content(state, Modifier) } }
        onNodeWithText("Trail details aren’t on this device yet").performScrollTo().assertIsDisplayed()
        onNodeWithText("Try again").performScrollTo().performClick()
        assertEquals(listOf(ForYouIntent.Retry), sent.filterIsInstance<ForYouIntent.Retry>())
    }
    @Test
    fun recommendationShowsSaveSyncTransitionsWithoutOpeningTrail() = runDesktopComposeUiTest {
        val target = mist
        val trails = listOf(trolltunga, halfDome, mist).associateBy { it.id }
        val sent = mutableListOf<ForYouIntent>()
        val snapshot = mutableStateOf(SavedSnapshot(emptyList(), mapOf(target.id to setOf("weekend")), listOf(target), emptyMap(), offline = true))
        setContent { TrailsTheme { ForYouUi().Content(ForYouState(LoadState(feed.copy(recommendedTrailIds = listOf(mist.id)), loading = false), trails, LoadState(snapshot.value, loading = false)) { sent += it }, Modifier) } }
        val cases = listOf(
            TrailSync(TrailSyncStatus.PENDING) to "Waiting for a connection",
            TrailSync(TrailSyncStatus.PENDING, canRetry = true) to "Sync needs attention · Local save is kept",
            TrailSync(TrailSyncStatus.SYNCING) to "Syncing…",
            TrailSync(TrailSyncStatus.FINISHING) to "Finishing your save",
            TrailSync(TrailSyncStatus.PARKED) to "Sync is paused · Saved here",
            TrailSync(TrailSyncStatus.PARKED, cause = TrailSyncCause.INCOMPATIBLE) to "App update needed to sync · Saved here",
        )
        for ((sync, message) in cases) {
            runOnIdle { snapshot.value = snapshot.value.copy(syncByTrail = mapOf(target.id to sync)) }
            onNodeWithText(message).performScrollTo().assertIsDisplayed()
            onNodeWithText("Try again").assertDoesNotExist()
        }
        runOnIdle { snapshot.value = snapshot.value.copy(syncByTrail = mapOf(target.id to TrailSync(TrailSyncStatus.SYNCED))) }
        for ((_, message) in cases) onNodeWithText(message).assertDoesNotExist()
        onNodeWithText("Edit saved collections for ${target.name}").performScrollTo().performClick()
        runOnIdle {
            val save = sent.filterIsInstance<ForYouIntent.SaveTrail>().single()
            assertEquals(target, save.trail)
            assertEquals(emptyList(), sent.filterIsInstance<ForYouIntent.OpenTrail>())
            save.onDismiss()
        }
    }
}
