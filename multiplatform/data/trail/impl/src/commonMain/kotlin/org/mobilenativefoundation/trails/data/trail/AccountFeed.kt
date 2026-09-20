@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class)

package org.mobilenativefoundation.trails.data.trail

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.mobilenativefoundation.store6.core.*
import org.mobilenativefoundation.store6.sqldelight.SqlDelightBookkeeper
import org.mobilenativefoundation.store6.sqldelight.SqlDelightSourceOfTruth
import org.mobilenativefoundation.trails.data.trail.db.M1Database

/** One account-scoped read, cached in the account's value database under its own cache_row namespace. */
internal class AccountFeed<T : Any>(
    kind: String,
    accountId: String,
    driver: SqlDriver,
    database: M1Database,
    private val backend: M1Backend,
    storageLifetime: StorageLifetime,
    private val serializer: KSerializer<T>,
    fetch: suspend () -> T,
) {
    private val queries = database.m1Queries
    private val key = CatalogKey(kind, accountId)
    private val source = storageLifetime.source(SqlDelightSourceOfTruth<CatalogKey, T>(
        driver, database,
        readQuery = { k -> queries.readCache(k.kind, k.id) { _, _, bytes -> Json.decodeFromString(serializer, bytes.decodeToString()) } },
        writeRow = { k, value -> queries.writeCache(k.kind, k.id, Json.encodeToString(serializer, value).encodeToByteArray()) },
        deleteRow = { queries.deleteCache(it.kind, it.id) },
        deleteNamespaceRows = { queries.deleteNamespace(it.value) },
        // The saved rows share this database; a clear-all for this feed only clears its namespace.
        deleteAllRows = { queries.deleteNamespace(kind) },
    ))
    private val store = store<CatalogKey, T> {
        fetcher { fetch() }
        persistence(source)
        bookkeeper(storageLifetime.bookkeeper(SqlDelightBookkeeper(driver, database)))
    }

    fun observe(): Flow<LoadState<T>> = store.stream(key).loadStates(backend.config)

    /** Invalidation keeps cached content visible; the stream reports a failed fetch. */
    suspend fun refresh() {
        store.invalidate(key)
        try { store.get(key, Freshness.MustBeFresh) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* The stream reports the structured read failure. */ }
    }

    suspend fun close() { store.close() }
}
