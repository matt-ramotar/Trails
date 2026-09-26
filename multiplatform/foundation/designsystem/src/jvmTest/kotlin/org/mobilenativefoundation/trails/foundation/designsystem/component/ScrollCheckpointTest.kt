package org.mobilenativefoundation.trails.foundation.designsystem.component


import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.foundation.designsystem.component.rememberCheckpointedListState
import org.mobilenativefoundation.trails.foundation.designsystem.component.rememberCheckpointedScrollState

@OptIn(ExperimentalTestApi::class)
class ScrollCheckpointTest {
    @Test
    fun readyLazyContentDoesNotClampRestoredIndexAgainstLoadingLayout() = runDesktopComposeUiTest(width = 400, height = 400) {
        var ready by mutableStateOf(false)
        var measureContent by mutableStateOf(true)
        lateinit var state: LazyListState
        val checkpoints = mutableListOf<Pair<Int, Int>>()
        setContent {
            state = rememberCheckpointedListState(60, 9, ready) { index, offset -> checkpoints += index to offset }
            LazyColumn(Modifier.size(200.dp).measureWhen(measureContent), state = state) {
                items(if (ready) 100 else 1) { Box(Modifier.fillMaxWidth().height(40.dp)) }
            }
        }
        runOnIdle {
            assertEquals(1, state.layoutInfo.totalItemsCount)
            assertTrue(checkpoints.isEmpty())
            measureContent = false
            ready = true
        }
        waitForIdle()
        runOnIdle { measureContent = true }
        waitForIdle()
        runOnIdle {
            assertEquals(60, state.firstVisibleItemIndex)
            assertEquals(9, state.firstVisibleItemScrollOffset)
            assertEquals(60 to 9, checkpoints.last())
            assertTrue(checkpoints.none { it.first == 0 }, "Loading layout must never overwrite the desired checkpoint")
        }
    }

    @Test
    fun readyDetailWaitsForActualContentPlacementBeforeRestoringOffset() = runDesktopComposeUiTest(width = 400, height = 400) {
        var ready by mutableStateOf(false)
        var measureContent by mutableStateOf(true)
        lateinit var state: ScrollState
        val checkpoints = mutableListOf<Int>()
        setContent {
            val checkpoint = rememberCheckpointedScrollState(390, ready) { checkpoints += it }
            state = checkpoint.state
            Column(Modifier.size(200.dp).measureWhen(measureContent).verticalScroll(state).then(checkpoint.contentModifier)) {
                repeat(if (ready) 30 else 1) { Box(Modifier.fillMaxWidth().height(40.dp)) }
            }
        }
        runOnIdle {
            assertEquals(0, state.maxValue)
            measureContent = false
            ready = true
        }
        waitForIdle()
        runOnIdle {
            assertTrue(checkpoints.isEmpty(), "Readiness without ready-content placement cannot publish a clamped position")
            measureContent = true
        }
        waitForIdle()
        runOnIdle {
            assertEquals(390, state.value)
            assertEquals(listOf(390), checkpoints)
        }
    }

    /** Delays actual child measurement/placement independently of repository readiness. */
    private fun Modifier.measureWhen(enabled: Boolean): Modifier = layout { measurable, constraints ->
        if (enabled) {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        } else {
            layout(constraints.maxWidth, constraints.maxHeight) { }
        }
    }
}
