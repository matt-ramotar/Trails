package org.mobilenativefoundation.trails.feat.savetrail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import org.mobilenativefoundation.trails.data.trail.Trail
import org.mobilenativefoundation.trails.data.trail.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.TrailFeature
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class TrailSaveCardTest {
    private val trail = Trail(
        "half-dome", "Half Dome", "Yosemite National Park, USA", "Granite summit.", TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486,
        setOf(TrailFeature.WATERFALL, TrailFeature.SUMMIT, TrailFeature.FOREST), 2,
    )

    @Test
    fun cardShowsStructuredFactsHighlightAndHeartTarget() = runDesktopComposeUiTest {
        setContent { TrailsTheme { TrailSaveCard(trail, snapshot = null, onOpen = {}, onSave = {}) } }
        onNodeWithText("4.9 (2486)").assertIsDisplayed()
        onNodeWithText("Hard").assertIsDisplayed()
        onNodeWithText("22.7 km").assertIsDisplayed()
        onNodeWithText("Waterfall").assertIsDisplayed()
        onNodeWithText("Save Half Dome").assertIsDisplayed()
    }
}
