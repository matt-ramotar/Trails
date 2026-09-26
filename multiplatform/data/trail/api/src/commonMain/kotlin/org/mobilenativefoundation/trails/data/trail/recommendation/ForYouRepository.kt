package org.mobilenativefoundation.trails.data.trail.recommendation

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.trails.data.trail.LoadState

interface ForYouRepository {
    fun observe(): Flow<LoadState<ForYouFeed>>
    suspend fun refresh()
}
