package org.mobilenativefoundation.trails.screen.activity

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
class ActivityUiTest {
    private val now = 1_789_905_600_000L
    private val halfDome = Trail("half-dome", "Half Dome", "Yosemite National Park, USA", "Granite.", TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486, setOf(TrailFeature.SUMMIT), 2)
    private val activities = listOf(
        CompletedActivity("sample-half-dome", "half-dome", "Half Dome", now - 86_400_000L, 22700, 660, 1463),
        CompletedActivity("sample-takao", "mount-takao-trail-1", "Mount Takao Trail 1", now - 3 * 86_400_000L, 7600, 180, 411),
    )

    @Test
    fun historyRendersTotalsHeroAndRowsAndRowsOpenTheirTrail() = runDesktopComposeUiTest {
        val sent = mutableListOf<ActivityIntent>()
        val state = ActivityState(LoadState(activities, loading = false), mapOf(halfDome.id to halfDome), LoadState(loading = false), now) { sent += it }
        setContent { TrailsTheme { ActivityUi().Content(state, Modifier) } }
        onNodeWithText("Activity").assertIsDisplayed()
        for (value in listOf("SEPTEMBER SO FAR", "30.3", "km walked", "2", "trails", "14h", "outside")) {
            onNodeWithText(value, useUnmergedTree = true).assertIsDisplayed()
        }
        onNodeWithText("Your recent adventures").assertIsDisplayed()
        onNodeWithText("Half Dome").assertIsDisplayed()
        onNodeWithText("Mount Takao Trail 1").performScrollTo().performClick()
        assertEquals(ActivityIntent.OpenTrail("mount-takao-trail-1"), sent.filterIsInstance<ActivityIntent.OpenTrail>().single())
    }

    @Test
    fun emptyHistoryOffersExplore() = runDesktopComposeUiTest {
        val sent = mutableListOf<ActivityIntent>()
        setContent { TrailsTheme { ActivityUi().Content(ActivityState(LoadState(emptyList(), loading = false), emptyMap(), LoadState(loading = false), now) { sent += it }, Modifier) } }
        onNodeWithText("Your hikes will show up here").assertIsDisplayed()
        onNodeWithText("Explore trails").performScrollTo().performClick()
        assertEquals(ActivityIntent.Explore, sent.filterIsInstance<ActivityIntent.Explore>().single())
    }
    @Test
    fun heroShowsSaveSyncTransitionsWithoutOpeningTrail() = verifySyncStatuses(hero = true)

    @Test
    fun rowShowsSaveSyncTransitionsWithoutOpeningTrail() = verifySyncStatuses(hero = false)

    private fun verifySyncStatuses(hero: Boolean) = runDesktopComposeUiTest {
        val target = if (hero) halfDome else halfDome.copy(id = "mount-takao-trail-1", name = "Mount Takao Trail 1")
        val trails = listOf(halfDome, target).associateBy { it.id }
        val sent = mutableListOf<ActivityIntent>()
        val snapshot = mutableStateOf(SavedSnapshot(emptyList(), mapOf(target.id to setOf("weekend")), listOf(target), emptyMap(), offline = true))
        setContent { TrailsTheme { ActivityUi().Content(ActivityState(LoadState(activities, loading = false), trails, LoadState(snapshot.value, loading = false), now) { sent += it }, Modifier) } }
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
            val save = sent.filterIsInstance<ActivityIntent.SaveTrail>().single()
            assertEquals(target, save.trail)
            assertEquals(emptyList(), sent.filterIsInstance<ActivityIntent.OpenTrail>())
            save.onDismiss()
        }
    }
}
