package org.mobilenativefoundation.trails.data.trail.account

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.mobilenativefoundation.trails.data.backend.BackendConfig
import org.mobilenativefoundation.trails.data.backend.NetworkMode
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.data.trail.saved.SaveOutcome
import org.mobilenativefoundation.trails.data.trail.saved.SetCollectionsCommand
import org.mobilenativefoundation.trails.data.trail.saved.TrailSyncStatus
import org.mobilenativefoundation.trails.data.trail.storage.PlatformTrailDatabaseDriverFactory
import org.mobilenativefoundation.trails.data.trail.storage.TrailDatabaseDriverFactory

class TrailPersistenceTest {
    private val online = BackendConfig(latencyRange = 0.milliseconds..0.milliseconds)

    @Test
    fun offlineColdQueryIsUnavailableAndWarmQuerySurvivesReopen() = runBlocking {
        val directory = Files.createTempDirectory("trails-query").toFile()
        try {
            val first = RealTrailDataFactory(PlatformTrailDatabaseDriverFactory(directory), this)
            first.applyBackendConfig(online.copy(networkMode = NetworkMode.OFFLINE))
            val unavailable = withTimeout(10_000) { first.trails.observeQuery(TrailQuery()).first { it.error != null } }
            assertEquals(null, unavailable.data)
            first.applyBackendConfig(online)
            first.trails.refreshQuery(TrailQuery())
            val loaded = withTimeout(10_000) { first.trails.observeQuery(TrailQuery()).first { it.data != null } }
            assertTrue(loaded.data!!.isNotEmpty())
            first.applyBackendConfig(online.copy(networkMode = NetworkMode.OFFLINE))
            first.close()
            val reopened = RealTrailDataFactory(PlatformTrailDatabaseDriverFactory(directory), this)
            assertEquals(NetworkMode.OFFLINE, reopened.restoreBackendConfig().networkMode)
            assertEquals(loaded.data, withTimeout(10_000) { reopened.trails.observeQuery(TrailQuery()).first { it.data != null } }.data)
            reopened.close()
        } finally { directory.deleteRecursively() }
    }

    @Test
    fun wholeCollectionSetAndReceiptSurviveOfflineRestartAndReconnect() = runBlocking {
        val directory = Files.createTempDirectory("trails-saved").toFile()
        try {
            val first = RealTrailDataFactory(PlatformTrailDatabaseDriverFactory(directory), this)
            first.applyBackendConfig(online)
            val trail = withTimeout(10_000) { first.trails.observeQuery(TrailQuery()).first { it.data != null } }.data!!.first()
            val account = first.open("alice")
            first.applyBackendConfig(online.copy(networkMode = NetworkMode.OFFLINE))
            val command = SetCollectionsCommand("two-lists", trail.id, setOf("weekend", "favorites"))
            val admission = assertIs<SaveOutcome.Journaled>(account.saved.save(command))
            val pending = withTimeout(10_000) { account.saved.state.first { it.data?.memberships?.get(trail.id) == command.collectionIds } }
            assertNotNull(pending.data!!.syncByTrail[trail.id])
            first.close()
            val reopened = RealTrailDataFactory(PlatformTrailDatabaseDriverFactory(directory), this)
            reopened.restoreBackendConfig()
            val restored = reopened.open("alice")
            assertEquals(admission.mutationId, assertIs<SaveOutcome.Journaled>(restored.saved.save(command)).mutationId)
            withTimeout(10_000) { restored.saved.state.first { it.data?.memberships?.get(trail.id) == command.collectionIds } }
            assertEquals(0L, reopened.backendEvidence().applications)
            reopened.applyBackendConfig(online)
            restored.saved.retryPending()
            withTimeout(10_000) { restored.saved.state.first { it.data?.syncByTrail?.values?.none { status -> status.status != TrailSyncStatus.SYNCED } == true } }
            assertEquals(1L, reopened.backendEvidence().applications)
            assertEquals(1L, reopened.backendEvidence().receipts)
            val removal = assertIs<SaveOutcome.Journaled>(restored.saved.save(SetCollectionsCommand("one-list", trail.id, setOf("favorites"))))
            assertFalse(removal.mutationId == admission.mutationId)
            withTimeout(10_000) { restored.saved.state.first { it.data?.memberships?.get(trail.id) == setOf("favorites") } }
            restored.close()
            val bob = reopened.open("bob")
            assertTrue(bob.saved.state.value.data!!.memberships.values.all { it.isEmpty() })
            reopened.close()
        } finally { directory.deleteRecursively() }
    }

