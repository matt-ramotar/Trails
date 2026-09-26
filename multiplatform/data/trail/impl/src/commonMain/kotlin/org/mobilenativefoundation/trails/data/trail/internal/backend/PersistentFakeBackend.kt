@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class, kotlin.time.ExperimentalTime::class)

package org.mobilenativefoundation.trails.data.trail.internal.backend

import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mobilenativefoundation.store6.core.seam.StoreResults
import org.mobilenativefoundation.store6.mutations.*
import org.mobilenativefoundation.trails.data.backend.*
import org.mobilenativefoundation.trails.data.backend.BackendConfig
import org.mobilenativefoundation.trails.data.trail.account.BackendEvidence
import org.mobilenativefoundation.trails.data.trail.activity.CompletedActivity
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.data.trail.internal.catalog.comparator
import org.mobilenativefoundation.trails.data.trail.internal.catalog.matches
import org.mobilenativefoundation.trails.data.trail.internal.saved.SavedCodec
import org.mobilenativefoundation.trails.data.trail.internal.saved.SavedKey
import org.mobilenativefoundation.trails.data.trail.internal.saved.SavedValue
import org.mobilenativefoundation.trails.data.trail.recommendation.ForYouFeed
import org.mobilenativefoundation.trails.data.trail.storage.db.TrailDataDatabase

@Serializable
private data class StoredBackendConfig(
    val version: Int = 1,
    val mode: NetworkMode = NetworkMode.ONLINE,
    val minMs: Long = 50,
    val maxMs: Long = 200,
    val errorRate: Float = 0f,
    val rateLimit: Int = 0,
    val conflictMode: ConflictMode = ConflictMode.DISABLED,
    val conflictProbability: Float = 0f,
    val seed: SimulationSeedPreset = SimulationSeedPreset.RANDOM,
    val pageSize: Int = 20,
    val maxPageSize: Int = 100,
) {
    fun config(): BackendConfig {
        require(version == 1)
        return BackendConfig(mode, minMs.milliseconds..maxMs.milliseconds, errorRate, rateLimit,
            conflictMode, conflictProbability, seed, pageSize, maxPageSize)
    }
    companion object {
        fun from(config: BackendConfig) = StoredBackendConfig(
            mode = config.networkMode,
            minMs = config.latencyRange.start.inWholeMilliseconds.coerceAtLeast(0),
            maxMs = config.latencyRange.endInclusive.inWholeMilliseconds.coerceAtLeast(config.latencyRange.start.inWholeMilliseconds.coerceAtLeast(0)),
            errorRate = config.errorRate.coerceIn(0f, 1f), rateLimit = config.rateLimitRequestsPerMinute.coerceAtLeast(0),
            conflictMode = config.conflictMode, conflictProbability = config.conflictProbability.coerceIn(0f, 1f),
            seed = config.simulationSeedPreset, pageSize = config.defaultPageSize, maxPageSize = config.maxPageSize,
        )
    }
}

/** Catalog version 4 includes derived activities. Changing this version replaces the installed catalog on open. */
internal const val SEED_VERSION = 4L

/** Older payloads gain the matching sample excerpt when they are fetched again. */
private val sampleReviewExcerpts = worldTrails.associate { it.id to it.reviewExcerpt }

private fun Trail.withSampleReviewExcerpt(): Trail =
    if (reviewExcerpt != null) this else copy(reviewExcerpt = sampleReviewExcerpts[id])

internal class PersistentFakeBackend(private val database: TrailDataDatabase) {
    private val sql = database.trailDataQueries
    private val gate = Mutex()
    private var retired = false
    private val applied = MutableStateFlow<BackendConfig?>(null)
    val config = applied.asStateFlow()
    val offline get() = applied.value?.networkMode == NetworkMode.OFFLINE
    val reseeded: Boolean

    init {
        reseeded = database.transactionWithResult {
            sql.initializeBackend(Json.encodeToString(StoredBackendConfig.serializer(), StoredBackendConfig()))
            if (sql.backendMeta().executeAsOne().seeded != SEED_VERSION) {
                sql.deleteBackendTrails()
                worldTrails.forEachIndexed { index, trail ->
                    sql.putBackendTrail(trail.id, Json.encodeToString(Trail.serializer(), trail.withSampleReviewExcerpt()).encodeToByteArray(), index.toLong())
                }
                sql.setSeeded(SEED_VERSION)
                true
            } else false
        }
    }

