@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class)

package org.mobilenativefoundation.trails.data.trail.account

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.mobilenativefoundation.store6.sqldelight.SqlDelightBookkeeper
import org.mobilenativefoundation.trails.data.backend.BackendConfig
import org.mobilenativefoundation.trails.data.backend.NetworkMode
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.data.trail.internal.account.AccountRecoveryPolicy
import org.mobilenativefoundation.trails.data.trail.internal.backend.PersistentFakeBackend
import org.mobilenativefoundation.trails.data.trail.internal.backend.SEED_VERSION
import org.mobilenativefoundation.trails.data.trail.internal.backend.worldTrails
import org.mobilenativefoundation.trails.data.trail.internal.catalog.CatalogKey
import org.mobilenativefoundation.trails.data.trail.saved.SaveOutcome
import org.mobilenativefoundation.trails.data.trail.saved.SetCollectionsCommand
import org.mobilenativefoundation.trails.data.trail.saved.TrailSyncStatus
import org.mobilenativefoundation.trails.data.trail.storage.PlatformTrailDatabaseDriverFactory
import org.mobilenativefoundation.trails.data.trail.storage.TrailDatabaseDriverFactory
import org.mobilenativefoundation.trails.data.trail.storage.db.TrailDataDatabase

/** Upgrade real file databases with a Store6-written, fresh catalog page and durable saves. */
class TrailReseedTest {
    private val online = BackendConfig(latencyRange = 0.milliseconds..0.milliseconds)
    private val offline = online.copy(networkMode = NetworkMode.OFFLINE)
    private val queryKey = CatalogKey("query", Json.encodeToString(TrailQuery.serializer(), TrailQuery().normalized()))
    private val legacyTrails = (0 until 8).map { index ->
        worldTrails[index].copy(id = "retired-trail-$index", name = "Retired trail $index", region = "Legacy")
    }

    @Test
    fun reseedClearsFreshCatalogAndPreservesAuthorityAndRetiredMemberships() = runBlocking {
        val directory = Files.createTempDirectory("trails-reseed").toFile()
        val drivers = PlatformTrailDatabaseDriverFactory(directory)
        val commands = listOf(
            SetCollectionsCommand("retired-save", legacyTrails.first().id, setOf("weekend")),
            SetCollectionsCommand("world-save", worldTrails.first().id, setOf("favorites")),
        )
        var runtime: RealTrailDataFactory? = null
        try {
            prepareLegacyInstall(drivers, this) { original ->
                val account = original.open("alice")
                commands.forEach { command -> assertIs<SaveOutcome.Journaled>(account.saved.save(command)) }
                account.saved.retryPending()
                withTimeout(10_000) {
                    account.saved.state.first { state -> commands.all { command ->
                        state.data?.memberships?.get(command.trailId) == command.collectionIds &&
                            state.data?.syncByTrail?.get(command.trailId)?.status == TrailSyncStatus.SYNCED
                    } }
                }
            }
            assertFreshQueryMetadata(drivers)
            val before = authority(drivers)
            val upgraded = factory(drivers, this).also { runtime = it }
            assertCacheEmpty(drivers)
            assertEquals(before, authority(drivers), "Reseeding must not alter authority, receipts, retirement, or counters")
            val evidence = upgraded.backendEvidence()
            assertEquals(2L, evidence.applications)
            assertEquals(2L, evidence.receipts)
            upgraded.restoreBackendConfig()
            val loaded = withTimeout(10_000) { upgraded.trails.observeQuery(TrailQuery()).first { it.data != null && !it.loading } }.data!!
            assertEquals(worldTrails, loaded)
            assertEquals(50, loaded.size)
            val saved = upgraded.open("alice").saved
            commands.forEach { command ->
                assertEquals(command.collectionIds, saved.state.value.data?.memberships?.get(command.trailId))
                assertIs<SaveOutcome.Journaled>(saved.reconcile(command))
            }
            assertFalse(saved.state.value.data!!.trails.any { it.id == legacyTrails.first().id })
            assertEquals(evidence.applications, upgraded.backendEvidence().applications)
            assertEquals(evidence.receipts, upgraded.backendEvidence().receipts)
            assertEquals(evidence.pushAttempts, upgraded.backendEvidence().pushAttempts)
            upgraded.applyBackendConfig(offline)
            upgraded.close()
            runtime = null

            // Once both markers are current, reopening retains the fetched rows and freshness.
            assertFreshQueryMetadata(drivers)
            val cache = cacheRows(drivers)
            val settledAuthority = authority(drivers)
            val reopened = factory(drivers, this).also { runtime = it }
            assertEquals(cache, cacheRows(drivers))
            assertEquals(NetworkMode.OFFLINE, reopened.restoreBackendConfig().networkMode)
            val cached = withTimeout(10_000) { reopened.trails.observeQuery(TrailQuery()).first { it.data != null && !it.loading } }
            assertEquals(worldTrails, cached.data)
            assertEquals(settledAuthority, authority(drivers), "Offline reopen must serve the retained fresh page without a request")
        } finally { runtime?.close(); directory.deleteRecursively() }
    }

