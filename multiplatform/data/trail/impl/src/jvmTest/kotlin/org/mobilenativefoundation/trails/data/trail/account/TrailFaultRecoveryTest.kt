@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class, org.mobilenativefoundation.store6.core.DelicateStoreApi::class)

package org.mobilenativefoundation.trails.data.trail.account

import app.cash.sqldelight.db.SqlDriver
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.mobilenativefoundation.store6.core.seam.WallClock
import org.mobilenativefoundation.store6.mutations.sqldelight.SqlDelightMutationJournalStorage
import org.mobilenativefoundation.trails.data.backend.BackendConfig
import org.mobilenativefoundation.trails.data.backend.NetworkMode
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.data.trail.internal.account.AccountRecoveryPolicy
import org.mobilenativefoundation.trails.data.trail.internal.backend.worldTrails
import org.mobilenativefoundation.trails.data.trail.internal.saved.SavedCodec
import org.mobilenativefoundation.trails.data.trail.internal.saved.SavedValue
import org.mobilenativefoundation.trails.data.trail.saved.SaveOutcome
import org.mobilenativefoundation.trails.data.trail.saved.SetCollectionsCommand
import org.mobilenativefoundation.trails.data.trail.saved.TrailSyncStatus
import org.mobilenativefoundation.trails.data.trail.storage.PlatformTrailDatabaseDriverFactory
import org.mobilenativefoundation.trails.data.trail.storage.TrailDatabaseDriverFactory
import org.mobilenativefoundation.trails.data.trail.storage.db.TrailDataDatabase

/** Faults run against production repositories, schema, server, and file-backed JDBC drivers. */
class TrailFaultRecoveryTest {
    private val online = BackendConfig(latencyRange = 0.milliseconds..0.milliseconds)
    private val offline = online.copy(networkMode = NetworkMode.OFFLINE)
    private val trailId = worldTrails.first().id

    @Test
    fun acceptedSaveWithLostAcknowledgementReplaysSameReceiptAfterRestart() = runBlocking {
        withDisk { directory ->
            val command = SetCollectionsCommand("lost-ack", trailId, setOf("weekend"))
            val first = FaultRig(directory, this)
            val mutationId = try {
                first.prepare(online, offline)
                val admitted = assertIs<SaveOutcome.Journaled>(first.account.saved.save(command))
                first.runtime.loseNextAcknowledgement()
                first.runtime.applyBackendConfig(online)
                first.account.saved.retryPending()
                assertEquals(1L, first.runtime.backendEvidence().applications)
                assertEquals(1L, first.runtime.backendEvidence().receipts)
                assertEquals(setOf("weekend"), first.serverValue(trailId)?.collectionIds)
                assertEquals(emptySet(), first.persisted(trailId)?.collectionIds, "Lost ACK must leave the confirmed local base unchanged")
                assertEquals(1, first.snapshot().syncByTrail.getValue(trailId).pendingCount)
                first.runtime.applyBackendConfig(offline)
                admitted.mutationId
            } finally { first.close() }

            val reopened = FaultRig(directory, this)
            try {
                reopened.restore()
                assertEquals(NetworkMode.OFFLINE, reopened.runtime.backendConfig.value?.networkMode)
                assertEquals(mutationId, assertIs<SaveOutcome.Journaled>(reopened.account.saved.reconcile(command)).mutationId)
                assertEquals(1, reopened.snapshot().syncByTrail.getValue(trailId).pendingCount)
                reopened.runtime.applyBackendConfig(online)
                reopened.account.saved.retryPending()
                reopened.awaitSettled(trailId, command.collectionIds)
                assertEquals(command.collectionIds, reopened.persisted(trailId)?.collectionIds)
                assertEquals(1L, reopened.runtime.backendEvidence().applications)
                assertEquals(1L, reopened.runtime.backendEvidence().receipts)
                assertEquals(2L, reopened.runtime.backendEvidence().pushAttempts)
                assertEquals(mutationId, assertIs<SaveOutcome.Journaled>(reopened.account.saved.save(command)).mutationId)
            } finally { reopened.close() }
        }
    }