    suspend fun restore(): BackendConfig = gate.withLock {
        requireAvailable()
        Json.decodeFromString(StoredBackendConfig.serializer(), sql.backendMeta().executeAsOne().config).config().also { applied.value = it }
    }
    suspend fun apply(config: BackendConfig) = gate.withLock {
        requireAvailable()
        val stored = StoredBackendConfig.from(config)
        sql.setBackendConfig(Json.encodeToString(StoredBackendConfig.serializer(), stored))
        applied.value = stored.config()
    }
    fun ensureReady() { check(applied.value != null) { "Backend settings must be restored before opening data" } }
    private fun requireAvailable() { if (retired) throw CancellationException("The fake backend has retired") }
    private fun requireOnline() { requireAvailable(); check(!offline) { "The fake backend Offline setting is applied" } }

    private suspend fun request(): Pair<BackendConfig, Random> {
        ensureReady()
        val configured = requireNotNull(applied.value)
        check(configured.networkMode != NetworkMode.OFFLINE) { "The fake backend Offline setting is applied" }
        val random = gate.withLock {
            requireOnline()
            val meta = sql.backendMeta().executeAsOne()
            val minute = Clock.System.now().toEpochMilliseconds() / 60_000
            val count = if (meta.rate_minute == minute) meta.rate_count + 1 else 1L
            sql.recordRequest(minute, count)
            check(configured.rateLimitRequestsPerMinute == 0 || count <= configured.rateLimitRequestsPerMinute) { "The fake backend rate limit was reached" }
            val seed = when (configured.simulationSeedPreset) {
                SimulationSeedPreset.RANDOM -> Random.nextInt()
                SimulationSeedPreset.SEED_42 -> 42
                SimulationSeedPreset.SEED_1337 -> 1337
                SimulationSeedPreset.SEED_9001 -> 9001
            }
            Random(seed xor meta.requests.toInt())
        }
        val min = configured.latencyRange.start.inWholeMilliseconds
        val max = configured.latencyRange.endInclusive.inWholeMilliseconds
        if (max > 0) delay(if (min == max) min else random.nextLong(min, max + 1))
        check(!offline) { "The fake backend Offline setting is applied" }
        check(random.nextFloat() >= configured.errorRate) { "The fake backend simulated a request failure" }
        return configured to random
    }

    suspend fun search(query: TrailQuery): List<Trail> {
        request()
        return gate.withLock {
            requireOnline()
            sql.backendTrails().executeAsList()
                .map { Json.decodeFromString(Trail.serializer(), it.payload.decodeToString()).withSampleReviewExcerpt() }
                .filter { it.matches(query.normalized()) }
                .sortedWith(query.sort.comparator())
        }
    }
    suspend fun trail(id: String): Trail {
        request()
        return gate.withLock {
            requireOnline()
            // Existing fixture rows gain content on a fresh read, without reseeding backend state.
            sql.backendTrail(id).executeAsOneOrNull()?.let { Json.decodeFromString(Trail.serializer(), it.payload.decodeToString()).withSampleReviewExcerpt() }
                ?: error("This trail is unavailable")
        }
    }
    suspend fun saved(account: String, trailId: String): SavedValue {
        request()
        return gate.withLock { requireOnline(); savedLocal(account, trailId) }
    }
    suspend fun activities(account: String): List<CompletedActivity> {
        request()
        return gate.withLock {
            requireOnline()
            fixture("backend-activities", account, ListSerializer(CompletedActivity.serializer())) { sampleActivities(Clock.System.now().toEpochMilliseconds()) }
        }
    }
    suspend fun forYou(account: String): ForYouFeed {
        request()
        return gate.withLock { requireOnline(); fixture("backend-foryou", account, ForYouFeed.serializer()) { sampleForYou } }
    }
    /**
     * Per-account fixtures live in this database's otherwise-unused cache_row table, so installed
     * builds need no schema migration. A row is written on the first request and read verbatim after.
     */
    private fun <T> fixture(namespace: String, account: String, serializer: KSerializer<T>, seed: () -> T): T =
        sql.readCache(namespace, account).executeAsOneOrNull()?.let { Json.decodeFromString(serializer, it.payload.decodeToString()) }
            ?: seed().also { sql.writeCache(namespace, account, Json.encodeToString(serializer, it).encodeToByteArray()) }
    private fun savedLocal(account: String, trailId: String): SavedValue =
        sql.readBackendSaved(account, trailId).executeAsOneOrNull()?.let { SavedCodec.decode(1, it.payload) } ?: SavedValue(trailId, emptySet())

