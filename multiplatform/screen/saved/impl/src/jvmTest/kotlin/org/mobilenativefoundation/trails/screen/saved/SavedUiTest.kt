package org.mobilenativefoundation.trails.screen.saved

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.catalog.TrailFeature
import org.mobilenativefoundation.trails.data.trail.saved.SavedSnapshot
import org.mobilenativefoundation.trails.data.trail.saved.TrailCollection

@OptIn(ExperimentalTestApi::class)
class SavedUiTest {
    private val trail = Trail("half-dome", "Half Dome", "Yosemite National Park, USA", "Granite summit.", TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486, setOf(TrailFeature.WATERFALL, TrailFeature.SUMMIT), 2)

    @Test
    fun tilesShowCountsPlaceholdersAndTheAllTrailsTab() = runDesktopComposeUiTest {
        val sent = mutableListOf<SavedIntent>()
        val snapshot = SavedSnapshot(
            collections = listOf(TrailCollection("weekend", "Weekend adventures"), TrailCollection("favorites", "Favorites")),
            memberships = mapOf(trail.id to setOf("weekend")), trails = listOf(trail), syncByTrail = emptyMap(),
        )
        val state = SavedState(LoadState(snapshot, loading = false), allTrails = false) { sent += it }
        setContent { TrailsTheme { SavedUi().Content(state, Modifier) } }
        onNodeWithText("1 trail").assertIsDisplayed()
        onNodeWithText("0 saved").assertIsDisplayed()
        onNodeWithContentDescription("Empty list").assertIsDisplayed()
        onNodeWithText("All trails").performClick()
        assertEquals(SavedIntent.SelectSegment(true), sent.filterIsInstance<SavedIntent.SelectSegment>().single())
    }

    @Test
    fun allTrailsWithoutCachedDetailsShowsTheMissingDetailsLineNotTheEmptyState() = runDesktopComposeUiTest {
        val sent = mutableListOf<SavedIntent>()
        val snapshot = SavedSnapshot(
            collections = listOf(TrailCollection("weekend", "Weekend adventures")),
            memberships = mapOf("half-dome" to setOf("weekend")), trails = emptyList(), syncByTrail = emptyMap(),
        )
        val state = SavedState(LoadState(snapshot, loading = false), allTrails = true) { sent += it }
        setContent { TrailsTheme { SavedUi().Content(state, Modifier) } }
        onNodeWithText("Some trail details aren’t on this device yet").assertIsDisplayed()
        onNodeWithText("Keep your next escape").assertDoesNotExist()
    }

    @Test
    fun normalTextKeepsTwoCollectionColumnsOnNarrowPhone() = verifyCollectionLayout(fontScale = 1f)

    @Test
    fun doubleTextUsesFullWidthCollectionTilesWithoutSplittingWords() = verifyCollectionLayout(fontScale = 2f)

    @Test
    fun missingTrailDetailsUseTheSameReadableCollectionLayout() = verifyCollectionLayout(fontScale = 2f, missingDetails = true)

    private fun verifyCollectionLayout(fontScale: Float, missingDetails: Boolean = false) = runDesktopComposeUiTest {
        val name = "Weekend adventures"
        val sent = mutableListOf<SavedIntent>()
        val snapshot = SavedSnapshot(
            collections = listOf(TrailCollection("weekend", name), TrailCollection("favorites", "Favorites")),
            memberships = mapOf(trail.id to setOf("weekend")),
            trails = if (missingDetails) emptyList() else listOf(trail),
            syncByTrail = emptyMap(),
        )
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                TrailsTheme {
                    Box(Modifier.width(320.dp)) {
                        SavedUi().Content(SavedState(LoadState(snapshot, loading = false), allTrails = false) { sent += it }, Modifier)
                    }
                }
            }
        }
        if (missingDetails) onNodeWithText("Some trail details aren’t on this device yet").performScrollTo().assertIsDisplayed()
        val first = onNodeWithText(name).performScrollTo().assertIsDisplayed()
        val firstBounds = first.fetchSemanticsNode().boundsInRoot
        if (fontScale == 2f) {
            assertTrue(firstBounds.width >= 279f, "Large text needs the full collection content width")
            val layouts = mutableListOf<TextLayoutResult>()
            onNodeWithText(name, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            for (line in 0 until layout.lineCount) {
                val start = layout.getLineStart(line)
                val end = layout.getLineEnd(line, visibleEnd = true)
                assertTrue(start == 0 || name[start - 1].isWhitespace(), "Collection names must wrap between words")
                assertTrue(end == name.length || name[end].isWhitespace(), "Collection names must not break within words")
                assertTrue(layout.getLineRight(line) <= layout.size.width + 1f && !layout.isLineEllipsized(line))
            }
        }
        first.performClick()
        assertEquals(listOf(SavedIntent.OpenCollection("weekend")), sent.filterIsInstance<SavedIntent.OpenCollection>())
        onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Favorites"))
        val secondBounds = onNodeWithText("Favorites").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        if (fontScale == 1f) {
            assertTrue(firstBounds.right < secondBounds.left, "Normal text keeps two columns")
        } else {
            assertTrue(secondBounds.width >= 279f, "Empty and missing-detail tiles also use the full width")
        }
    }
}
