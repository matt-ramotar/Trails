package org.mobilenativefoundation.trails.server.fake.services

import org.mobilenativefoundation.trails.server.BackendConfigProvider
import org.mobilenativefoundation.trails.server.error.ServerError
import org.mobilenativefoundation.trails.server.fake.internal.seed.SeedDataLoader
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ErrorSimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.LatencySimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.NetworkGate
import org.mobilenativefoundation.trails.server.fake.internal.storage.BackendTables
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredRun
import org.mobilenativefoundation.trails.server.model.RunFilter
import org.mobilenativefoundation.trails.server.model.RunHistoryEntry
import org.mobilenativefoundation.trails.server.model.RunRecord
import org.mobilenativefoundation.trails.server.model.RunStats
import org.mobilenativefoundation.trails.server.model.RunStatus
import org.mobilenativefoundation.trails.server.model.RunSummary
import org.mobilenativefoundation.trails.server.pagination.CursorPage
import org.mobilenativefoundation.trails.server.pagination.CursorRequest
import org.mobilenativefoundation.trails.server.pagination.OffsetPage
import org.mobilenativefoundation.trails.server.pagination.OffsetRequest
import org.mobilenativefoundation.trails.server.services.RunService

internal class FakeRunService(
    tables: BackendTables,
    networkGate: NetworkGate,
    latencySimulator: LatencySimulator,
    errorSimulator: ErrorSimulator,
    seedLoader: SeedDataLoader,
    configProvider: BackendConfigProvider,
) : BaseFakeService(tables, networkGate, latencySimulator, errorSimulator, seedLoader, configProvider), RunService {

    override suspend fun listRuns(
        resortId: String,
        request: OffsetRequest,
        filter: RunFilter?,
    ): OffsetPage<RunRecord> = withSimulation {
        val runs = applyFilter(tables.runs().query { it.resortId == resortId }, filter)
            .sortedBy { it.name }
        val offset = request.offset.coerceAtLeast(0)
        val limit = request.limit.coerceAtLeast(1)
        val end = (offset + limit).coerceAtMost(runs.size)
        val items = if (offset >= runs.size) emptyList() else runs.subList(offset, end)
        OffsetPage(
            items = items.map { toRecord(it) },
            offset = offset,
            limit = limit,
            totalCount = runs.size,
            hasMore = end < runs.size,
        )
    }

    override suspend fun getRun(runId: String): RunRecord = withSimulation {
        val run = tables.runs().get(runId) ?: throw ServerError.NotFound("run", runId)
        toRecord(run)
    }

    override suspend fun searchRuns(
        query: String,
        resortId: String?,
        limit: Int,
    ): List<RunSummary> = withSimulation {
        val normalized = query.trim().lowercase()
        val runs = tables.runs().getAll().filter {
            it.name.lowercase().contains(normalized) && (resortId == null || it.resortId == resortId)
        }
        runs.take(limit).map { toSummary(it) }
    }

    override suspend fun getRunHistory(
        token: String,
        request: CursorRequest,
    ): CursorPage<RunHistoryEntry> = withSimulation {
        CursorPage(
            items = emptyList(),
            nextCursor = null,
            prevCursor = null,
            totalCount = 0,
        )
    }

    private fun applyFilter(runs: List<StoredRun>, filter: RunFilter?): List<StoredRun> {
        if (filter == null) return runs
        return runs.filter { run ->
            val matchesDifficulty = filter.difficulty?.let { it.contains(run.difficulty) } ?: true
            val matchesOpen = filter.isOpen?.let { run.isOpen == it } ?: true
            val matchesVertical = filter.minVertical?.let { run.verticalFeet >= it } ?: true
            val matchesNight = filter.hasNightSkiing?.let {
                run.features.any { feature -> feature.contains("night", ignoreCase = true) }
            } ?: true
            matchesDifficulty && matchesOpen && matchesVertical && matchesNight
        }
    }

    private fun toRecord(run: StoredRun): RunRecord {
        return RunRecord(
            id = run.id,
            resortId = run.resortId,
            name = run.name,
            difficulty = run.difficulty,
            stats = RunStats(
                lengthMiles = run.lengthMiles,
                verticalFeet = run.verticalFeet,
                averageGradePct = run.averageGradePct,
                maxGradePct = run.maxGradePct,
            ),
            status = RunStatus(
                isOpen = run.isOpen,
                conditions = run.conditions,
                lastGroomed = run.lastGroomed,
            ),
            liftAccess = run.liftAccess,
            features = run.features,
            version = run.version,
        )
    }

    private fun toSummary(run: StoredRun): RunSummary {
        return RunSummary(
            id = run.id,
            resortId = run.resortId,
            name = run.name,
            difficulty = run.difficulty,
            verticalFeet = run.verticalFeet,
            distance = formatMiles(run.lengthMiles),
            liftAccess = run.liftAccess.firstOrNull().orEmpty(),
        )
    }

    private fun formatMiles(value: Float): String {
        return if (value % 1.0f == 0.0f) {
            "${value.toInt()} mi"
        } else {
            "${(value * 10).toInt() / 10.0f} mi"
        }
    }
}
