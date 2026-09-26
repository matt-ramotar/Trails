package org.mobilenativefoundation.trails.data.trail.saved

import kotlinx.coroutines.flow.StateFlow
import org.mobilenativefoundation.trails.data.trail.LoadState

interface SavedRepository {
    val state: StateFlow<LoadState<SavedSnapshot>>
    suspend fun save(command: SetCollectionsCommand): SaveOutcome
    /** Inspects an earlier frozen command. Never creates a new durable mutation. */
    suspend fun reconcile(command: SetCollectionsCommand): SaveOutcome
    suspend fun refresh()
    suspend fun retryPending()
}