    fun server(account: String, installation: String) = object : MutationServer<SavedKey, SavedValue> {
        override suspend fun push(request: MutationPush<SavedKey, SavedValue>): MutationAck<SavedKey, SavedValue> {
            gate.withLock { requireAvailable(); sql.incrementPushes() }
            val (configured, random) = this@PersistentFakeBackend.request()
            return gate.withLock {
                requireOnline()
                require(request.identity.namespace == "saved-$account")
                val desired = (request.mine as MutationPresence.Present<SavedValue>).value
                require(desired.trailId == request.identity.canonicalId && desired.collectionIds.all { it == "weekend" || it == "favorites" })
                val bytes = SavedCodec.encode(desired)
                val previous = sql.readReceipt(account, installation, request.idempotencyKey).executeAsOneOrNull()
                val authoritative = if (previous != null) {
                    check(previous.trail_id == desired.trailId && previous.version == 1L && previous.request.contentEquals(bytes)) { "Idempotency identity reused with different choices" }
                    SavedCodec.decode(1, previous.result)
                } else {
                    val current = savedLocal(account, desired.trailId)
                    val captured = (request.base as? MutationPresence.Present<SavedValue>)?.value ?: SavedValue(desired.trailId, emptySet())
                    val simulated = configured.conflictMode != ConflictMode.DISABLED && random.nextFloat() < configured.conflictProbability
                    if ((current != captured && configured.conflictMode != ConflictMode.LAST_WRITE_WINS && configured.conflictMode != ConflictMode.AUTO_MERGE) || (simulated && configured.conflictMode == ConflictMode.HTTP_409)) {
                        throw StoreResults.exception(StoreResults.conflict(null, "Saved choices changed on the fake server"))
                    }
                    val result = if (simulated && configured.conflictMode == ConflictMode.AUTO_MERGE) desired.copy(collectionIds = current.collectionIds + desired.collectionIds) else desired
                    val resultBytes = SavedCodec.encode(result)
                    database.transaction {
                        sql.writeBackendSaved(account, desired.trailId, resultBytes)
                        sql.writeReceipt(account, installation, request.idempotencyKey, desired.trailId, 1, bytes, resultBytes)
                        sql.incrementApplications()
                    }
                    result
                }
                if (sql.backendMeta().executeAsOne().lose_ack != 0L) {
                    sql.setLoseAck(0)
                    error("The fake backend persisted this save before losing its response")
                }
                MutationPresentAck(authoritative, null, null)
            }
        }
        override suspend fun retire(request: MutationRetirement): MutationRetirementAck {
            this@PersistentFakeBackend.request()
            return gate.withLock {
                requireOnline()
                database.transaction {
                    sql.initializeClient(account, installation, request.clientId)
                    sql.recordRetirement(request.retiredThroughSequence, account, installation, request.clientId)
                }
                MutationRetirementAck(request.retiredThroughSequence)
            }
        }
    }

    suspend fun evidence(): BackendEvidence = gate.withLock {
        requireAvailable()
        val meta = sql.backendMeta().executeAsOne()
        BackendEvidence(meta.applications, sql.receiptCount().executeAsOne(), meta.pushes, meta.requests)
    }
    suspend fun loseNextAcknowledgement() = gate.withLock { requireAvailable(); sql.setLoseAck(1) }
    /** All backend SQL is under this gate; future delayed requests fail before touching the driver. */
    suspend fun close() = gate.withLock { retired = true }
}
