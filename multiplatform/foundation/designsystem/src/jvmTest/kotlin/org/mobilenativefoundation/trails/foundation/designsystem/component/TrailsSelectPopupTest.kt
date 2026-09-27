package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class TrailsSelectPopupTest {
    @Test
    fun popupCentersAndClampsAtTheViewportEdgeInBothDirections() {
        val provider = TrailsSelectPopupPositionProvider(anchorGap = 8, viewportInset = 12)
        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            assertEquals(
                IntOffset(128, 256),
                provider.calculatePosition(IntRect(250, 200, 310, 248), IntSize(320, 640), direction, IntSize(180, 160)),
            )
        }
    }

    @Test
    fun popupFlipsAboveAndRequiresOversizedContentToRemeasureBeforeEntry() {
        var above = false
        var maximum = IntSize.Zero
        var ready = false
        val provider = TrailsSelectPopupPositionProvider(8, 12) { isAbove, maximumSize, positioned ->
            above = isAbove
            maximum = maximumSize
            ready = positioned
        }
        val anchor = IntRect(40, 560, 200, 608)
        assertEquals(IntOffset(30, 392), provider.calculatePosition(anchor, IntSize(320, 640), LayoutDirection.Ltr, IntSize(180, 160)))
        assertTrue(above)
        assertTrue(ready)
        assertEquals(IntSize(296, 540), maximum)
        provider.calculatePosition(anchor, IntSize(320, 640), LayoutDirection.Ltr, IntSize(500, 700))
        assertFalse(ready, "Entry waits for a menu sized to the available viewport")
    }

    @Test
    fun keyboardTraversalDoesNotSelectAndEscapeReturnsFocusToTheTrigger() = runDesktopComposeUiTest {
        var selected by mutableStateOf("Metric")
        setContent { TrailsTheme { TrailsSelect("Distance units", selected, listOf("Metric", "Imperial"), { it }, { selected = it }, Modifier.width(240.dp)) } }
        onNodeWithText("Distance units").performClick()
        onNodeWithText("Imperial").performSemanticsAction(SemanticsActions.RequestFocus) { assertTrue(it()) }
        onNodeWithText("Imperial").performKeyInput { pressKey(Key.DirectionUp) }
        onNodeWithText("Metric").assertIsFocused()
        assertEquals("Metric", selected)
        onNodeWithText("Metric").performKeyInput { pressKey(Key.Escape) }
        onNodeWithTag("trails-select-popup").assertDoesNotExist()
        onNodeWithText("Distance units").assertIsFocused()
        assertEquals("Metric", selected)
    }

    @Test
    fun exitingPopupKeepsItsSizeAndStopsAcceptingSelections() = runDesktopComposeUiTest {
        var selected by mutableStateOf("Metric")
        setContent { TrailsTheme { TrailsSelect("Distance units", selected, listOf("Metric", "Imperial"), { it }, { selected = it }, Modifier.width(240.dp)) } }
        onNodeWithText("Distance units").performClick()
        val width = onNodeWithTag("trails-select-popup").fetchSemanticsNode().boundsInRoot.width
        mainClock.autoAdvance = false
        onNodeWithText("Imperial").performClick()
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(64)
        onNodeWithText("Metric").assertIsNotEnabled()
        val exitingWidth = onNodeWithTag("trails-select-popup").fetchSemanticsNode().boundsInRoot.width
        assertTrue(abs(exitingWidth - width) <= 1f, "Native exit translates and fades without shrinking")
        assertEquals("Imperial", selected)
        mainClock.autoAdvance = true
        onNodeWithTag("trails-select-popup").assertDoesNotExist()
    }

    @Test
    fun longOptionListsScrollAndDisablingDoesNotReopenOnEnable() = runDesktopComposeUiTest {
        val options = (1..40).map { "Option $it" }
        var selected by mutableStateOf(options.first())
        var enabled by mutableStateOf(true)
        setContent { TrailsTheme { TrailsSelect("Trail ordering", selected, options, { it }, { selected = it }, Modifier.width(240.dp), enabled = enabled) } }
        onNodeWithText("Trail ordering").performClick()
        onNodeWithText(options.last()).performScrollTo().performClick()
        assertEquals(options.last(), selected)
        onNodeWithText("Trail ordering").performClick()
        runOnIdle { enabled = false }
        onNodeWithTag("trails-select-popup").assertDoesNotExist()
        runOnIdle { enabled = true }
        onNodeWithTag("trails-select-popup").assertDoesNotExist()
    }

    @Test
    fun compactSelectReservesChevronSpaceOnNarrowPhoneAtDoubleTextSize() = runDesktopComposeUiTest {
        var selected by mutableStateOf("Most popular")
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                TrailsTheme {
                    Row(Modifier.width(320.dp).padding(horizontal = 20.dp)) {
                        Text("50 trails", style = TrailsTheme.typography.labelLarge)
                        TrailsSelect("Sort by $selected", selected, listOf("Most popular", "Shortest"), { it }, { selected = it }, compact = true)
                    }
                }
            }
        }
        val trigger = onNodeWithText("Sort by Most popular").assertIsDisplayed()
        val triggerBounds = trigger.fetchSemanticsNode().boundsInRoot
        val labelBounds = onNodeWithTag("trails-select-label", useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val iconBounds = onNodeWithTag("trails-select-indicator", useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(iconBounds.width >= 15.9f && iconBounds.height >= 15.9f, "The chevron must retain its full 16 dp size")
        assertTrue(labelBounds.width > 0f && labelBounds.right <= iconBounds.left, "Wrapped text must not consume the chevron slot")
        assertTrue(triggerBounds.contains(labelBounds.topLeft) && triggerBounds.contains(labelBounds.bottomRight), "The label stays inside the trigger")
        assertTrue(triggerBounds.contains(iconBounds.topLeft) && triggerBounds.contains(iconBounds.bottomRight), "The chevron stays inside the trigger")
        assertTrue(triggerBounds.right <= 300f, "The compact select stays inside the narrow screen's content")
        trigger.performClick()
        onNodeWithText("Shortest").performClick()
        assertEquals("Shortest", selected)
    }
}
