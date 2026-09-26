@file:OptIn(
    org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class,
    org.mobilenativefoundation.store6.core.DelicateStoreApi::class,
)

package org.mobilenativefoundation.trails.integration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.mattramotar.atom.runtime.Admission
import dev.mattramotar.atom.runtime.state.InMemoryStateHandle
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.mobilenativefoundation.store6.core.Freshness
import org.mobilenativefoundation.store6.core.StoreResult
import org.mobilenativefoundation.store6.core.seam.WallClock
import org.mobilenativefoundation.store6.mutations.MutationPendingState
import org.mobilenativefoundation.trails.integration.db.FixtureDatabase

class Store6PersistenceTest {
    private val key = MembershipKey("alice", "weekend", "eagle-peak")

    @Test
    fun offlineAtomSaveSurvivesAllDriversClosingAndThenSettles() = runBlocking {
        withDisk { directory ->
            val clock = TestClock()
            val mutationId = Rig(directory, clock = clock).use { rig ->
                assertFalse(rig.services.memberships.get(key).saved)
                rig.services.backend.setOffline(true)
                val atom = SaveAtom(this, InMemoryStateHandle<SaveState>(SaveState.Idle), rig.services)
                atom.onStart()
                try {
                    assertIs<Admission.Accepted>(atom.intent(SaveIntent.Save(SaveCommand("offline-save", key))))
                    val journaled = withTimeout(10_000) { atom.state.first { it is SaveState.Journaled } } as SaveState.Journaled
                    assertEquals(listOf(journaled.mutationId), rig.services.memberships.pendingWrites().map { it.mutationId })
                    rig.services.recoverAndDrain()
                    assertEquals(0L, rig.services.backend.appliedCount())
                    journaled.mutationId
                } finally {
                    atom.close()
                    atom.awaitDisposed()
                }
            }
            Rig(directory, clock = clock).use { rig ->
                assertTrue(rig.services.backend.isOffline())
                assertEquals(listOf(mutationId), rig.services.memberships.pendingWrites().map { it.mutationId })
                val restored = withTimeout(10_000) {
                    rig.services.memberships.stream(key, Freshness.LocalOnly)
                        .filterIsInstance<StoreResult.Data<Membership>>().first { it.value.saved }
                }
                assertTrue(restored.value.saved)
                assertFalse(rig.services.committed(key).saved)
                rig.services.backend.setOffline(false)
                // Global drain respects durable retry timing. Advance beyond the maximum jitter.
                clock.advancePastBackoff()
                rig.services.recoverAndDrain()
                assertTrue(rig.services.committed(key).saved, rig.services.diagnostics())
                assertTrue(rig.services.memberships.pendingWrites().isEmpty(), rig.services.diagnostics())
                assertEquals(1L, rig.services.backend.appliedCount())
                assertEquals(2L, rig.services.backend.pushAttempts())
            }
            Rig(directory, clock = clock).use { rig ->
                assertTrue(rig.services.committed(key).saved)
                assertEquals(1L, rig.services.backend.appliedCount())
                assertTrue(rig.services.memberships.pendingWrites().isEmpty())
            }
        }
    }

    @Test
    fun serverReceiptPreventsDuplicateApplicationAfterResponseLossAndReopen() = runBlocking {
        withDisk { directory ->
            val clock = TestClock()
            val installationId = Rig(directory, clock = clock).use { rig ->
                rig.services.memberships.get(key)
                assertIs<EnqueueOutcome.Journaled>(rig.services.enqueue(SaveCommand("lost-ack", key)))
                rig.services.backend.loseNextAcknowledgement()
                rig.services.recoverAndDrain()
                assertEquals(1L, rig.services.backend.appliedCount())
                assertEquals(1, rig.services.memberships.pendingWrites().size)
                assertEquals(1L, rig.services.backend.pushAttempts())
                rig.services.journalInstallationId
            }
            Rig(directory, clock = clock).use { rig ->
                assertEquals(installationId, rig.services.journalInstallationId)
                clock.advancePastBackoff()
                rig.services.recoverAndDrain()
                assertEquals(1L, rig.services.backend.appliedCount())
                assertEquals(2L, rig.services.backend.pushAttempts())
                assertTrue(rig.services.memberships.pendingWrites().isEmpty(), rig.services.diagnostics())
                assertTrue(rig.services.committed(key).saved)
            }
        }
    }

    @Test
    fun accountJournalAndMetroConstructionAreIsolated() = runBlocking {
        withDisk { directory ->
            Rig(directory).use { rig ->
                rig.services.memberships.get(key)
                rig.services.backend.setOffline(true)
                assertIs<EnqueueOutcome.Journaled>(rig.services.enqueue(SaveCommand("account-save", key)))
                assertIs<FixturePresenter>(fixtureGraph(rig.services).presenter)
            }
            Rig(directory, account = "bob").use { rig ->
                assertTrue(rig.services.memberships.pendingWrites().isEmpty())
            }
            Rig(directory).use { rig ->
                assertEquals(1, rig.services.memberships.pendingWrites().size)
            }
        }
    }

