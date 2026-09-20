@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class)

package org.mobilenativefoundation.trails.integration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.mobilenativefoundation.store6.mutations.sqldelight.SqlDelightMutationJournalStorage
import org.mobilenativefoundation.trails.integration.db.FixtureDatabase

/** Application command receipts must outlive both the caller and Store6 pending inspection. */
class AdmissionRecoveryTest {
    private val key = MembershipKey("alice", "weekend", "eagle-peak")

    @Test
    fun lostCallerResultReconcilesOriginalAdmissionAfterAllDriversReopen() = runBlocking {
        withAdmissionDisk { directory ->
            val command = SaveCommand("lost-caller-result", key)
            val originalMutationId = AdmissionRecoveryRig(directory).use { rig ->
                assertFalse(rig.services.memberships.get(key).saved)
                rig.services.backend.setOffline(true)

                // The real enqueue has returned successfully. Only delivery to its caller is lost:
                // this does not inject an invalid throw-after-commit into journal storage.
                assertFailsWith<CallerResultLost> {
                    assertIs<EnqueueOutcome.Journaled>(rig.services.enqueue(command))
                    throw CallerResultLost()
                }
                val pending = rig.services.memberships.pendingWrites()
                assertEquals(1, pending.size)
                assertEquals(0L, rig.services.backend.appliedCount())
                // Test oracle only: no returned result or live service reaches the reopened rig.
                pending.single().mutationId
            }

            AdmissionRecoveryRig(directory).use { rig ->
                assertTrue(rig.services.backend.isOffline())
                assertEquals(listOf(originalMutationId), rig.services.memberships.pendingWrites().map { it.mutationId })

                val recovered = assertIs<EnqueueOutcome.Journaled>(rig.services.enqueue(command.copy()))
                assertEquals(command, recovered.command)
                assertEquals(originalMutationId, recovered.mutationId, "The command must recover its original admission")
                assertEquals(listOf(originalMutationId), rig.services.memberships.pendingWrites().map { it.mutationId })
                assertEquals(0L, rig.services.backend.appliedCount())
            }
        }
    }

    @Test
    fun concurrentDuplicateCommandsAdmitExactlyOneDurableMutation() = runBlocking {
        withAdmissionDisk { directory ->
            val command = SaveCommand("concurrent-save", key)
            val mutationId = AdmissionRecoveryRig(directory).use { rig ->
                assertFalse(rig.services.memberships.get(key).saved)
                rig.services.backend.setOffline(true)
                val outcomes = withTimeout(10_000) {
                    coroutineScope {
                        val ready = Channel<Unit>(capacity = 2)
                        val start = CompletableDeferred<Unit>()
                        val calls = List(2) {
                            async(Dispatchers.Default) {
                                ready.send(Unit)
                                start.await()
                                rig.services.enqueue(command.copy())
                            }
                        }
                        repeat(2) { ready.receive() }
                        ready.close()
                        start.complete(Unit)
                        calls.awaitAll()
                    }
                }
                val ids = outcomes.map { outcome ->
                    val admitted = assertIs<EnqueueOutcome.Journaled>(outcome)
                    assertEquals(command, admitted.command)
                    admitted.mutationId
                }
                assertEquals(1, ids.distinct().size, "Concurrent duplicate callers must receive the same mutation ID")
                assertEquals(listOf(ids.first()), rig.services.memberships.pendingWrites().map { it.mutationId })
                assertEquals(0L, rig.services.backend.appliedCount())
                ids.first()
            }

            AdmissionRecoveryRig(directory).use { rig ->
                assertTrue(rig.services.backend.isOffline())
                assertEquals(listOf(mutationId), rig.services.memberships.pendingWrites().map { it.mutationId })
            }
        }
    }

    @Test
    fun changedKeyUnderTheSameCommandIdIsRejectedWithoutReplacingOriginalAdmission() = runBlocking {
        withAdmissionDisk { directory ->
            val original = SaveCommand("immutable-command", key)
            val changed = original.copy(key = key.copy(listId = "lakes"))
            val mutationId = AdmissionRecoveryRig(directory).use { rig ->
                assertFalse(rig.services.memberships.get(key).saved)
                assertFalse(rig.services.memberships.get(changed.key).saved)
                rig.services.backend.setOffline(true)
                assertIs<EnqueueOutcome.Journaled>(rig.services.enqueue(original)).mutationId
            }

            AdmissionRecoveryRig(directory).use { rig ->
                assertTrue(rig.services.backend.isOffline())
                val rejected = assertIs<EnqueueOutcome.Rejected>(rig.services.enqueue(changed))
                assertEquals(changed, rejected.command)
                val pending = rig.services.memberships.pendingWrites()
                assertEquals(listOf(mutationId), pending.map { it.mutationId })
                assertEquals(key.namespace.value, pending.single().namespace)
                assertEquals(key.canonicalId(), pending.single().canonicalId)
                assertFalse(rig.services.committed(changed.key).saved)

                val recovered = assertIs<EnqueueOutcome.Journaled>(rig.services.enqueue(original.copy()))
                assertEquals(mutationId, recovered.mutationId, "Invalid ID reuse must preserve the original receipt")
                assertEquals(listOf(mutationId), rig.services.memberships.pendingWrites().map { it.mutationId })
                assertEquals(0L, rig.services.backend.appliedCount())
            }
        }
    }