    @Test
    fun acknowledgedSaveFinishesLocalAdoptionAfterRestartWhileOfflineWithoutAnotherPush() = runBlocking {
        withDisk { directory ->
            val command = SetCollectionsCommand("adoption", trailId, setOf("weekend", "favorites"))
            val first = FaultRig(directory, this)
            try {
                first.prepare(online, offline)
                assertIs<SaveOutcome.Journaled>(first.account.saved.save(command))
                first.reject("values", "cache_row", "reject_adoption")
                first.runtime.applyBackendConfig(online)
                first.account.saved.retryPending()
                assertEquals(1L, first.runtime.backendEvidence().applications)
                assertEquals(1L, first.runtime.backendEvidence().pushAttempts)
                val pending = first.snapshot().syncByTrail.getValue(trailId)
                assertEquals(TrailSyncStatus.FINISHING, pending.status)
                assertEquals(1, pending.pendingCount)
                assertEquals(emptySet(), first.persisted(trailId)?.collectionIds)
                first.runtime.applyBackendConfig(offline)
            } finally { first.close() }

            val reopened = FaultRig(directory, this)
            try {
                reopened.restore()
                assertEquals(TrailSyncStatus.FINISHING, reopened.snapshot().syncByTrail.getValue(trailId).status)
                reopened.allow("values", "reject_adoption")
                reopened.account.saved.retryPending()
                reopened.awaitSettled(trailId, command.collectionIds)
                assertEquals(NetworkMode.OFFLINE, reopened.runtime.backendConfig.value?.networkMode)
                assertEquals(command.collectionIds, reopened.persisted(trailId)?.collectionIds)
                assertEquals(1L, reopened.runtime.backendEvidence().applications)
                assertEquals(1L, reopened.runtime.backendEvidence().receipts)
                assertEquals(1L, reopened.runtime.backendEvidence().pushAttempts, "A durable ACK must be adopted without sending it again")
            } finally { reopened.close() }
        }
    }

    @Test
    fun commandReceiptFailureRollsBackAdmissionAndSameIdentityCanRetryAfterRestart() = runBlocking {
        withDisk { directory ->
            val command = SetCollectionsCommand("receipt-rollback", trailId, setOf("weekend"))
            val first = FaultRig(directory, this)
            try {
                first.prepare(online, offline)
                first.reject("journal", "command_acceptance", "reject_acceptance")
                assertIs<SaveOutcome.Rejected>(first.account.saved.save(command))
                assertNull(first.receipt(command.id))
                first.assertNoJournalIntents()
                assertEquals(emptySet(), first.persisted(trailId)?.collectionIds)
            } finally { first.close() }

            val reopened = FaultRig(directory, this)
            try {
                reopened.restore()
                assertNull(reopened.receipt(command.id))
                reopened.assertNoJournalIntents()
                reopened.allow("journal", "reject_acceptance")
                val admitted = assertIs<SaveOutcome.Journaled>(reopened.account.saved.save(command))
                assertEquals(admitted.mutationId, reopened.receipt(command.id))
                reopened.awaitMembership(trailId, command.collectionIds)
                assertEquals(0L, reopened.runtime.backendEvidence().applications)
            } finally { reopened.close() }
        }
    }

    @Test
    fun serverReceiptFailureRollsBackCanonicalEffectBeforeSuccessfulRetry() = runBlocking {
        withDisk { directory ->
            val rig = FaultRig(directory, this)
            try {
                rig.prepare(online, offline)
                val command = SetCollectionsCommand("server-rollback", trailId, setOf("favorites"))
                assertIs<SaveOutcome.Journaled>(rig.account.saved.save(command))
                rig.reject("backend", "backend_receipt", "reject_server_receipt")
                rig.runtime.applyBackendConfig(online)
                rig.account.saved.retryPending()
                assertEquals(0L, rig.runtime.backendEvidence().applications)
                assertEquals(0L, rig.runtime.backendEvidence().receipts)
                assertNull(rig.serverValue(trailId), "Canonical write and operation receipt must roll back together")
                assertEquals(1, rig.snapshot().syncByTrail.getValue(trailId).pendingCount)
                rig.allow("backend", "reject_server_receipt")
                rig.account.saved.retryPending()
                rig.awaitSettled(trailId, command.collectionIds)
                assertEquals(command.collectionIds, rig.serverValue(trailId)?.collectionIds)
                assertEquals(1L, rig.runtime.backendEvidence().applications)
                assertEquals(1L, rig.runtime.backendEvidence().receipts)
            } finally { rig.close() }
        }
    }

