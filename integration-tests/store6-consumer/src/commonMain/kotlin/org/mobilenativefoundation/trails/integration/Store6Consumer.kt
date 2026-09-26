@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class, kotlin.uuid.ExperimentalUuidApi::class)

package org.mobilenativefoundation.trails.integration

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid
import org.mobilenativefoundation.store6.core.Freshness
import org.mobilenativefoundation.store6.core.StoreKey
import org.mobilenativefoundation.store6.core.StoreNamespace
import org.mobilenativefoundation.store6.core.seam.WallClock
import org.mobilenativefoundation.store6.mutations.MutationAck
import org.mobilenativefoundation.store6.mutations.MutationCodec
import org.mobilenativefoundation.store6.mutations.MutationKeyResolver
import org.mobilenativefoundation.store6.mutations.MutationPresence
import org.mobilenativefoundation.store6.mutations.MutationPresentAck
import org.mobilenativefoundation.store6.mutations.MutationPush
import org.mobilenativefoundation.store6.mutations.MutationRetirement
import org.mobilenativefoundation.store6.mutations.MutationRetirementAck
import org.mobilenativefoundation.store6.mutations.MutationServer
import org.mobilenativefoundation.store6.mutations.MutatorRef
import org.mobilenativefoundation.store6.mutations.StaleSet
import org.mobilenativefoundation.store6.mutations.mutationStore
import org.mobilenativefoundation.store6.mutations.mutatorRegistry
import org.mobilenativefoundation.store6.mutations.sqldelight.SqlDelightMutationJournalStorage
import org.mobilenativefoundation.store6.sqldelight.SqlDelightBookkeeper
import org.mobilenativefoundation.store6.sqldelight.SqlDelightSourceOfTruth
import org.mobilenativefoundation.trails.integration.db.FixtureDatabase

@Serializable
data class Membership(val trailId: String, val listId: String, val saved: Boolean)

data class MembershipKey(val account: String, val listId: String, val trailId: String) : StoreKey {
    init {
        require(listOf(account, listId, trailId).all { it.matches(Regex("[a-z0-9-]+")) })
    }
    override val namespace = StoreNamespace("saved-$account")
    override fun canonicalId() = "$listId:$trailId"
}

object MembershipCodec : MutationCodec<Membership> {
    override fun encode(value: Membership) = Json.encodeToString(Membership.serializer(), value).encodeToByteArray()
    override fun decode(version: Int, bytes: ByteArray): Membership {
        require(version == 1) { "Unsupported membership payload version $version" }
        return Json.decodeFromString(Membership.serializer(), bytes.decodeToString())
    }
}

class BackendOffline : Exception("The fake backend Offline setting is applied")
class AcknowledgementLost : Exception("The backend persisted the operation before losing its response")

/** Persists simulated server values, Offline, and operation receipts in a separate database. */
class DurableFixtureBackend(private val database: FixtureDatabase) {
    private val gate = Mutex()
    init { database.backendQueries.initializeSettings() }

    suspend fun setOffline(value: Boolean) = gate.withLock {
        database.backendQueries.setOffline(if (value) 1L else 0L)
    }

    suspend fun isOffline(): Boolean = gate.withLock { database.backendQueries.settings().executeAsOne().offline != 0L }

    suspend fun loseNextAcknowledgement() = gate.withLock { database.backendQueries.setFailNextAck(1L) }

    suspend fun appliedCount(): Long = gate.withLock { database.backendQueries.settings().executeAsOne().apply_count }

    suspend fun pushAttempts(): Long = gate.withLock { database.backendQueries.pushCount().executeAsOne() }

    suspend fun clientIds(account: String, installationId: String): List<String> = gate.withLock {
        database.backendQueries.clientIds(account, installationId).executeAsList()
    }

    // The pinned Store6 factory uses the same internal client ID for independent journals.
    // Bind an immutable transport partition to this journal, never to process-local key extras.
    fun scoped(account: String, installationId: String): MutationServer<MembershipKey, Membership> =
        object : MutationServer<MembershipKey, Membership> {
            override suspend fun push(request: MutationPush<MembershipKey, Membership>) =
                pushScoped(account, installationId, request)

            override suspend fun retire(request: MutationRetirement) =
                retireScoped(account, installationId, request)
        }

    suspend fun load(key: MembershipKey): Membership = gate.withLock {
        if (database.backendQueries.settings().executeAsOne().offline != 0L) throw BackendOffline()
        database.backendQueries.readValue(key.namespace.value, key.canonicalId()).executeAsOneOrNull()
            ?.let { MembershipCodec.decode(1, it.payload) }
            ?: Membership(key.trailId, key.listId, saved = false)
    }