    @Test
    fun settledCommandRetainsItsAdmissionIdentityAfterReopenWithNoPendingItem() = runBlocking {
        withAdmissionDisk { directory ->
            val command = SaveCommand("settled-command", key)
            val mutationId = AdmissionRecoveryRig(directory).use { rig ->
                assertFalse(rig.services.memberships.get(key).saved)
                val admitted = assertIs<EnqueueOutcome.Journaled>(rig.services.enqueue(command))
                rig.services.recoverAndDrain()
                assertNull(rig.services.status.value.operationError)
                assertTrue(rig.services.memberships.pendingWrites().isEmpty())
                assertTrue(rig.services.committed(key).saved)
                assertEquals(1L, rig.services.backend.appliedCount())
                rig.services.backend.setOffline(true)
                admitted.mutationId
            }

            AdmissionRecoveryRig(directory).use { rig ->
                assertTrue(rig.services.backend.isOffline())
                assertTrue(rig.services.memberships.pendingWrites().isEmpty())
                assertTrue(rig.services.committed(key).saved)

                val recovered = assertIs<EnqueueOutcome.Journaled>(rig.services.enqueue(command.copy()))
                assertEquals(mutationId, recovered.mutationId, "A settled command needs its retained admission receipt")
                assertTrue(rig.services.memberships.pendingWrites().isEmpty(), "Recovery must not create new pending work")
                assertEquals(1L, rig.services.backend.appliedCount())
            }
        }
    }

    @Test
    fun receiptInsertFailureRollsBackJournalAdmissionAndSameCommandCanRetryAfterReopen() = runBlocking {
        withAdmissionDisk { directory ->
            val command = SaveCommand("receipt-insert-failure", key)
            AdmissionRecoveryRig(directory).use { rig ->
                assertFalse(rig.services.memberships.get(key).saved)
                rig.services.backend.setOffline(true)
                rig.executeJournalSql(
                    """
                        CREATE TRIGGER reject_command_acceptance
                        BEFORE INSERT ON command_acceptance
                        BEGIN
                            SELECT RAISE(ABORT, 'Injected admission receipt insertion failure');
                        END
                    """.trimIndent(),
                )

                val rejected = assertIs<EnqueueOutcome.Rejected>(rig.services.enqueue(command))
                assertEquals(command, rejected.command)
                assertNull(rig.receiptMutationId(command))
                rig.assertNoRetainedMutations()
                assertTrue(rig.services.memberships.pendingWrites().isEmpty())
                assertFalse(rig.services.committed(key).saved)
            }

            AdmissionRecoveryRig(directory).use { rig ->
                assertTrue(rig.services.backend.isOffline())
                assertNull(rig.receiptMutationId(command), "Failed receipt insertion must not survive reopen")
                rig.assertNoRetainedMutations()
                assertTrue(rig.services.memberships.pendingWrites().isEmpty())
                assertTrue(rig.services.memberships.deadLetters().isEmpty())
                assertFalse(rig.services.committed(key).saved)

                // No IF EXISTS: the persistent trigger must still exist on the reopened connection.
                rig.executeJournalSql("DROP TRIGGER reject_command_acceptance")
                val admitted = assertIs<EnqueueOutcome.Journaled>(rig.services.enqueue(command.copy()))
                assertEquals(command, admitted.command)
                assertEquals(admitted.mutationId, rig.receiptMutationId(command))
                assertEquals(listOf(admitted.mutationId), rig.services.memberships.pendingWrites().map { it.mutationId })
                assertEquals(0L, rig.services.backend.appliedCount())
            }
        }
    }

    private suspend fun withAdmissionDisk(block: suspend (File) -> Unit) {
        val directory = Files.createTempDirectory("trails-admission-recovery-").toFile()
        try {
            block(directory)
        } finally {
            check(directory.deleteRecursively())
        }
    }
}

private class CallerResultLost : Exception("The caller did not receive the completed admission result")

/** Every reopen constructs fresh Store6 services and three new file-backed JDBC connections. */
private class AdmissionRecoveryRig(directory: File, account: String = "alice") : AutoCloseable {
    private val drivers = mutableListOf<JdbcSqliteDriver>()

    private fun database(directory: File, name: String): Pair<JdbcSqliteDriver, FixtureDatabase> {
        val file = File(directory, name)
        val create = !file.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { drivers += it }
        if (create) FixtureDatabase.Schema.create(driver).value
        return driver to FixtureDatabase(driver)
    }

    private val values = database(directory, "$account-values.db")
    private val journal = database(directory, "$account-journal.db")
    private val backend = database(directory, "backend.db")
    val services = FixtureServices(
        account, values.first, values.second, journal.first, journal.second, DurableFixtureBackend(backend.second),
    )

    // Test-only SQL runs with no admission or drain in flight on this rig.
    fun executeJournalSql(sql: String) {
        journal.first.execute(identifier = null, sql = sql, parameters = 0).value
    }

    fun receiptMutationId(command: SaveCommand): String? =
        journal.second.commandAcceptanceQueries.acceptance(command.key.account, command.id)
            .executeAsOneOrNull()?.mutation_id

    suspend fun assertNoRetainedMutations() {
        SqlDelightMutationJournalStorage(journal.first, journal.second).transaction { transaction ->
            // The pinned Store6 factory's default client is client-0; use its public journal seam,
            // rather than asserting only the pending projection or reading internal SQL tables.
            assertTrue(transaction.intents("client-0").isEmpty(), "Receipt failure must roll back the inserted intent")
            assertTrue(transaction.executions("client-0").isEmpty(), "Receipt failure must leave no orphan execution")
        }
    }

    override fun close() {
        try {
            services.close()
        } finally {
            drivers.asReversed().forEach { it.close() }
        }
    }
}
