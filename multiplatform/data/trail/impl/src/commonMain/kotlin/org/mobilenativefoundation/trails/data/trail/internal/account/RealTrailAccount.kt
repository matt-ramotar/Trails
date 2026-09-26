@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class, kotlin.uuid.ExperimentalUuidApi::class)

package org.mobilenativefoundation.trails.data.trail.internal.account

import app.cash.sqldelight.db.SqlDriver
import kotlin.uuid.Uuid
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import org.mobilenativefoundation.store6.core.Freshness
import org.mobilenativefoundation.store6.core.StoreResult
import org.mobilenativefoundation.store6.mutations.*
import org.mobilenativefoundation.store6.mutations.sqldelight.SqlDelightMutationJournalStorage
import org.mobilenativefoundation.store6.sqldelight.SqlDelightBookkeeper
import org.mobilenativefoundation.store6.sqldelight.SqlDelightSourceOfTruth
import org.mobilenativefoundation.trails.data.trail.LoadState
import org.mobilenativefoundation.trails.data.trail.account.TrailAccount
import org.mobilenativefoundation.trails.data.trail.activity.ActivityRepository
import org.mobilenativefoundation.trails.data.trail.activity.CompletedActivity
import org.mobilenativefoundation.trails.data.trail.internal.backend.PersistentFakeBackend
import org.mobilenativefoundation.trails.data.trail.internal.catalog.RealTrailRepository
import org.mobilenativefoundation.trails.data.trail.internal.saved.Acceptance
import org.mobilenativefoundation.trails.data.trail.internal.saved.CorrelatedSavedJournal
import org.mobilenativefoundation.trails.data.trail.internal.saved.SavedCodec
import org.mobilenativefoundation.trails.data.trail.internal.saved.SavedCommandCodec
import org.mobilenativefoundation.trails.data.trail.internal.saved.SavedCommandPayload
import org.mobilenativefoundation.trails.data.trail.internal.saved.SavedKey
import org.mobilenativefoundation.trails.data.trail.internal.saved.SavedValue
import org.mobilenativefoundation.trails.data.trail.internal.storage.StorageLifetime
import org.mobilenativefoundation.trails.data.trail.internal.storage.bookkeeper
import org.mobilenativefoundation.trails.data.trail.internal.storage.journal
import org.mobilenativefoundation.trails.data.trail.internal.storage.messageText
import org.mobilenativefoundation.trails.data.trail.internal.storage.source
import org.mobilenativefoundation.trails.data.trail.recommendation.ForYouFeed
import org.mobilenativefoundation.trails.data.trail.recommendation.ForYouRepository
import org.mobilenativefoundation.trails.data.trail.saved.SaveOutcome
import org.mobilenativefoundation.trails.data.trail.saved.SavedRepository
import org.mobilenativefoundation.trails.data.trail.saved.SavedSnapshot
import org.mobilenativefoundation.trails.data.trail.saved.SetCollectionsCommand
import org.mobilenativefoundation.trails.data.trail.saved.TrailCollection
import org.mobilenativefoundation.trails.data.trail.saved.TrailSync
import org.mobilenativefoundation.trails.data.trail.saved.TrailSyncCause
import org.mobilenativefoundation.trails.data.trail.saved.TrailSyncStatus
import org.mobilenativefoundation.trails.data.trail.storage.db.TrailDataDatabase

