package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class TrailsNativeFocusAndRtlTest {
    @Test
    fun pressingAndDisablingACardTransformsItsSurfaceAlongWithItsContent() = runDesktopComposeUiTest {
        var enabled by mutableStateOf(true)
        mainClock.autoAdvance = false
        setContent {
            TrailsTheme {
                Box(Modifier.size(260.dp, 120.dp).background(Color.Black).testTag("frame"), contentAlignment = Alignment.Center) {
                    TrailsCard(Modifier.width(240.dp).height(96.dp).testTag("card"), onClick = {}, enabled = enabled, contentPadding = PaddingValues(0.dp)) {}
                }
            }
        }
        val before = onNodeWithTag("frame").captureToImage().toPixelMap()[11, 60]
        onNodeWithTag("card").performTouchInput { down(center) }
        mainClock.advanceTimeBy(400)
        val pressed = onNodeWithTag("frame").captureToImage().toPixelMap()[11, 60]
        assertTrue(before.green - pressed.green > 0.5f, "The white card surface must shrink with its pressed content")
        onNodeWithTag("card").performTouchInput { up() }
        runOnIdle { enabled = false }
        mainClock.advanceTimeBy(400)
        val disabled = onNodeWithTag("frame").captureToImage().toPixelMap()[130, 60]
        assertTrue(abs(disabled.green - 0.5f) < 0.03f, "Disabled opacity must include the surface itself")
    }

    @Test
    fun rightToLeftSelectionIndicatorStaysBehindTheSelectedTabAtDoubleFontScale() = runDesktopComposeUiTest {
        var selected by mutableStateOf("Nearby")
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalDensity provides Density(1f, 2f)) {
                TrailsTheme {
                    TrailsSegmentedControl(listOf("Nearby", "Saved"), selected, { selected = it }, { it }, Modifier.width(360.dp))
                }
            }
        }
        listOf("Nearby", "Saved", "Nearby").forEach { label ->
            onNodeWithText(label).performClick()
            val tab = onNodeWithText(label).fetchSemanticsNode().boundsInRoot
            val indicator = onNodeWithTag("trails-segment-indicator").fetchSemanticsNode().boundsInRoot
            assertTrue(abs(tab.left - indicator.left) <= 1f, "$label indicator starts outside its tab: $tab vs $indicator")
            assertTrue(abs(tab.right - indicator.right) <= 1f, "$label indicator ends outside its tab: $tab vs $indicator")
            assertTrue(abs(tab.top - indicator.top) <= 1f && abs(tab.bottom - indicator.bottom) <= 1f)
        }
        val nearby = onNodeWithText("Nearby").fetchSemanticsNode().boundsInRoot
        val saved = onNodeWithText("Saved").fetchSemanticsNode().boundsInRoot
        assertTrue(nearby.left > saved.left, "The test must exercise a physically reversed tab order")
    }

    @Test
    fun keyboardFocusPaintsAVisibleRingOnTabsSwitchAndSelectWithoutChangingTheirState() = runDesktopComposeUiTest {
        var focusManager: FocusManager? = null
        setContent {
            focusManager = LocalFocusManager.current
            TrailsTheme {
                Column(Modifier.width(320.dp)) {
                    TrailsSegmentedControl(listOf("Nearby", "Saved"), "Nearby", { error("Focus changed tab selection") }, { it })
                    TrailsSwitchRow("Offline maps", false, { error("Focus toggled switch") })
                    TrailsSelect("Distance units", "Metric", listOf("Metric", "Imperial"), { it }, { error("Focus changed selection") })
                }
            }
        }
        listOf("Saved", "Offline maps", "Distance units").forEach { label ->
            runOnIdle { focusManager!!.clearFocus(force = true) }
            assertVisibleFocusRing(onNodeWithText(label))
        }
    }

    @Test
    fun popupOptionsShowKeyboardFocusBeforeSelection() = runDesktopComposeUiTest {
        var selected by mutableStateOf("Metric")
        setContent {
            TrailsTheme {
                TrailsSelect("Distance units", selected, listOf("Metric", "Imperial"), { it }, { selected = it }, Modifier.width(320.dp))
            }
        }
        onNodeWithText("Distance units").performClick()
        assertVisibleFocusRing(onNodeWithText("Imperial"))
        assertEquals("Metric", selected)
        onNodeWithText("Imperial").performClick()
        assertEquals("Imperial", selected)
    }

    private fun assertVisibleFocusRing(node: SemanticsNodeInteraction) {
        val before = node.captureToImage().toPixelMap()
        val center = before.width / 2
        val beforePixel = before[center, 1]
        node.performSemanticsAction(SemanticsActions.RequestFocus) { assertTrue(it()) }.assertIsFocused()
        val after = node.captureToImage().toPixelMap()
        assertEquals(Color(0xFF1D4B35), after[center, 1], "The focused target needs a visible Trails accent ring")
        assertTrue(beforePixel != after[center, 1], "Focus must paint a new visible cue")
    }
}
