@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class)

package org.mobilenativefoundation.trails.data.trail

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mobilenativefoundation.store6.core.*
import org.mobilenativefoundation.store6.sqldelight.SqlDelightBookkeeper
import org.mobilenativefoundation.store6.sqldelight.SqlDelightSourceOfTruth
import org.mobilenativefoundation.trails.data.trail.db.M1Database

@Serializable
internal data class TrailPage(val trails: List<Trail>)

internal data class CatalogKey(val kind: String, val id: String) : StoreKey {
    override val namespace = StoreNamespace(kind)
    override fun canonicalId() = id
}

internal class RealTrailRepository(driver: SqlDriver, database: M1Database, private val backend: M1Backend) : TrailRepository {
    private val queries = database.m1Queries
    private val storageLifetime = StorageLifetime()
    val revision = MutableStateFlow(0L)
    private val source = storageLifetime.source(SqlDelightSourceOfTruth<CatalogKey, TrailPage>(
        driver, database,
        readQuery = { key -> queries.readCache(key.kind, key.id) { _, _, bytes -> decodePage(bytes) } },
        writeRow = { key, page ->
            queries.writeCache(key.kind, key.id, encodePage(page))
            if (key.kind == "query") page.trails.forEach { trail ->
                queries.writeCache("trail", trail.id, encodePage(TrailPage(listOf(trail))))
            }
        },
        deleteRow = { queries.deleteCache(it.kind, it.id) },
        deleteNamespaceRows = { queries.deleteNamespace(it.value) },
        deleteAllRows = { queries.deleteAllCache() },
    ))
    private val store = store<CatalogKey, TrailPage> {
        fetcher { key ->
            if (key.kind == "trail") TrailPage(listOf(backend.trail(key.id)))
            else TrailPage(backend.search(Json.decodeFromString(TrailQuery.serializer(), key.id)))
        }
        persistence(source)
        bookkeeper(storageLifetime.bookkeeper(SqlDelightBookkeeper(driver, database)))
    }

    override fun observeQuery(query: TrailQuery) = observe(query.key()).map { state ->
        LoadState(state.data?.trails?.sortedWith(query.sort.comparator()), state.loading, state.error, state.offline)
    }

    override fun observeTrail(id: String) = observe(CatalogKey("trail", id)).map { state ->
        LoadState(state.data?.trails?.singleOrNull(), state.loading, state.error, state.offline)
    }

    private fun observe(key: CatalogKey) = store.stream(key).loadStates(backend.config) { revision.update { it + 1 } }

    override suspend fun refreshQuery(query: TrailQuery) { refresh(query.key()) }
    override suspend fun refreshTrail(id: String) { refresh(CatalogKey("trail", id)) }
    private suspend fun refresh(key: CatalogKey) {
        // Invalidation keeps cached content visible and wakes every current collector.
        store.invalidate(key)
        try { store.get(key, Freshness.MustBeFresh); revision.update { it + 1 } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Store streams report the structured read failure. */ }
    }
    override suspend fun count(query: TrailQuery): LoadState<Int> = try {
        LoadState(store.get(query.key()).trails.size, loading = false, offline = backend.offline)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (failure: Exception) { LoadState(loading = false, error = failure.message, offline = backend.offline) }

    suspend fun cachedTrails(): List<Trail> = source.withTransaction {
        queries.allCache("trail").executeAsList().flatMap { decodePage(it.payload).trails }
    }
    suspend fun close() { store.close(); storageLifetime.retire() }
    // Sort is a presentation order over one cached membership, never part of the cache key:
    // changing it offline must reorder what is already stored instead of missing a new key.
    private fun TrailQuery.key() =
        CatalogKey("query", Json.encodeToString(TrailQuery.serializer(), normalized().copy(sort = TrailSort.MOST_POPULAR)))
    private fun encodePage(page: TrailPage) = Json.encodeToString(TrailPage.serializer(), page).encodeToByteArray()
    private fun decodePage(bytes: ByteArray) = Json.decodeFromString(TrailPage.serializer(), bytes.decodeToString())
}

internal fun StoreError.messageText(): String = when (this) {
    is StoreError.Fetch -> message
    is StoreError.Persistence -> message
    is StoreError.Conversion -> message
    is StoreError.FreshnessUnsatisfiable -> message
    is StoreError.Conflict -> message
    is StoreError.Missing -> message
}

internal fun Trail.matches(query: TrailQuery): Boolean =
    (query.text.isEmpty() || "${name.lowercase()} ${region.lowercase()}".contains(query.text)) &&
        (query.region == null || region.lowercase() == query.region) &&
        (query.difficulties.isEmpty() || difficulty in query.difficulties) &&
        distanceMeters >= query.minMeters && (query.maxMeters?.let { distanceMeters <= it } ?: true) &&
        elevationMeters >= query.minElevationGain && (query.maxElevationGain?.let { elevationMeters <= it } ?: true) &&
        (!query.dogFriendly || TrailFeature.DOG_FRIENDLY in features) &&
        (query.activities.isEmpty() || activities.any { it in query.activities }) &&
        features.containsAll(query.features)

/** `sortedWith` is stable, so the zero comparator keeps the catalog's recommended order. */
internal fun TrailSort.comparator(): Comparator<Trail> = when (this) {
    TrailSort.MOST_POPULAR -> Comparator { _, _ -> 0 }
    TrailSort.HIGHEST_RATED -> compareByDescending<Trail> { it.rating }.thenByDescending { it.reviewCount }
    TrailSort.SHORTEST -> compareBy { it.distanceMeters }
    TrailSort.LONGEST -> compareByDescending { it.distanceMeters }
}