    private suspend fun pushScoped(
        account: String,
        installationId: String,
        request: MutationPush<MembershipKey, Membership>,
    ): MutationAck<MembershipKey, Membership> = gate.withLock {
        require(request.identity.namespace == "saved-$account") { "The transport account does not match the mutation identity" }
        database.transaction {
            database.backendQueries.initializeClient(account, installationId, request.clientId)
            database.backendQueries.incrementPushCount(account, installationId, request.clientId)
        }
        if (database.backendQueries.settings().executeAsOne().offline != 0L) throw BackendOffline()
        val requested = (request.mine as MutationPresence.Present<Membership>).value
        val requestBytes = MembershipCodec.encode(requested)
        val previous = database.backendQueries.readReceipt(account, installationId, request.idempotencyKey).executeAsOneOrNull()
        val authoritative = if (previous != null) {
            check(previous.namespace == request.identity.namespace &&
                previous.canonical_id == request.identity.canonicalId &&
                previous.request_schema == 1L && previous.request_payload.contentEquals(requestBytes)) {
                "An idempotency key was reused for a different identity or request payload"
            }
            MembershipCodec.decode(1, previous.payload)
        } else {
            database.transaction {
                database.backendQueries.writeValue(request.identity.namespace, request.identity.canonicalId, requestBytes)
                database.backendQueries.writeReceipt(
                    account, installationId, request.idempotencyKey,
                    request.identity.namespace, request.identity.canonicalId, 1L, requestBytes, requestBytes,
                )
                database.backendQueries.incrementApplyCount()
            }
            requested
        }
        if (database.backendQueries.settings().executeAsOne().fail_next_ack != 0L) {
            database.backendQueries.setFailNextAck(0L)
            throw AcknowledgementLost()
        }
        MutationPresentAck(authoritative, etag = null, canonicalKey = null)
    }

    // Retirement retains receipts so tests can inspect completed operations.
    private suspend fun retireScoped(account: String, installationId: String, request: MutationRetirement) = gate.withLock {
        if (database.backendQueries.settings().executeAsOne().offline != 0L) throw BackendOffline()
        database.transaction {
            database.backendQueries.initializeClient(account, installationId, request.clientId)
            database.backendQueries.recordRetirement(request.retiredThroughSequence, account, installationId, request.clientId)
        }
        MutationRetirementAck(confirmedThroughSequence = request.retiredThroughSequence)
    }
}

data class DurableStatus(
    val pending: Int? = null,
    val parked: Int? = null,
    val offline: Boolean? = null,
    val revision: Long = 0,
    val inspectionError: String? = null,
    val operationError: String? = null,
)

