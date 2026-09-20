package org.mobilenativefoundation.trails.screen.traildetail

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class TrailDetailUiTest {
    private val trail = Trail(
        "half-dome", "Half Dome", "Yosemite National Park, USA", "Climb past Yosemite's waterfalls and forest to the granite summit. ".repeat(12),
        TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486, setOf(TrailFeature.WATERFALL, TrailFeature.SUMMIT, TrailFeature.FOREST), 2,
    )

    @Test
    fun factsUseR2LabelsAndShowMoreExpandsTheDescription() = runDesktopComposeUiTest {
        val state = TrailDetailState(trail = LoadState(trail, loading = false), saved = LoadState(loading = false)) {}
        setContent { TrailsTheme { TrailDetailUi().Content(state, Modifier) } }
        onNodeWithText("Elev. gain").assertIsDisplayed()
        onNodeWithText("Route type").assertIsDisplayed()
        onNodeWithText("Out & back").assertIsDisplayed()
        onNodeWithText("Photo source").assertExists()
        onNodeWithText("Show more").performClick()
        onNodeWithText("Show less").performScrollTo().assertIsDisplayed()
    }
}