    @Test
    fun saveUnsaveSaveRetainsLatestWholeSetDuringOrderedReplayAfterRestart() = runBlocking {
        withDisk { directory ->
            val desired = setOf("favorites")
            val first = FaultRig(directory, this)
            try {
                first.prepare(online, offline)
                listOf(setOf("weekend"), emptySet(), desired).forEachIndexed { index, set ->
                    assertIs<SaveOutcome.Journaled>(first.account.saved.save(SetCollectionsCommand("order-$index", trailId, set)))
                }
                first.awaitMembership(trailId, desired)
                assertEquals(0L, first.runtime.backendEvidence().applications)
            } finally { first.close() }

            val reopened = FaultRig(directory, this)
            val projections = mutableListOf<Set<String>>()
            var observer: Job? = null
            try {
                reopened.restore()
                reopened.awaitMembership(trailId, desired)
                observer = launch(start = CoroutineStart.UNDISPATCHED) {
                    reopened.account.saved.state.mapNotNull { it.data?.memberships?.get(trailId) }.collect { projections += it }
                }
                reopened.runtime.applyBackendConfig(online)
                reopened.account.saved.retryPending()
                reopened.awaitSettled(trailId, desired)
                assertTrue(projections.isNotEmpty())
                assertTrue(projections.all { it == desired }, "Earlier acknowledgements must not regress the submitted whole set: $projections")
                assertEquals(desired, reopened.serverValue(trailId)?.collectionIds)
                assertEquals(desired, reopened.persisted(trailId)?.collectionIds)
                assertEquals(3L, reopened.runtime.backendEvidence().applications)
                assertEquals(3L, reopened.runtime.backendEvidence().receipts)
            } finally { observer?.cancelAndJoin(); reopened.close() }
        }
    }

    @Test
    fun automaticRecoveryContinuesBeyondEightFailuresAndPersistedFiveMinuteBackoff() = runBlocking {
        withDisk { directory ->
            val clock = RecoveryClock()
            val waiting = Channel<Unit>(Channel.UNLIMITED)
            val ticks = Channel<Unit>(Channel.UNLIMITED)
            val policy = AccountRecoveryPolicy(clock = clock, awaitRetry = {
                waiting.send(Unit)
                ticks.receive()
            })
            val rig = FaultRig(directory, this, policy)
            try {
                rig.prepare(online, offline)
                assertIs<SaveOutcome.Journaled>(rig.account.saved.save(SetCollectionsCommand("long-recovery", trailId, setOf("weekend"))))
                rig.runtime.applyBackendConfig(online.copy(errorRate = 1f))
                // Every continuation is controlled by a durable-clock advance and an explicit
                // scheduler tick. No real-time sleep or keyed manual retry bypasses eligibility.
                repeat(10) { index ->
                    withTimeout(10_000) { waiting.receive() }
                    assertEquals((index + 1).toLong(), rig.runtime.backendEvidence().pushAttempts)
                    assertEquals(0L, rig.runtime.backendEvidence().applications)
                    clock.advancePastMaximumBackoff()
                    if (index < 9) ticks.send(Unit)
                }
                rig.runtime.applyBackendConfig(online)
                ticks.send(Unit)
                rig.awaitSettled(trailId, setOf("weekend"))
                assertEquals(1L, rig.runtime.backendEvidence().applications)
                assertEquals(1L, rig.runtime.backendEvidence().receipts)
                assertEquals(11L, rig.runtime.backendEvidence().pushAttempts)
            } finally { rig.close(); waiting.close(); ticks.close() }
        }
    }

    @Test
    fun closingAccountCancelsRetryReceiverBeforeClosingItsChannel() = runBlocking {
        withDisk { directory ->
            val entered = CompletableDeferred<Unit>()
            val receiveEnded = CountDownLatch(1)
            val receiveFailure = AtomicReference<Throwable?>()
            val uncaught = AtomicReference<Throwable?>()
            val policy = AccountRecoveryPolicy(awaitRetry = { wake ->
                // Make an early channel close observable: account.close cannot continue until
                // the receiver has observed its result on another thread.
                (wake as Channel<Unit>).invokeOnClose {
                    check(receiveEnded.await(5, TimeUnit.SECONDS)) { "The retry receiver did not finish" }
                }
                entered.complete(Unit)
                // Save/config signals may already be buffered or arrive concurrently. Keep this
                // injected receiver alive through them; only retirement may end this test wait.
                try { while (true) wake.receive() }
                catch (failure: Throwable) {
                    receiveFailure.set(failure)
                    receiveEnded.countDown()
                    throw failure
                }
            })
            val parent = CoroutineScope(coroutineContext + Dispatchers.Default + CoroutineExceptionHandler { _, failure -> uncaught.set(failure) })
            val rig = FaultRig(directory, parent, policy)
            try {
                rig.prepare(online, offline)
                assertIs<SaveOutcome.Journaled>(rig.account.saved.save(SetCollectionsCommand("close-retry", trailId, setOf("weekend"))))
                rig.runtime.applyBackendConfig(online.copy(errorRate = 1f))
                withTimeout(10_000) { entered.await() }
                withTimeout(10_000) { async(Dispatchers.Default) { rig.account.close() }.await() }
                assertIs<CancellationException>(receiveFailure.get(), "The receiver must observe account cancellation, not a closed-channel failure")
                assertNull(uncaught.get(), "Account retirement must not throw into the application exception handler")
            } finally { receiveEnded.countDown(); rig.close() }
        }
    }