    @Test
    fun restartAfterBackendCommitStillClearsOldCatalog() = runBlocking {
        val directory = Files.createTempDirectory("trails-reseed-interrupted").toFile()
        val drivers = PlatformTrailDatabaseDriverFactory(directory)
        var runtime: RealTrailDataFactory? = null
        try {
            prepareLegacyInstall(drivers, this)
            assertFreshQueryMetadata(drivers)
            // Simulate a process exiting after the backend transaction but before catalog setup.
            val backendDriver = drivers.open("trails-m1-backend.db")
            try {
                val database = TrailDataDatabase(backendDriver)
                val backend = PersistentFakeBackend(database)
                assertTrue(backend.reseeded)
                assertEquals(SEED_VERSION, database.trailDataQueries.backendMeta().executeAsOne().seeded)
                backend.close()
            } finally { backendDriver.close() }
            assertEquals(9, cacheRows(drivers).size, "The old query and eight details must still be present before restart")
            val reopened = factory(drivers, this).also { runtime = it }
            assertCacheEmpty(drivers)
            reopened.restoreBackendConfig()
            val loaded = withTimeout(10_000) { reopened.trails.observeQuery(TrailQuery()).first { it.data != null && !it.loading } }
            assertEquals(worldTrails, loaded.data)
        } finally { runtime?.close(); directory.deleteRecursively() }
    }

    @Test
    fun failedReplacementRollsBackCatalogAndSeedVersionTogether() = runBlocking {
        val directory = Files.createTempDirectory("trails-reseed-rollback").toFile()
        val drivers = PlatformTrailDatabaseDriverFactory(directory)
        try {
            prepareLegacyInstall(drivers, this)
            val driver = drivers.open("trails-m1-backend.db")
            try {
                val database = TrailDataDatabase(driver)
                val before = database.trailDataQueries.backendTrails().executeAsList().map { it.id to it.payload.toList() }
                driver.execute(null,
                    "CREATE TRIGGER fail_reseed BEFORE INSERT ON backend_trail WHEN NEW.position = 3 BEGIN SELECT RAISE(ABORT, 'Interrupted seed'); END",
                    0,
                ).value
                assertFails { PersistentFakeBackend(database) }
                assertEquals(1L, database.trailDataQueries.backendMeta().executeAsOne().seeded)
                assertEquals(before, database.trailDataQueries.backendTrails().executeAsList().map { it.id to it.payload.toList() })
            } finally { driver.close() }
        } finally { directory.deleteRecursively() }
    }

    private fun factory(drivers: TrailDatabaseDriverFactory, scope: CoroutineScope) =
        RealTrailDataFactory(drivers, scope, AccountRecoveryPolicy(automatic = false))