    @Test
    fun firstMutationsFromDifferentAccountsDoNotShareBackendReceipts() = runBlocking {
        withDisk { directory ->
            val bobKey = key.copy(account = "bob")
            Rig(directory).use { rig ->
                assertFalse(rig.services.memberships.get(key).saved)
                assertIs<EnqueueOutcome.Journaled>(rig.services.enqueue(SaveCommand("alice-first-save", key)))
                rig.services.recoverAndDrain()
                assertEquals(null, rig.services.status.value.operationError)
                assertTrue(rig.services.memberships.pendingWrites().isEmpty(), rig.services.diagnostics())
                assertEquals(1L, rig.services.backend.appliedCount())
            }
            Rig(directory, account = "bob").use { rig ->
                assertFalse(rig.services.memberships.get(bobKey).saved)
                assertIs<EnqueueOutcome.Journaled>(rig.services.enqueue(SaveCommand("bob-first-save", bobKey)))
                rig.services.recoverAndDrain()
                assertEquals(null, rig.services.status.value.operationError, "Bob's first save must have its own server receipt")
                assertTrue(rig.services.memberships.pendingWrites().isEmpty(), rig.services.diagnostics())
                assertTrue(rig.services.committed(bobKey).saved)
                assertEquals(2L, rig.services.backend.appliedCount())
            }
            Rig(directory).use { rig ->
                assertTrue(rig.services.committed(key).saved)
                assertTrue(rig.services.memberships.pendingWrites().isEmpty())
                assertEquals(2L, rig.services.backend.appliedCount())
            }
            Rig(directory, account = "bob").use { rig ->
                assertTrue(rig.services.committed(bobKey).saved)
                assertTrue(rig.services.memberships.pendingWrites().isEmpty())
                assertEquals(2L, rig.services.backend.appliedCount())
            }
        }
    }

    @Test
    fun separateInstallationsOfOneAccountDoNotShareBackendReceipts() = runBlocking {
        withDisk { directory ->
            val secondKey = key.copy(trailId = "pine-ridge")
            val firstInstallation = Rig(directory).use { rig ->
                rig.services.memberships.get(key)
                assertIs<EnqueueOutcome.Journaled>(rig.services.enqueue(SaveCommand("first-installation-save", key)))
                rig.services.recoverAndDrain()
                assertTrue(rig.services.memberships.pendingWrites().isEmpty(), rig.services.diagnostics())
                rig.services.journalInstallationId
            }
            val secondInstallation = Rig(directory, installation = "second").use { rig ->
                assertNotEquals(firstInstallation, rig.services.journalInstallationId)
                assertFalse(rig.services.memberships.get(secondKey).saved)
                assertIs<EnqueueOutcome.Journaled>(rig.services.enqueue(SaveCommand("second-installation-save", secondKey)))
                rig.services.recoverAndDrain()
                assertTrue(rig.services.memberships.pendingWrites().isEmpty(), rig.services.diagnostics())
                assertTrue(rig.services.committed(secondKey).saved)
                assertEquals(2L, rig.services.backend.appliedCount())
                assertEquals(2L, rig.services.backend.pushAttempts())
                rig.services.journalInstallationId
            }
            Rig(directory).use { rig ->
                assertEquals(firstInstallation, rig.services.journalInstallationId)
                assertTrue(rig.services.committed(key).saved)
                assertTrue(rig.services.memberships.pendingWrites().isEmpty(), rig.services.diagnostics())
            }
            Rig(directory, installation = "second").use { rig ->
                assertEquals(secondInstallation, rig.services.journalInstallationId)
                assertTrue(rig.services.committed(secondKey).saved)
                assertTrue(rig.services.memberships.pendingWrites().isEmpty(), rig.services.diagnostics())
                assertEquals(2L, rig.services.backend.appliedCount())
            }
        }
    }

    @Test
    fun acknowledgedValueAdoptsAfterReopenWhileBackendRemainsOffline() = runBlocking {
        withDisk { directory ->
            val mutationId = Rig(directory).use { rig ->
                assertFalse(rig.services.memberships.get(key).saved)
                rig.rejectValueWrites()
                val admitted = assertIs<EnqueueOutcome.Journaled>(rig.services.enqueue(SaveCommand("adoption-failure", key)))
                rig.services.recoverAndDrain()
                assertEquals(1L, rig.services.backend.appliedCount())
                assertEquals(1L, rig.services.backend.pushAttempts())
                val pendingRows = rig.services.memberships.pendingWrites()
                assertEquals(1, pendingRows.size, rig.services.diagnostics())
                val pending = pendingRows.single()
                assertEquals(admitted.mutationId, pending.mutationId)
                assertEquals(MutationPendingState.ADOPTING, pending.state)
                assertTrue(rig.services.diagnostics().contains("ADOPTION:adoption-failed"), rig.services.diagnostics())
                assertEquals(false, rig.persisted(key)?.saved, "The value transaction must roll back despite the durable ACK")
                admitted.mutationId
            }
            // Repair the persisted fault after reopening. The durable ACK needs no new push.
            Rig(directory).use { rig ->
                rig.services.backend.setOffline(true)
                val pending = rig.services.memberships.pendingWrites().single()
                assertEquals(mutationId, pending.mutationId)
                assertEquals(MutationPendingState.ADOPTING, pending.state)
                assertEquals(false, rig.persisted(key)?.saved)
                rig.allowValueWrites()
                rig.services.recoverAndDrain()
                assertTrue(rig.services.backend.isOffline())
                assertTrue(rig.services.committed(key).saved)
                assertEquals(true, rig.persisted(key)?.saved)
                assertTrue(rig.services.memberships.pendingWrites().isEmpty(), rig.services.diagnostics())
                assertEquals(1L, rig.services.backend.appliedCount())
                assertEquals(1L, rig.services.backend.pushAttempts(), "ACKED recovery must not attempt another push, even while Offline")
                // Complete any server retirement checkpoint that was unavailable while Offline.
                rig.services.reconnect()
                assertTrue(rig.services.memberships.pendingWrites().isEmpty())
                assertEquals(1L, rig.services.backend.appliedCount())
                assertEquals(1L, rig.services.backend.pushAttempts())
            }
            Rig(directory).use { rig ->
                assertTrue(rig.services.committed(key).saved)
                assertTrue(rig.services.memberships.pendingWrites().isEmpty())
                assertEquals(1L, rig.services.backend.appliedCount())
            }
        }
    }

