package org.mobilenativefoundation.trails.server.fake.services

import org.mobilenativefoundation.trails.server.BackendConfigProvider
import org.mobilenativefoundation.trails.server.error.ServerError
import org.mobilenativefoundation.trails.server.fake.internal.seed.SeedDataLoader
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ErrorSimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.LatencySimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.NetworkGate
import org.mobilenativefoundation.trails.server.fake.internal.storage.BackendTables
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredResort
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredUserFavorite
import org.mobilenativefoundation.trails.server.model.LocationRecord
import org.mobilenativefoundation.trails.server.model.ResortFilter
import org.mobilenativefoundation.trails.server.model.ResortRecord
import org.mobilenativefoundation.trails.server.model.ResortStats
import org.mobilenativefoundation.trails.server.model.ResortStatus
import org.mobilenativefoundation.trails.server.model.ResortSummary
import org.mobilenativefoundation.trails.server.pagination.OffsetPage
import org.mobilenativefoundation.trails.server.pagination.OffsetRequest
import org.mobilenativefoundation.trails.server.services.ResortService

internal class FakeResortService(
    tables: BackendTables,
    networkGate: NetworkGate,
    latencySimulator: LatencySimulator,
    errorSimulator: ErrorSimulator,
    seedLoader: SeedDataLoader,
    configProvider: BackendConfigProvider,
) : BaseFakeService(tables, networkGate, latencySimulator, errorSimulator, seedLoader, configProvider), ResortService {

    override suspend fun listResorts(
        request: OffsetRequest,
        filter: ResortFilter?,
    ): OffsetPage<ResortRecord> = withSimulation {
        val filtered = applyFilter(tables.resorts().getAll(), filter).sortedBy { it.name }
        val offset = request.offset.coerceAtLeast(0)
        val limit = request.limit.coerceAtLeast(1)
        val end = (offset + limit).coerceAtMost(filtered.size)
        val items = if (offset >= filtered.size) emptyList() else filtered.subList(offset, end)
        OffsetPage(
            items = items.map { toRecord(it) },
            offset = offset,
            limit = limit,
            totalCount = filtered.size,
            hasMore = end < filtered.size,
        )
    }

    override suspend fun getResort(resortId: String): ResortRecord = withSimulation {
        val resort = tables.resorts().get(resortId) ?: throw ServerError.NotFound("resort", resortId)
        toRecord(resort)
    }

    override suspend fun searchResorts(query: String, limit: Int): List<ResortSummary> = withSimulation {
        val normalized = query.trim().lowercase()
        tables.resorts().query {
            it.name.lowercase().contains(normalized) || it.city.lowercase().contains(normalized)
        }.take(limit).map { toSummary(it) }
    }

    override suspend fun favoriteResort(token: String, resortId: String) = withSimulation {
        val userId = userIdForToken(token)
        val key = "${userId}:${resortId}"
        if (!tables.userFavorites().exists(key)) {
            tables.userFavorites().put(
                StoredUserFavorite(
                    userId = userId,
                    resortId = resortId,
                    createdAt = 1704067200000,
                )
            )
        }
    }

    override suspend fun unfavoriteResort(token: String, resortId: String) = withSimulation {
        val userId = userIdForToken(token)
        tables.userFavorites().remove("${userId}:${resortId}")
        Unit
    }

    override suspend fun getFavoriteResorts(token: String): List<ResortSummary> = withSimulation {
        val userId = userIdForToken(token)
        val favorites = tables.userFavorites().query { it.userId == userId }.map { it.resortId }.toSet()
        tables.resorts().query { favorites.contains(it.id) }.map { toSummary(it) }
    }

    private suspend fun userIdForToken(token: String): String {
        val session = tables.sessions().get(token) ?: throw ServerError.Unauthorized()
        return session.userId
    }

    private fun applyFilter(resorts: List<StoredResort>, filter: ResortFilter?): List<StoredResort> {
        if (filter == null) return resorts
        return resorts.filter { resort ->
            val matchesCountry = filter.country?.let { resort.country.equals(it, ignoreCase = true) } ?: true
            val matchesState = filter.state?.let { resort.state?.equals(it, ignoreCase = true) ?: false } ?: true
            val matchesVertical = filter.minVertical?.let { resort.verticalFeet >= it } ?: true
            val matchesNight = filter.hasNightSkiing?.let {
                resort.amenities.any { amenity -> amenity.contains("Night", ignoreCase = true) }
            } ?: true
            val matchesOpen = filter.isOpen?.let { resort.isOpen == it } ?: true
            matchesCountry && matchesState && matchesVertical && matchesNight && matchesOpen
        }
    }

    private fun toRecord(resort: StoredResort): ResortRecord {
        return ResortRecord(
            id = resort.id,
            name = resort.name,
            location = LocationRecord(
                city = resort.city,
                state = resort.state,
                country = resort.country,
                latitude = resort.latitude,
                longitude = resort.longitude,
                elevation = resort.elevation,
            ),
            stats = ResortStats(
                runs = resort.runs,
                lifts = resort.lifts,
                verticalFeet = resort.verticalFeet,
                skiableAcres = resort.skiableAcres,
                snowfallAnnualInches = resort.snowfallAnnualInches,
            ),
            status = ResortStatus(
                isOpen = resort.isOpen,
                liftsOpen = resort.liftsOpen,
                liftsTotal = resort.lifts,
                runsOpen = resort.runsOpen,
                runsTotal = resort.runs,
                lastUpdated = "2025-01-11T12:00:00Z",
            ),
            amenities = resort.amenities,
            imageUrl = resort.imageUrl,
            version = resort.version,
        )
    }

    private fun toSummary(resort: StoredResort): ResortSummary {
        val location = if (resort.state != null) {
            "${resort.city}, ${resort.state}"
        } else {
            "${resort.city}, ${resort.country}"
        }
        return ResortSummary(
            id = resort.id,
            name = resort.name,
            location = location,
            imageUrl = resort.imageUrl,
        )
    }
}
