package org.mobilenativefoundation.trails.data.trail.catalog

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.trails.data.trail.LoadState

interface TrailRepository {
    fun observeQuery(query: TrailQuery): Flow<LoadState<List<Trail>>>
    fun observeTrail(id: String): Flow<LoadState<Trail>>
    suspend fun refreshQuery(query: TrailQuery)
    suspend fun refreshTrail(id: String)
    suspend fun count(query: TrailQuery): LoadState<Int>
}
