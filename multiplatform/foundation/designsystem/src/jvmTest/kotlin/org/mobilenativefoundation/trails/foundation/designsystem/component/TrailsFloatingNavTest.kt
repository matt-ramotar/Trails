package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class TrailsFloatingNavTest {
    @Test
    fun indicatorTracksSelectedDestinationInRightToLeftLayout() = runDesktopComposeUiTest {
        var selected by mutableStateOf(TrailsDestination.EXPLORE)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TrailsTheme {
                    Box(Modifier.width(360.dp)) {
                        TrailsFloatingNav(TrailsDestination.entries, selected, { selected = it })
                    }
                }
            }
        }
        for (destination in listOf(TrailsDestination.SAVED, TrailsDestination.EXPLORE)) {
            onNodeWithText(destination.label).performClick().assertIsSelected()
            val tab = onNodeWithText(destination.label).fetchSemanticsNode().boundsInRoot
            val indicator = onNodeWithTag("Trails root navigation indicator").fetchSemanticsNode().boundsInRoot
            assertTrue(abs(indicator.left - tab.left) <= 1f && abs(indicator.right - tab.right) <= 1f,
                "The selected surface must stay under ${destination.label} in RTL: $indicator vs $tab")
        }
    }

    @Test
    fun allFiveLabelsFitAtDoubleFontScaleOnNarrowPhone() = assertAllDestinationsFit(width = 320, fontScale = 2f)

    @Test
    fun allFiveLabelsFitAtDoubleFontScaleOnPhone() = assertAllDestinationsFit(width = 360, fontScale = 2f)

    @Test
    fun defaultScaleKeepsTheSingleRowPill() = assertAllDestinationsFit(width = 360, fontScale = 1f)

    private fun assertAllDestinationsFit(width: Int, fontScale: Float) = runDesktopComposeUiTest {
        var selected by mutableStateOf(TrailsDestination.EXPLORE)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = fontScale)) {
                TrailsTheme {
                    Box(Modifier.width(width.dp)) {
                        TrailsFloatingNav(TrailsDestination.entries, selected, onSelect = { selected = it }, Modifier.testTag("nav"))
                    }
                }
            }
        }
        val navBounds = onNodeWithTag("nav").fetchSemanticsNode().boundsInRoot
        val tabBounds = TrailsDestination.entries.map { destination ->
            val tab = onNodeWithText(destination.label)
            tab.performClick().assertIsSelected()
            assertEquals(destination, selected)
            val bounds = tab.fetchSemanticsNode().boundsInRoot
            val text = onNodeWithText(destination.label, useUnmergedTree = true)
            val layouts = mutableListOf<TextLayoutResult>()
            text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertEquals(1, layout.lineCount, "${destination.label} must remain a whole, legible label at $fontScale scale and $width dp")
            // Compose can retain the wider paragraph constraint after sizing Text to its ink.
            // Check the rendered line rather than hasVisualOverflow's paragraph-width comparison.
            assertTrue(layout.getLineLeft(0) >= 0f && layout.getLineRight(0) <= layout.size.width + 1f, "${destination.label} clips horizontally")
            assertTrue(layout.getLineBottom(0) <= layout.size.height + 1f && !layout.isLineEllipsized(0), "${destination.label} clips vertically or ellipsizes")
            val textBounds = text.fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.contains(textBounds.topLeft) && bounds.contains(textBounds.bottomRight), "${destination.label} leaves its tab")
            assertTrue(navBounds.contains(bounds.topLeft) && navBounds.contains(bounds.bottomRight), "${destination.label} leaves the navigation")
            assertTrue(bounds.width >= 48f && bounds.height >= 48f, "${destination.label} needs a 48 dp target: $bounds")
            bounds
        }
        if (fontScale == 1f) {
            assertEquals(1, tabBounds.map { it.top }.distinct().size, "Default navigation remains one row")
            assertTrue(navBounds.height <= 88f, "Compact Native tabs must leave at least as much room for content")
        }
    }

    @Test
    fun selectingSavedReportsTheDestination() = runDesktopComposeUiTest {
        var selected: TrailsDestination? = null
        setContent {
            TrailsTheme { TrailsFloatingNav(listOf(TrailsDestination.EXPLORE, TrailsDestination.SAVED), TrailsDestination.EXPLORE, onSelect = { selected = it }) }
        }
        onNodeWithText("Explore").assertIsSelected()
        onNodeWithText("Saved").performClick()
        assertEquals(TrailsDestination.SAVED, selected)
    }
}
