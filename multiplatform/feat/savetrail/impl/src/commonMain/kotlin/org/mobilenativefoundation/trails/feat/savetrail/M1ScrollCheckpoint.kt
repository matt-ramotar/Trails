package org.mobilenativefoundation.trails.feat.savetrail

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

/** Restore after repository content is laid out; write settled positions, never every drag frame. */
@Composable
fun rememberCheckpointedListState(
    initialIndex: Int,
    initialOffset: Int,
    ready: Boolean,
    onPosition: (index: Int, offset: Int) -> Unit,
): LazyListState {
    val state = rememberLazyListState(initialIndex, initialOffset)
    val start = remember { initialIndex to initialOffset }
    var restored by remember { mutableStateOf(false) }
    val latestReady by rememberUpdatedState(ready)
    val latestPosition by rememberUpdatedState(onPosition)
    LaunchedEffect(ready) {
        if (ready && !restored) {
            // Defer measurement until the ready composition has applied. An immediate scroll
            // can re-enter a pending lazy composition; the next measure also avoids clamping
            // the requested checkpoint against the previous loading layout.
            state.requestScrollToItem(start.first, start.second)
            restored = true
        }
    }
    LaunchedEffect(state) {
        snapshotFlow {
            if (restored && latestReady && !state.isScrollInProgress) state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset else null
        }.filterNotNull().distinctUntilChanged().collect { (index, offset) -> latestPosition(index, offset) }
    }
    DisposableEffect(state) {
        onDispose {
            if (restored && latestReady) latestPosition(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
        }
    }
    return state
}

@Composable
fun rememberCheckpointedScrollState(initialOffset: Int, ready: Boolean, onPosition: (Int) -> Unit): M1CheckpointedScrollState {
    val state = rememberScrollState(initialOffset)
    val start = remember { initialOffset }
    var restored by remember { mutableStateOf(false) }
    var contentPlaced by remember { mutableStateOf(false) }
    val latestReady by rememberUpdatedState(ready)
    val latestPosition by rememberUpdatedState(onPosition)
    LaunchedEffect(ready, contentPlaced) {
        if (ready && contentPlaced && !restored) {
            state.scrollTo(start)
            restored = true
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { if (restored && latestReady && !state.isScrollInProgress) state.value else null }
            .filterNotNull().distinctUntilChanged().collect { latestPosition(it) }
    }
    DisposableEffect(state) {
        onDispose { if (restored && latestReady) latestPosition(state.value) }
    }
    // Attach inside verticalScroll. Placement follows the ready content's measurement, so
    // ScrollState.maxValue is current; a frame callback alone makes no such guarantee.
    return M1CheckpointedScrollState(state, Modifier.onGloballyPositioned { if (ready) contentPlaced = true })
}

data class M1CheckpointedScrollState(val state: ScrollState, val contentModifier: Modifier)
