package org.mobilenativefoundation.trails.ui.trail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.catalog.TrailFeature
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

    @Test
    fun cardAndBookmarkKeepIndependentActionsAndFocusReturn() = runDesktopComposeUiTest {
        var opens = 0
        var saves = 0
        var onDismiss: (() -> Unit)? = null
        setContent {
            TrailsTheme {
                TrailSaveCard(
                    trail,
                    snapshot = null,
                    onOpen = { opens += 1 },
                    onSave = { restoreFocus -> saves += 1; onDismiss = restoreFocus },
                )
            }
        }
        onNodeWithText("Half Dome").assertHasClickAction().performClick()
        onNodeWithText("Save Half Dome").assertHasClickAction().performClick()
        runOnIdle {
            assertEquals(1, opens)
            assertEquals(1, saves)
            assertNotNull(onDismiss).invoke()
        }
    }

    @Test
    fun highlightAndBookmarkFitAtDoubleFontScaleOnNarrowPhone() = runDesktopComposeUiTest {
        val summitTrail = trail.copy(features = setOf(TrailFeature.SUMMIT))
        var opens = 0
        var saves = 0
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                TrailsTheme {
                    Box(Modifier.width(320.dp).padding(horizontal = 20.dp)) {
                        TrailSaveCard(
                            summitTrail,
                            snapshot = null,
                            onOpen = { opens++ },
                            onSave = { saves++ },
                            modifier = Modifier.testTag("card"),
                        )
                    }
                }
            }
        }
        val highlight = onNodeWithText("Summit views", useUnmergedTree = true).assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        highlight.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        val textBounds = highlight.fetchSemanticsNode().boundsInRoot
        val cardBounds = onNodeWithTag("card").fetchSemanticsNode().boundsInRoot
        val bookmark = onNodeWithText("Save Half Dome").assertIsDisplayed().assertHasClickAction()
        val bookmarkBounds = bookmark.fetchSemanticsNode().boundsInRoot
        assertTrue(textBounds.right <= bookmarkBounds.left, "The highlight must leave room for the separate bookmark")
        assertTrue(textBounds.left >= cardBounds.left && textBounds.right <= cardBounds.right)
        assertTrue(textBounds.height >= layout.size.height - 1f, "The wrapped highlight must not be clipped by its photo")
        for (line in 0 until layout.lineCount) {
            assertTrue(layout.getLineLeft(line) >= 0f && layout.getLineRight(line) <= layout.size.width + 1f)
            assertTrue(layout.getLineBottom(line) <= layout.size.height + 1f && !layout.isLineEllipsized(line))
        }
        assertTrue(bookmarkBounds.width >= 48f && bookmarkBounds.height >= 48f, "The bookmark keeps a full touch target")
        bookmark.performClick()
        runOnIdle { assertEquals(0, opens); assertEquals(1, saves) }
        onNodeWithText("Half Dome").performClick()
        runOnIdle { assertEquals(1, opens); assertEquals(1, saves) }
    }
}
