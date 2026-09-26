package org.mobilenativefoundation.trails.data.trail.activity

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.trails.data.trail.LoadState

interface ActivityRepository {
    fun observe(): Flow<LoadState<List<CompletedActivity>>>
    suspend fun refresh()
}
