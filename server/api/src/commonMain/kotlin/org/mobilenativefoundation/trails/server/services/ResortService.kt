package org.mobilenativefoundation.trails.server.services

import org.mobilenativefoundation.trails.server.model.ResortFilter
import org.mobilenativefoundation.trails.server.model.ResortRecord
import org.mobilenativefoundation.trails.server.model.ResortSummary
import org.mobilenativefoundation.trails.server.pagination.OffsetPage
import org.mobilenativefoundation.trails.server.pagination.OffsetRequest

interface ResortService {
    suspend fun listResorts(
        request: OffsetRequest,
        filter: ResortFilter? = null,
    ): OffsetPage<ResortRecord>

    suspend fun getResort(resortId: String): ResortRecord

    suspend fun searchResorts(query: String, limit: Int = 10): List<ResortSummary>

    suspend fun favoriteResort(token: String, resortId: String)
    suspend fun unfavoriteResort(token: String, resortId: String)
    suspend fun getFavoriteResorts(token: String): List<ResortSummary>
}