    private suspend fun withDisk(block: suspend (File) -> Unit) {
        val directory = Files.createTempDirectory("trails-production-fault-").toFile()
        try { block(directory) } finally { check(directory.deleteRecursively()) }
    }
}

private class RecoveryClock : WallClock {
    @Volatile private var now = 1_800_000_000_000L
    override fun nowEpochMillis() = now
    fun advancePastMaximumBackoff() { now += 300_001 }
}

private class FaultRig(
    directory: File,
    parent: CoroutineScope,
    policy: AccountRecoveryPolicy = AccountRecoveryPolicy(automatic = false),
) {
    private val drivers = mutableMapOf<String, SqlDriver>()
    private val platform = PlatformTrailDatabaseDriverFactory(directory)
    val runtime = RealTrailDataFactory(object : TrailDatabaseDriverFactory {
        override fun open(name: String) = platform.open(name).also { drivers[name] = it }
    }, parent, policy)
    lateinit var account: TrailAccount
    private fun driver(partition: String) = drivers.entries.single { (name, _) -> name.endsWith("-$partition.db") }.value
    private fun database(partition: String) = TrailDataDatabase(driver(partition))

    suspend fun prepare(online: BackendConfig, offline: BackendConfig) {
        runtime.applyBackendConfig(online)
        withTimeout(10_000) { runtime.trails.observeQuery(TrailQuery()).first { it.data != null } }
        runtime.applyBackendConfig(offline)
        account = runtime.open("alice")
    }
    suspend fun restore() { runtime.restoreBackendConfig(); account = runtime.open("alice") }
    fun snapshot() = requireNotNull(account.saved.state.value.data)
    suspend fun awaitMembership(id: String, set: Set<String>) {
        withTimeout(10_000) { account.saved.state.first { it.data?.memberships?.get(id) == set } }
    }
    suspend fun awaitSettled(id: String, set: Set<String>) {
        withTimeout(10_000) { account.saved.state.first { it.data?.let { saved -> saved.memberships[id] == set && saved.syncByTrail[id]?.status == TrailSyncStatus.SYNCED } == true } }
    }
    fun persisted(id: String): SavedValue? = database("values").trailDataQueries.readCache("saved-alice", id).executeAsOneOrNull()?.let { SavedCodec.decode(1, it.payload) }
    fun serverValue(id: String): SavedValue? = database("backend").trailDataQueries.readBackendSaved("alice", id).executeAsOneOrNull()?.let { SavedCodec.decode(1, it.payload) }
    fun receipt(id: String) = database("journal").trailDataQueries.acceptance("alice", id).executeAsOneOrNull()?.mutation_id
    suspend fun assertNoJournalIntents() {
        SqlDelightMutationJournalStorage(driver("journal"), database("journal")).transaction { tx ->
            // The Store6 factory defaults to client-0. Use its public transaction seam.
            assertTrue(tx.intents("client-0").isEmpty())
            assertTrue(tx.executions("client-0").isEmpty())
        }
    }
    fun reject(partition: String, table: String, trigger: String) {
        // Persistent trigger: JDBC borrows multiple connections; TEMP triggers would not inject
        // the fault on Store6's dispatcher and would disappear when the borrowed connection closes.
        driver(partition).execute(null, "CREATE TRIGGER $trigger BEFORE INSERT ON $table BEGIN SELECT RAISE(ABORT, 'Injected $trigger'); END", 0).value
    }
    fun allow(partition: String, trigger: String) { driver(partition).execute(null, "DROP TRIGGER $trigger", 0).value }
    suspend fun close() { runtime.close() }
}