    private suspend fun prepareLegacyInstall(
        drivers: TrailDatabaseDriverFactory,
        scope: CoroutineScope,
        prepareSaves: suspend (RealTrailDataFactory) -> Unit = {},
    ) {
        // Start the fixture at the current version so only Store6 can write the eight old rows.
        factory(drivers, scope).close()
        withDatabase(drivers, "trails-m1-backend.db") { _, database ->
            database.transaction {
                database.trailDataQueries.deleteBackendTrails()
                legacyTrails.forEachIndexed { index, trail ->
                    database.trailDataQueries.putBackendTrail(trail.id, Json.encodeToString(Trail.serializer(), trail).encodeToByteArray(), index.toLong())
                }
            }
        }
        val original = factory(drivers, scope)
        try {
            original.applyBackendConfig(online)
            val loaded = withTimeout(10_000) { original.trails.observeQuery(TrailQuery()).first { it.data != null && !it.loading } }
            assertEquals(legacyTrails, loaded.data)
            prepareSaves(original)
        } finally { original.close() }
        withDatabase(drivers, "trails-m1-backend.db") { _, database ->
            database.trailDataQueries.initializeClient("alice", "legacy-installation", "legacy-client")
            database.trailDataQueries.recordRetirement(7, "alice", "legacy-installation", "legacy-client")
            database.trailDataQueries.setSeeded(1)
        }
        withDatabase(drivers, "trails-m1-catalog.db") { driver, _ ->
            // The previous app had no catalog marker; its backend_meta table was unused.
            driver.execute(null, "DELETE FROM backend_meta", 0).value
        }
    }

    private suspend fun assertFreshQueryMetadata(drivers: TrailDatabaseDriverFactory) {
        val driver = drivers.open("trails-m1-catalog.db")
        try {
            val status = assertNotNull(SqlDelightBookkeeper(driver, TrailDataDatabase(driver)).status(queryKey))
            assertNotNull(status.meta, "A raw cache row would be revalidated automatically and would not prove the upgrade")
            assertNotNull(status.lastSuccessSequence)
            assertFalse(status.durablyStale)
        } finally { driver.close() }
    }

    private fun assertCacheEmpty(drivers: TrailDatabaseDriverFactory) {
        assertTrue(cacheRows(drivers).isEmpty(), "Old catalog pages must be removed before any repository can observe them")
        withDatabase(drivers, "trails-m1-catalog.db") { _, database ->
            assertEquals(SEED_VERSION, database.trailDataQueries.backendMeta().executeAsOne().seeded)
        }
    }

    private fun cacheRows(drivers: TrailDatabaseDriverFactory) = withDatabase(drivers, "trails-m1-catalog.db") { driver, _ ->
        rows(driver, "SELECT namespace, canonical_id, hex(payload) FROM cache_row ORDER BY namespace, canonical_id", 3)
    }

    private fun authority(drivers: TrailDatabaseDriverFactory) = withDatabase(drivers, "trails-m1-backend.db") { driver, _ ->
        listOf(
            rows(driver, "SELECT version, config, applications, pushes, requests, rate_minute, rate_count, lose_ack FROM backend_meta", 8),
            rows(driver, "SELECT account, trail_id, hex(payload) FROM backend_saved ORDER BY account, trail_id", 3),
            rows(driver, "SELECT account, installation, idempotency_key, trail_id, version, hex(request), hex(result) FROM backend_receipt ORDER BY account, installation, idempotency_key", 7),
            rows(driver, "SELECT account, installation, client_id, retired_through FROM backend_client ORDER BY account, installation, client_id", 4),
        )
    }

    private fun rows(driver: SqlDriver, statement: String, columns: Int): List<List<String>> =
        driver.executeQuery(null, statement, { cursor ->
            val result = mutableListOf<List<String>>()
            while (cursor.next().value) result += (0 until columns).map { cursor.getString(it).orEmpty() }
            QueryResult.Value(result)
        }, 0).value

    private fun <T> withDatabase(drivers: TrailDatabaseDriverFactory, name: String, block: (SqlDriver, TrailDataDatabase) -> T): T {
        val driver = drivers.open(name)
        return try { block(driver, TrailDataDatabase(driver)) } finally { driver.close() }
    }
}
