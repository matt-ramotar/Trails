package org.mobilenativefoundation.trails.server.services

import org.mobilenativefoundation.trails.server.model.RunFilter
import org.mobilenativefoundation.trails.server.model.RunHistoryEntry
import org.mobilenativefoundation.trails.server.model.RunRecord
import org.mobilenativefoundation.trails.server.model.RunSummary
import org.mobilenativefoundation.trails.server.pagination.CursorPage
import org.mobilenativefoundation.trails.server.pagination.CursorRequest
import org.mobilenativefoundation.trails.server.pagination.OffsetPage
import org.mobilenativefoundation.trails.server.pagination.OffsetRequest

interface RunService {
    suspend fun listRuns(
        resortId: String,
        request: OffsetRequest,
        filter: RunFilter? = null,
    ): OffsetPage<RunRecord>

    suspend fun getRun(runId: String): RunRecord

    suspend fun searchRuns(
        query: String,
        resortId: String? = null,
        limit: Int = 10,
    ): List<RunSummary>

    suspend fun getRunHistory(
        token: String,
        request: CursorRequest,
    ): CursorPage<RunHistoryEntry>
}
