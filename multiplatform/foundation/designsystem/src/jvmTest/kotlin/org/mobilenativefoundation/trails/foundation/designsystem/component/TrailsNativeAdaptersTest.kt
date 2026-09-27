package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class TrailsNativeAdaptersTest {
    @Test
    fun nestedCardActionRemainsIndependentFromOpeningTheCard() = runDesktopComposeUiTest {
        var opened = 0
        var saved = 0
        setContent {
            TrailsTheme {
                TrailsCard(Modifier.width(320.dp).testTag("card"), onClick = { opened++ }) {
                    Text("Mount Washington")
                    TrailsButton("Save trail", { saved++ })
                }
            }
        }
        onNodeWithText("Save trail").performClick()
        assertEquals(1, saved)
        assertEquals(0, opened)
        onNodeWithTag("card").performClick()
        assertEquals(1, opened)
        assertEquals(1, saved)
    }

    @Test
    fun disabledListItemCannotDispatchThroughATouch() = runDesktopComposeUiTest {
        var opened = false
        setContent {
            TrailsTheme {
                TrailsListGroup(Modifier.width(320.dp)) {
                    TrailsListItem(onClick = { opened = true }, enabled = false) { Text("Unavailable collection") }
                }
            }
        }
        onNodeWithText("Unavailable collection").assertIsNotEnabled().performTouchInput { click() }
        assertFalse(opened)
    }

    @Test
    fun listTextCanGrowAtDoubleFontScaleWithoutClippingItsActionTarget() = runDesktopComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                TrailsTheme {
                    TrailsListGroup(Modifier.width(260.dp)) {
                        TrailsListItem(Modifier.testTag("item"), onClick = {}) {
                            Text("A long saved collection title for this hiking season", Modifier.testTag("title"))
                        }
                    }
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        onNodeWithTag("title", useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertTrue(layout.lineCount > 1)
        assertTrue((0 until layout.lineCount).none(layout::isLineEllipsized))
        assertTrue(layout.getLineBottom(layout.lineCount - 1) <= layout.size.height + 1f)
        val item = onNodeWithTag("item").fetchSemanticsNode().boundsInRoot
        val title = onNodeWithTag("title", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(item.contains(title.topLeft) && item.contains(title.bottomRight))
        assertTrue(item.height >= 48f)
    }

    @Test
    fun switchRowOwnsOneStatefulActionAndDisabledRowDoesNotChangeIt() = runDesktopComposeUiTest {
        var checked by mutableStateOf(false)
        setContent {
            TrailsTheme {
                Column {
                    TrailsSwitchRow("Offline maps", checked, { checked = it })
                    TrailsSwitchRow("Unavailable setting", false, { error("Disabled switch dispatched") }, enabled = false)
                }
            }
        }
        onNodeWithText("Offline maps").assertIsOff().performClick().assertIsOn()
        assertTrue(checked)
        onNodeWithText("Unavailable setting").assertIsNotEnabled().performTouchInput { click() }
    }

    @Test
    fun sliderRetainsNativeRangeActionAndQuantizesToConfiguredSteps() = runDesktopComposeUiTest {
        var value by mutableFloatStateOf(0f)
        setContent {
            TrailsTheme {
                TrailsSlider(value, { value = it }, 0f..100f, "Distance", "$value kilometers", Modifier.testTag("slider"), steps = 3)
            }
        }
        onNodeWithTag("slider").performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(63f)) }
        assertEquals(75f, value)
        onNodeWithTag("slider").assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(75f, 0f..100f, 3)))
        assertTrue(onNodeWithTag("slider").fetchSemanticsNode().boundsInRoot.height >= 48f)
    }

    @Test
    fun selectingAnOptionUpdatesStateAndClosesTheNativePopup() = runDesktopComposeUiTest {
        var selected by mutableStateOf("Metric")
        setContent {
            TrailsTheme {
                TrailsSelect("Distance units", selected, listOf("Metric", "Imperial"), { it }, { selected = it })
            }
        }
        onNodeWithText("Distance units").performClick()
        onNodeWithText("Metric").assertIsSelected()
        onNodeWithText("Imperial").performClick()
        assertEquals("Imperial", selected)
        onNodeWithText("Distance units").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Imperial"))
    }

    @Test
    fun segmentSelectionMovesWithoutChangingItsSingleSelectionSemantics() = runDesktopComposeUiTest {
        var selected by mutableStateOf("Nearby")
        setContent {
            TrailsTheme {
                TrailsSegmentedControl(listOf("Nearby", "Saved"), selected, { selected = it }, { it }, Modifier.width(280.dp))
            }
        }
        onNodeWithText("Nearby").assertIsSelected()
        onNodeWithText("Saved").performClick().assertIsSelected()
        assertEquals("Saved", selected)
    }
}