internal class RealTrailAccount(
    override val accountId: String,
    private val valueDriver: SqlDriver,
    private val journalDriver: SqlDriver,
    private val backend: PersistentFakeBackend,
    private val catalog: RealTrailRepository,
    parent: CoroutineScope,
    private val recovery: AccountRecoveryPolicy,
) : TrailAccount, SavedRepository {
    private val values = TrailDataDatabase(valueDriver)
    private val journalDatabase = TrailDataDatabase(journalDriver)
    private val storageLifetime = StorageLifetime()
    private val accountJob = SupervisorJob(parent.coroutineContext[Job])
    private val scope = CoroutineScope(parent.coroutineContext + accountJob)
    private val admission = Mutex()
    private val drainGate = Mutex()
    private val keyGate = Mutex()
    private val publication = Mutex()
    private val retirement = Mutex()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var syncing = false
    var isClosed = false
        private set
    private var valueDriverClosed = false
    private var journalDriverClosed = false
    private val projected = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    private val observing = mutableSetOf<String>()
    private val mutableState = MutableStateFlow(LoadState<SavedSnapshot>())
    override val state = mutableState.asStateFlow()
    override val saved: SavedRepository = this
    private val collections: List<TrailCollection>
    private val installation: String

    init {
        values.transaction {
            values.trailDataQueries.putCollection("weekend", "Weekend adventures", 0)
            values.trailDataQueries.putCollection("favorites", "Favorites", 1)
        }
        collections = values.trailDataQueries.collections().executeAsList().map { TrailCollection(it.id, it.name) }
        installation = journalDatabase.transactionWithResult {
            journalDatabase.trailDataQueries.initializeIdentity(accountId, Uuid.random().toString())
            journalDatabase.trailDataQueries.identity().executeAsOne().also { require(it.account == accountId) }.installation_id
        }
    }
    private val journal = CorrelatedSavedJournal(accountId, journalDatabase,
        storageLifetime.journal(SqlDelightMutationJournalStorage(journalDriver, journalDatabase)))
    private val activityFeed = AccountFeed("activities", accountId, valueDriver, values, backend, storageLifetime, ListSerializer(CompletedActivity.serializer())) { backend.activities(accountId) }
    private val forYouFeed = AccountFeed("foryou", accountId, valueDriver, values, backend, storageLifetime, ForYouFeed.serializer()) { backend.forYou(accountId) }
    override val activities: ActivityRepository = object : ActivityRepository {
        override fun observe() = activityFeed.observe()
        override suspend fun refresh() = activityFeed.refresh()
    }
    override val forYou: ForYouRepository = object : ForYouRepository {
        override fun observe() = forYouFeed.observe()
        override suspend fun refresh() = forYouFeed.refresh()
    }
    private lateinit var setCollections: MutatorRef<SavedKey, SavedValue, SavedCommandPayload>
    private val registry = mutatorRegistry<SavedKey, SavedValue> {
        setCollections = create(SavedCommandCodec.ID, 1, SavedCommandCodec,
            stales = { _, _ -> StaleSet(emptySet(), emptySet()) }, project = { it.value })
    }
    private val source = storageLifetime.source(SqlDelightSourceOfTruth<SavedKey, SavedValue>(
        valueDriver, values,
        readQuery = { key -> values.trailDataQueries.readCache(key.namespace.value, key.trailId) { _, _, payload -> SavedCodec.decode(1, payload) } },
        writeRow = { key, value -> values.trailDataQueries.writeCache(key.namespace.value, key.trailId, SavedCodec.encode(value)) },
        deleteRow = { values.trailDataQueries.deleteCache(it.namespace.value, it.trailId) },
        deleteNamespaceRows = { values.trailDataQueries.deleteNamespace(it.value) },
        deleteAllRows = { values.trailDataQueries.deleteAllCache() },
        wallClock = recovery.clock,
    ))
    private val store = mutationStore(registry, backend.server(accountId, installation),
        MutationKeyResolver { identity -> if (identity.namespace == "saved-$accountId") SavedKey(accountId, identity.canonicalId) else null },
        1, SavedCodec) {
        fetcher { backend.saved(accountId, it.trailId) }
        persistence(source)
        bookkeeper(storageLifetime.bookkeeper(SqlDelightBookkeeper(valueDriver, values)))
        journalStorage(journal)
        recovery.clock?.let { wallClock(it) }
        conflicts { merge { _, mine, _ -> MutationConflictResolution.Retry(mine) } }
    }

    suspend fun initialize() {
        val ids = catalog.cachedTrails().map { it.id }.toSet() + journal.knownTrailIds() + store.pendingWrites().map { it.canonicalId }
        for (id in ids) observeKey(id)
        publish()
        scope.launch {
            catalog.revision.collect {
                safely { catalog.cachedTrails().forEach { observeKey(it.id) }; publish() }
            }
        }
        scope.launch { backend.config.collect { safely { publish() }; if (!backend.offline) wake.trySend(Unit) } }
        if (recovery.automatic) scope.launch {
            for (signal in wake) {
                while (isActive) {
                    // A failed inspection leaves recovery scheduled instead of killing its owner.
                    var retry = true
                    safely {
                        drain(manual = false)
                        retry = store.pendingWrites().any { !backend.offline || it.needsLocalAdoption() }
                    }
                    if (!retry) break
                    // Store6 eligibility can be five minutes away. Keep bounded passes alive for
                    // this account until settled. A new save or settings change wakes this wait.
                    recovery.awaitRetry(wake)
                }
            }
        }
        wake.trySend(Unit)
    }

    private suspend fun observeKey(trailId: String) = keyGate.withLock {
        if (trailId in observing) return@withLock
        val key = SavedKey(accountId, trailId)
        // A locally provisioned demo account starts with no memberships. Empty values are durable.
        // Reopened mutation overlays reconstruct submitted choices over these confirmed bases.
        if (source.reader(key).first() == null) source.write(key, SavedValue(trailId, emptySet()))
        val first = store.stream(key, Freshness.LocalOnly).first { it is StoreResult.Data || it is StoreResult.Error }
        if (first is StoreResult.Error) error(first.error.messageText())
        projected.update { it + (trailId to (first as StoreResult.Data<SavedValue>).value.collectionIds) }
        observing += trailId
        scope.launch {
            store.stream(key, Freshness.LocalOnly).collect { result ->
                when (result) {
                    is StoreResult.Data -> { projected.update { it + (trailId to result.value.collectionIds) }; safely { publish() } }
                    is StoreResult.Error -> mutableState.update { it.copy(loading = false, error = result.error.messageText()) }
                    else -> Unit
                }
            }
        }
    }

    override suspend fun save(command: SetCollectionsCommand): SaveOutcome {
        val frozen = command.copy(collectionIds = command.collectionIds.sorted().toSet())
        check(!isClosed) { "This account is no longer active" }
        // The account owns this job. Cancelling a screen's await does not cancel durable admission.
        return scope.async { admission.withLock { admit(frozen, allowEnqueue = true) } }.await()
    }
    override suspend fun reconcile(command: SetCollectionsCommand): SaveOutcome {
        val frozen = command.copy(collectionIds = command.collectionIds.sorted().toSet())
        check(!isClosed)
        return scope.async { admission.withLock { admit(frozen, allowEnqueue = false) } }.await()
    }

    private suspend fun admit(command: SetCollectionsCommand, allowEnqueue: Boolean): SaveOutcome {
        if (command.id.isBlank() || command.trailId.isBlank() || command.collectionIds.any { id -> collections.none { it.id == id } })
            return SaveOutcome.Rejected(command, "Choose valid collections for this trail")
        val payload = SavedCommandPayload(command.id, accountId, SavedValue(command.trailId, command.collectionIds))
        val existing = try { journal.receipt(command.id) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { return SaveOutcome.Uncertain(command, failure.message ?: "Save information is unavailable") }
        if (existing != null) return outcome(command, payload, existing)
        if (!allowEnqueue) return SaveOutcome.Rejected(command, "This save was not added on this device")
        return try {
            observeKey(command.trailId)
            val id = store.mutate(SavedKey(accountId, command.trailId), setCollections, payload)
            wake.trySend(Unit)
            scope.launch { safely { publish() } }
            SaveOutcome.Journaled(command, id)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            val receipt = try { journal.receipt(command.id) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (inspection: Exception) { return SaveOutcome.Uncertain(command, inspection.message ?: "Checking your save") }
            if (receipt != null) outcome(command, payload, receipt)
            else SaveOutcome.Rejected(command, failure.message ?: "Couldn’t save on this device")
        }
    }
    private fun outcome(command: SetCollectionsCommand, payload: SavedCommandPayload, receipt: Acceptance): SaveOutcome =
        if (payload == receipt.payload) SaveOutcome.Journaled(command, receipt.mutationId)
        else SaveOutcome.Rejected(command, "This command already describes different save choices")

    override suspend fun refresh() {
        check(!isClosed)
        scope.async { safely { catalog.cachedTrails().forEach { observeKey(it.id) }; publish() } }.await()
    }
    override suspend fun retryPending() {
        check(!isClosed)
        scope.async { safely { drain(manual = true) } }.await()
        wake.trySend(Unit)
    }
    private suspend fun drain(manual: Boolean) = drainGate.withLock {
        val localAdoptions = if (backend.offline) store.pendingWrites().filter { it.needsLocalAdoption() }.map { it.canonicalId }.distinct() else emptyList()
        if (backend.offline && localAdoptions.isEmpty()) { publish(); return@withLock }
        syncing = true
        try {
            publish()
            if (backend.offline) localAdoptions.forEach { store.drain(SavedKey(accountId, it)) }
            else if (manual) store.pendingWrites().map { it.canonicalId }.distinct().forEach { store.drain(SavedKey(accountId, it)) }
            else store.drain()
        } finally { syncing = false; publish() }
    }

    private suspend fun publish() = publication.withLock {
        val trails = catalog.cachedTrails()
        val pending = store.pendingWrites().groupBy { it.canonicalId }
        val parked = store.deadLetters().associateBy { it.canonicalId }
        val sync = (projected.value.keys + pending.keys + parked.keys).associateWith { id ->
            val rows = pending[id].orEmpty()
            when {
                parked[id] != null -> parked.getValue(id).failure.let { failure ->
                    TrailSync(TrailSyncStatus.PARKED, reason = failure.message, cause = failure.syncCause())
                }
                rows.any { it.state == MutationPendingState.ADOPTING || it.state == MutationPendingState.APPLYING_EFFECTS } -> TrailSync(TrailSyncStatus.FINISHING, rows.size)
                rows.isNotEmpty() -> TrailSync(if (syncing) TrailSyncStatus.SYNCING else TrailSyncStatus.PENDING, rows.size,
                    reason = if (backend.offline) "Waiting until you’re back online" else if (rows.any { it.attempt > 0 }) "Sync needs attention" else "Waiting to sync", canRetry = !backend.offline && rows.any { it.attempt > 0 })
                else -> TrailSync(TrailSyncStatus.SYNCED)
            }
        }
        mutableState.value = LoadState(SavedSnapshot(collections, projected.value, trails, sync, syncing, backend.offline), loading = false, offline = backend.offline)
    }
    /**
     * Store6 normalizes "the stored bytes cannot be decoded, or their mutator is not registered"
     * to [MutationFailureKind.CODEC]. For this app that is exactly work a newer build wrote, so it
     * is the one parked cause an app update resolves. Every other kind is ordinary paused work.
     */
    private fun MutationFailure.syncCause() =
        if (kind == MutationFailureKind.CODEC) TrailSyncCause.INCOMPATIBLE else TrailSyncCause.OTHER
    private fun PendingIntent.needsLocalAdoption() = state == MutationPendingState.ADOPTING || state == MutationPendingState.APPLYING_EFFECTS
    private suspend fun safely(operation: suspend () -> Unit) {
        try { operation() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { mutableState.update { it.copy(loading = false, error = failure.message ?: "Saved trails are unavailable", offline = backend.offline) } }
    }
    override suspend fun close() = retirement.withLock {
        if (!valueDriverClosed || !journalDriverClosed) {
            isClosed = true
            accountJob.cancelAndJoin()
            // A receive inside the retry timer must observe account cancellation first.
            // Closing its channel first can throw ClosedReceiveChannelException on another thread.
            wake.close()
            activityFeed.close()
            forYouFeed.close()
            store.close()
            storageLifetime.retire()
            if (!valueDriverClosed) { valueDriver.close(); valueDriverClosed = true }
            if (!journalDriverClosed) { journalDriver.close(); journalDriverClosed = true }
        }
    }
}