    @Test
    fun inspectionFailureDoesNotRelabelDurablyAdmittedCommand() = runBlocking {
        withDisk { directory ->
            val command = SaveCommand("inspection-failure", key)
            val admitted = Rig(directory).use { rig ->
                val outcome = assertIs<EnqueueOutcome.Journaled>(rig.services.enqueue(command))
                rig.services.close()
                rig.services.inspect()
                assertTrue(rig.services.status.value.inspectionError != null)
                assertEquals(command, outcome.command)
                outcome
            }
            Rig(directory).use { rig ->
                assertEquals(listOf(admitted.mutationId), rig.services.memberships.pendingWrites().map { it.mutationId })
            }
        }
    }

    @Test
    fun localAdmissionExceptionPreservesUncertainCommandWithoutAutomaticRetry() = runBlocking {
        val command = SaveCommand("uncertain-admission", key)
        var calls = 0
        val atom = SaveAtom(this, InMemoryStateHandle<SaveState>(SaveState.Idle), SaveCommandSink {
            calls++
            throw IllegalStateException("The local transaction return was lost")
        })
        atom.onStart()
        try {
            assertIs<Admission.Accepted>(atom.intent(SaveIntent.Save(command)))
            val uncertain = withTimeout(10_000) { atom.state.first { it is SaveState.Uncertain } } as SaveState.Uncertain
            assertEquals(command, uncertain.command)
            val additional = assertIs<Admission.Accepted>(atom.intent(SaveIntent.Save(SaveCommand("second-command", key))))
            withTimeout(10_000) { atom.status.first { it.lastReducedSequence >= additional.sequence } }
            assertEquals(1, calls)
            assertEquals(uncertain, atom.state.value)
        } finally {
            atom.close()
            atom.awaitDisposed()
        }
    }

    private suspend fun CoroutineScope.withDisk(block: suspend (File) -> Unit) {
        val directory = Files.createTempDirectory("trails-consumer-").toFile()
        try { block(directory) } finally { check(directory.deleteRecursively()) }
    }
}

private class TestClock : WallClock {
    private var millis = 1_800_000_000_000L
    override fun nowEpochMillis() = millis
    fun advancePastBackoff() { millis += 300_001L }
}

private class Rig(
    private val directory: File,
    account: String = "alice",
    installation: String = "primary",
    clock: WallClock? = null,
) : AutoCloseable {
    private val drivers = mutableListOf<JdbcSqliteDriver>()
    private fun database(name: String): Pair<JdbcSqliteDriver, FixtureDatabase> {
        val file = File(directory, name)
        val create = !file.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { drivers += it }
        if (create) FixtureDatabase.Schema.create(driver).value
        return driver to FixtureDatabase(driver)
    }
    private val values = database("$account-$installation-values.db")
    private val journal = database("$account-$installation-journal.db")
    private val backend = database("backend.db")
    val services = FixtureServices(
        account, values.first, values.second, journal.first, journal.second, DurableFixtureBackend(backend.second),
        clock = clock,
    )

    fun rejectValueWrites() {
        values.first.execute(
            identifier = null,
            sql = """
                CREATE TRIGGER reject_value_adoption
                BEFORE INSERT ON value_row
                BEGIN
                    SELECT RAISE(ABORT, 'Injected source-of-truth adoption failure');
                END
            """.trimIndent(),
            parameters = 0,
        ).value
    }

    fun allowValueWrites() {
        values.first.execute(null, "DROP TRIGGER reject_value_adoption", 0).value
    }

    fun persisted(key: MembershipKey): Membership? =
        values.second.valueRowsQueries.selectRow(key.namespace.value, key.canonicalId()) { _, _, payload ->
            MembershipCodec.decode(1, payload)
        }.executeAsOneOrNull()

    override fun close() {
        services.close()
        drivers.forEach { it.close() }
    }
}