    @Test
    fun normalizedSearchAndFiltersCompose() {
        assertEquals("alpine lake", TrailQuery(text = "  ALPINE   Lake  ").normalized().text)
        assertEquals(TrailQuery(text = "alpine lake"), TrailQuery(text = "  ALPINE   Lake  ").normalized())
    }

    @Test
    fun accountCloseJoinsInProgressAdmissionBeforeClosingDrivers() = runBlocking {
        val directory = Files.createTempDirectory("trails-retirement").toFile()
        val blocker = AdmissionBlocker(PlatformTrailDatabaseDriverFactory(directory))
        val appScope = CoroutineScope(coroutineContext + Dispatchers.Default)
        val runtime = RealTrailDataFactory(blocker, appScope)
        try {
            runtime.applyBackendConfig(online)
            val trail = withTimeout(10_000) { runtime.trails.observeQuery(TrailQuery()).first { it.data != null } }.data!!.first()
            val account = runtime.open("alice")
            runtime.applyBackendConfig(online.copy(networkMode = NetworkMode.OFFLINE))
            blocker.block = true
            val command = SetCollectionsCommand("retiring-admission", trail.id, setOf("weekend"))
            val save = async { account.saved.save(command) }
            withTimeout(10_000) { blocker.entered.await() }
            val close = async(start = CoroutineStart.UNDISPATCHED) { account.close() }
            assertFalse(close.isCompleted, "Retirement must wait for the synchronous admission transaction")
            assertFalse(blocker.journalClosed, "The journal driver must remain open until the admission job joins")
            blocker.release.countDown()
            withTimeout(10_000) { close.await() }
            assertFailsWith<CancellationException> { save.await() }
            assertTrue(blocker.journalClosed)
            runtime.close()
            val reopened = RealTrailDataFactory(PlatformTrailDatabaseDriverFactory(directory), this)
            reopened.restoreBackendConfig()
            val restored = reopened.open("alice")
            assertIs<SaveOutcome.Journaled>(restored.saved.reconcile(command))
            withTimeout(10_000) { restored.saved.state.first { it.data?.memberships?.get(trail.id) == setOf("weekend") } }
            assertEquals(0L, reopened.backendEvidence().applications)
            reopened.close()
        } finally {
            blocker.release.countDown()
            runtime.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun lateEarlierQueryCannotChangeAnotherQueriesResults() = runBlocking {
        val directory = Files.createTempDirectory("trails-queries").toFile()
        val runtime = RealTrailDataFactory(PlatformTrailDatabaseDriverFactory(directory), this)
        val observers = CoroutineScope(coroutineContext + SupervisorJob(coroutineContext[Job]))
        try {
            runtime.applyBackendConfig(online.copy(latencyRange = 300.milliseconds..300.milliseconds))
            val old = async { runtime.trails.observeQuery(TrailQuery(text = "lake")).first { it.data != null } }
            withTimeout(10_000) { while (runtime.backendEvidence().requests == 0L) yield() }
            runtime.applyBackendConfig(online)
            val current = runtime.trails.observeQuery(TrailQuery(text = "half dome")).stateIn(observers, SharingStarted.Eagerly, LoadState())
            val halfDome = withTimeout(10_000) { current.first { it.data != null } }
            assertEquals(listOf("half-dome"), halfDome.data!!.map { it.id })
            val lakes = withTimeout(10_000) { old.await() }
            assertTrue(lakes.data!!.all { "${it.name} ${it.region}".contains("lake", ignoreCase = true) })
            assertEquals(halfDome.data, current.value.data)
        } finally { observers.cancel(); runtime.close(); directory.deleteRecursively() }
    }
}

private class AdmissionBlocker(private val delegate: TrailDatabaseDriverFactory) : TrailDatabaseDriverFactory {
    val entered = CompletableDeferred<Unit>()
    val release = CountDownLatch(1)
    @Volatile var block = false
    @Volatile var journalClosed = false
    override fun open(name: String): SqlDriver {
        val driver = delegate.open(name)
        if (!name.endsWith("-journal.db")) return driver
        return object : SqlDriver by driver {
            override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<Long> {
                if (block && sql.contains("INSERT INTO command_acceptance")) {
                    entered.complete(Unit)
                    check(release.await(10, TimeUnit.SECONDS)) { "Test did not release the admission transaction" }
                }
                return driver.execute(identifier, sql, parameters, binders)
            }
            override fun close() { journalClosed = true; driver.close() }
        }
    }
}
