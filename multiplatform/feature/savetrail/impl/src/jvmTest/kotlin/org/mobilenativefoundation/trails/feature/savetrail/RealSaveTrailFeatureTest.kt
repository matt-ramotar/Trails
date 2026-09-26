package org.mobilenativefoundation.trails.feature.savetrail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.catalog.TrailFeature
import org.mobilenativefoundation.trails.data.trail.saved.SaveOutcome
import org.mobilenativefoundation.trails.data.trail.saved.SavedRepository
import org.mobilenativefoundation.trails.data.trail.saved.SavedSnapshot
import org.mobilenativefoundation.trails.data.trail.saved.SetCollectionsCommand
import org.mobilenativefoundation.trails.data.trail.saved.TrailCollection

/** JOURNALED closes the sheet and names the destination in the toast. */
@OptIn(ExperimentalTestApi::class)
class RealSaveTrailFeatureTest {
    @Test
    fun savingOneCollectionNamesItInTheToastAndClosesTheSheet() = runDesktopComposeUiTest {
        val repository = JournalingSavedRepository()
        val feature = RealSaveTrailFeature(repository)
        feature.open(trail)
        setContent { TrailsTheme { Box(Modifier.fillMaxSize()) { feature.Content(onViewSaved = {}); feature.Toast() } } }
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("Weekend adventures").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Save to a list").assertExists()
        onNodeWithText("Weekend adventures").performScrollTo().performClick()
        onNodeWithText("Save trail").performScrollTo().performClick()

        waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("Saved to Weekend adventures").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("View").assertExists()
        onNodeWithText("Save to a list").assertDoesNotExist()
        assertEquals(listOf(setOf("weekend")), repository.saves.map { it.collectionIds })
    }

    @Test
    fun savingTwoCollectionsCountsThemInTheToast() = runDesktopComposeUiTest {
        val feature = RealSaveTrailFeature(JournalingSavedRepository())
        feature.open(trail)
        setContent { TrailsTheme { Box(Modifier.fillMaxSize()) { feature.Content(onViewSaved = {}); feature.Toast() } } }
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("Weekend adventures").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Weekend adventures").performScrollTo().performClick()
        onNodeWithText("My favorites").performScrollTo().performClick()
        onNodeWithText("Save trail").performScrollTo().performClick()

        waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("Saved to 2 lists").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Save to a list").assertDoesNotExist()
    }

    private class JournalingSavedRepository : SavedRepository {
        override val state = MutableStateFlow(LoadState(data = snapshot, loading = false))
        val saves = mutableListOf<SetCollectionsCommand>()
        override suspend fun save(command: SetCollectionsCommand): SaveOutcome {
            saves += command
            return SaveOutcome.Journaled(command, "mutation-${saves.size}")
        }
        override suspend fun reconcile(command: SetCollectionsCommand) = SaveOutcome.Journaled(command, "mutation-reconciled")
        override suspend fun refresh() = Unit
        override suspend fun retryPending() = Unit
    }

    private companion object {
        val trail = Trail("alpine-lake-loop", "Alpine Lake Loop", "Alpine", "A lake walk.", TrailDifficulty.MODERATE, 6400, 320, 120, 4.9, 128, setOf(TrailFeature.LAKE), 0)
        val snapshot = SavedSnapshot(
            listOf(TrailCollection("weekend", "Weekend adventures"), TrailCollection("favorites", "My favorites")),
            mapOf(trail.id to emptySet()),
            listOf(trail),
            emptyMap(),
        )
    }
}
