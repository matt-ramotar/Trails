package org.mobilenativefoundation.trails.screen.traildetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.catalog.TrailFeature

@OptIn(ExperimentalTestApi::class)
class TrailDetailUiTest {
    private val trail = Trail(
        "half-dome", "Half Dome", "Yosemite National Park, USA", "Climb past Yosemite's waterfalls and forest to the granite summit. ".repeat(12),
        TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486, setOf(TrailFeature.WATERFALL, TrailFeature.SUMMIT, TrailFeature.FOREST), 2,
    )

    @Test
    fun factsShowTrailMeasurementsAndExpandableDescription() = runDesktopComposeUiTest {
        val state = TrailDetailState(trail = LoadState(trail, loading = false), saved = LoadState(loading = false)) {}
        setContent { TrailsTheme { TrailDetailUi().Content(state, Modifier) } }
        onNodeWithText("Elev. gain").assertIsDisplayed()
        onNodeWithText("Route type").assertIsDisplayed()
        onNodeWithText("Out & back").assertIsDisplayed()
        onNodeWithText("Photo source").assertExists()
        onNodeWithText("Show more").performClick()
        onNodeWithText("Show less").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun factsKeepFourColumnsAtNormalTextSize() = verifyFactsLayout(width = 448, fontScale = 1f, columns = 4)

    @Test
    fun doubleTextUsesTwoColumnsAndKeepsMeasurementsTogether() = verifyFactsLayout(width = 448, fontScale = 2f, columns = 2)

    @Test
    fun narrowDoubleTextFactsWrapBetweenWords() = verifyFactsLayout(width = 320, fontScale = 2f, columns = 2)

    private fun verifyFactsLayout(width: Int, fontScale: Float, columns: Int) = runDesktopComposeUiTest {
        val state = TrailDetailState(trail = LoadState(trail, loading = false), saved = LoadState(loading = false)) {}
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                TrailsTheme {
                    Box(Modifier.width(width.dp)) { TrailDetailUi().Content(state, Modifier) }
                }
            }
        }
        onNodeWithText("Route type").performScrollTo().assertIsDisplayed()
        val labels = listOf("Length", "Elev. gain", "Est. time", "Route type")
        val bounds = labels.map { onNodeWithText(it).assertIsDisplayed().fetchSemanticsNode().boundsInRoot }
        assertEquals(columns, bounds.count { it.top == bounds.first().top }, "Facts must use the available width at this text size")
        if (columns == 2) {
            assertTrue(bounds[2].top >= bounds[0].bottom && bounds[3].top == bounds[2].top)
        }
        val values = listOf("22.7 km", "1463 m", "11 h 0 min", "Out & back")
        for (value in values + labels) {
            val text = onNodeWithText(value, useUnmergedTree = true)
            val layouts = mutableListOf<TextLayoutResult>()
            text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            if (width == 448 && fontScale == 2f && (value in values.take(2) || value in labels)) {
                assertEquals(1, layout.lineCount, "Measurements, units and labels should stay together when two columns provide room")
            }
            for (line in 0 until layout.lineCount) {
                val start = layout.getLineStart(line)
                val end = layout.getLineEnd(line, visibleEnd = true)
                assertTrue(start == 0 || value[start - 1].isWhitespace(), "Fact values must not split within words")
                assertTrue(end == value.length || value[end].isWhitespace(), "Fact values must wrap between words")
                assertTrue(layout.getLineRight(line) <= layout.size.width + 1f && !layout.isLineEllipsized(line))
            }
            assertTrue(text.fetchSemanticsNode().boundsInRoot.height >= layout.size.height - 1f, "Fact values must remain fully visible")
        }
    }
}