/** The coordinator owns drain/recovery. Atom only calls enqueue(). Drivers are externally owned. */
class FixtureServices(
    val account: String,
    valueDriver: SqlDriver,
    valueDatabase: FixtureDatabase,
    journalDriver: SqlDriver,
    journalDatabase: FixtureDatabase,
    val backend: DurableFixtureBackend,
    clock: WallClock? = null,
) : SaveCommandSink {
    val journalInstallationId: String = journalDatabase.transactionWithResult {
        journalDatabase.journalIdentityQueries.initializeIdentity(account, Uuid.random().toString())
        journalDatabase.journalIdentityQueries.identity().executeAsOne().also {
            require(it.account == account) { "The journal belongs to another account" }
        }.installation_id
    }
    private val durableJournal = SqlDelightMutationJournalStorage(journalDriver, journalDatabase)
    private val correlatedJournal = CorrelatedJournalStorage(account, journalDatabase, durableJournal)
    private val admissionGate = Mutex()
    private lateinit var desiredMembership: MutatorRef<MembershipKey, Membership, SavedCommandPayload>
    private val registry = mutatorRegistry<MembershipKey, Membership> {
        desiredMembership = create(
            id = SavedCommandCodec.MUTATOR_ID,
            version = SavedCommandCodec.VERSION,
            codec = SavedCommandCodec,
            stales = { _, _ -> StaleSet(emptySet(), emptySet()) },
            project = { it.membership },
        )
    }
    private val sot = SqlDelightSourceOfTruth<MembershipKey, Membership>(
        driver = valueDriver,
        transacter = valueDatabase,
        readQuery = { key ->
            valueDatabase.valueRowsQueries.selectRow(key.namespace.value, key.canonicalId()) { _, _, payload ->
                MembershipCodec.decode(1, payload)
            }
        },
        writeRow = { key, value -> valueDatabase.valueRowsQueries.putRow(key.namespace.value, key.canonicalId(), MembershipCodec.encode(value)) },
        deleteRow = { key -> valueDatabase.valueRowsQueries.deleteRow(key.namespace.value, key.canonicalId()) },
        deleteNamespaceRows = { valueDatabase.valueRowsQueries.deleteNamespace(it.value) },
        deleteAllRows = { valueDatabase.valueRowsQueries.deleteAll() },
        wallClock = clock,
    )
    val memberships = mutationStore(
        registry = registry,
        server = backend.scoped(account, journalInstallationId),
        keyResolver = MutationKeyResolver { identity ->
            val parts = identity.canonicalId.split(':')
            if (identity.namespace == "saved-$account" && parts.size == 2) MembershipKey(account, parts[0], parts[1]) else null
        },
        valueCodecVersion = 1,
        valueCodec = MembershipCodec,
    ) {
        fetcher { key -> backend.load(key) }
        persistence(sot)
        bookkeeper(SqlDelightBookkeeper(valueDriver, valueDatabase))
        journalStorage(correlatedJournal)
        clock?.let { wallClock(it) }
    }
    private val mutableStatus = MutableStateFlow(DurableStatus())
    val status = mutableStatus.asStateFlow()

    override suspend fun enqueue(command: SaveCommand): EnqueueOutcome = admissionGate.withLock {
        val key = command.key
        if (key.account != account) return@withLock EnqueueOutcome.Rejected(command, "The account owner does not match")
        if (command.id.isBlank()) return@withLock EnqueueOutcome.Rejected(command, "The command ID is required")
        val payload = SavedCommandPayload(command.id, account, Membership(key.trailId, key.listId, saved = true))
        val existing = try {
            correlatedJournal.receipt(command.id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            return@withLock EnqueueOutcome.Uncertain(command, failure.message?.take(200) ?: "Admission receipt is unavailable")
        }
        if (existing != null) return@withLock receiptOutcome(command, payload, existing)
        try {
            val id = memberships.mutate(key, desiredMembership, payload)
            // Nothing fallible runs after the durable result before it is returned to the Atom.
            EnqueueOutcome.Journaled(command, id)
        } catch (cancelled: CancellationException) {
            // Propagate account cancellation. Committed receipts survive for reconciliation
            // by the account service. Atom does not take over retries.
            throw cancelled
        } catch (failure: Exception) {
            val recovered = try {
                correlatedJournal.receipt(command.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (inspectionFailure: Exception) {
                return@withLock EnqueueOutcome.Uncertain(
                    command, inspectionFailure.message?.take(200) ?: "Local admission outcome is unknown",
                )
            }
            if (recovered != null) {
                receiptOutcome(command, payload, recovered)
            } else {
                // Admission has finished. A successful read with no receipt proves this command
                // was not inserted. Do not call mutate again automatically.
                EnqueueOutcome.Rejected(command, failure.message?.take(200) ?: "The command was not saved")
            }
        }
    }

    private fun receiptOutcome(command: SaveCommand, payload: SavedCommandPayload, receipt: AdmissionReceipt): EnqueueOutcome =
        if (receipt.payload == payload) {
            EnqueueOutcome.Journaled(command, receipt.mutationId)
        } else {
            EnqueueOutcome.Rejected(command, "The command ID already belongs to different save choices")
        }

    suspend fun inspect() {
        try {
            mutableStatus.value = mutableStatus.value.copy(
                pending = memberships.pendingWrites().size,
                parked = memberships.deadLetters().size,
                offline = backend.isOffline(),
                revision = mutableStatus.value.revision + 1,
                inspectionError = null,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            mutableStatus.value = mutableStatus.value.copy(
                inspectionError = failure.message?.take(200) ?: "Durable status is unavailable",
            )
        }
    }

    suspend fun recoverAndDrain() {
        runAccountOperation { memberships.drain() }
        inspect()
    }

    suspend fun initialize() {
        runAccountOperation {
            if (!backend.isOffline()) memberships.get(MembershipKey(account, "weekend", "eagle-peak"))
            memberships.drain()
        }
        inspect()
    }

    suspend fun applyOffline(value: Boolean) {
        runAccountOperation { backend.setOffline(value) }
        inspect()
    }

    suspend fun reconnect() {
        runAccountOperation {
            backend.setOffline(false)
            // This explicit user retry bypasses backoff for the bounded fixture's current key.
            memberships.drain(MembershipKey(account, "weekend", "eagle-peak"))
        }
        inspect()
    }

    private suspend fun runAccountOperation(operation: suspend () -> Unit) {
        try {
            operation()
            mutableStatus.value = mutableStatus.value.copy(operationError = null)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            mutableStatus.value = mutableStatus.value.copy(operationError = failure.message?.take(200) ?: "Account recovery failed")
        }
    }

    suspend fun committed(key: MembershipKey) = memberships.get(key, Freshness.LocalOnly)

    suspend fun diagnostics(): String {
        val clients = backend.clientIds(account, journalInstallationId)
        val failures = durableJournal.transaction { transaction ->
            clients.flatMap { transaction.failures(it) }.joinToString { "${it.kind}:${it.detail}:${it.message}" }
        }
        val pending = memberships.pendingWrites().joinToString { "${it.mutationId}:${it.state}:attempt=${it.attempt}" }
        val parked = memberships.deadLetters().joinToString { "${it.mutationId}:${it.failure.kind}:${it.failure.message}" }
        return "pending=[$pending], parked=[$parked], failures=[$failures], status=${status.value}"
    }

    fun close() = memberships.close()
}
