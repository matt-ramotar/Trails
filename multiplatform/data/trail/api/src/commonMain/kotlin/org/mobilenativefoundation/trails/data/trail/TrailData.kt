package org.mobilenativefoundation.trails.data.trail

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import org.mobilenativefoundation.trails.server.BackendConfig

@Serializable
enum class TrailDifficulty { EASY, MODERATE, HARD, STRENUOUS }

@Serializable
enum class TrailFeature { LAKE, FOREST, WATERFALL, SUMMIT, LOOP, DOG_FRIENDLY }

/** Derived from route facts, never authored per route: every route is hiked; multi-day treks are backpacked. */
@Serializable
enum class TrailActivity { HIKING, BACKPACKING }

/** Most popular is the catalog's documented recommended order; the others reorder the same membership. */
@Serializable
enum class TrailSort { MOST_POPULAR, HIGHEST_RATED, SHORTEST, LONGEST }

@Serializable
data class Trail(
    val id: String,
    val name: String,
    val region: String,
    val description: String,
    val difficulty: TrailDifficulty,
    val distanceMeters: Int,
    val elevationMeters: Int,
    val durationMinutes: Int,
    val rating: Double,
    val reviewCount: Int,
    val features: Set<TrailFeature>,
    val photoIndex: Int,
    val reviewExcerpt: String? = null,
    val activities: Set<TrailActivity> = emptySet(),
)

@Serializable
data class TrailQuery(
    val text: String = "",
    val region: String? = null,
    val difficulties: Set<TrailDifficulty> = emptySet(),
    val minMeters: Int = 0,
    val maxMeters: Int? = null,
    val features: Set<TrailFeature> = emptySet(),
    val minElevationGain: Int = 0,
    val maxElevationGain: Int? = null,
    val dogFriendly: Boolean = false,
    val activities: Set<TrailActivity> = emptySet(),
    val sort: TrailSort = TrailSort.MOST_POPULAR,
) {
    fun normalized(): TrailQuery = copy(
        text = text.trim().replace(Regex("\\s+"), " ").lowercase(),
        region = region?.trim()?.lowercase()?.takeIf { it.isNotEmpty() },
        minMeters = minMeters.coerceAtLeast(0),
        maxMeters = maxMeters?.coerceAtLeast(minMeters.coerceAtLeast(0)),
        difficulties = difficulties.sortedBy { it.name }.toSet(),
        features = features.sortedBy { it.name }.toSet(),
        minElevationGain = minElevationGain.coerceAtLeast(0),
        maxElevationGain = maxElevationGain?.coerceAtLeast(minElevationGain.coerceAtLeast(0)),
        activities = activities.sortedBy { it.name }.toSet(),
    )
}

/** Null data means unknown/unavailable. A successful empty list is a different state. */
data class LoadState<out T>(
    val data: T? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val offline: Boolean = false,
)

interface TrailRepository {
    fun observeQuery(query: TrailQuery): Flow<LoadState<List<Trail>>>
    fun observeTrail(id: String): Flow<LoadState<Trail>>
    suspend fun refreshQuery(query: TrailQuery)
    suspend fun refreshTrail(id: String)
    suspend fun count(query: TrailQuery): LoadState<Int>
}

@Serializable
data class TrailCollection(val id: String, val name: String)

/** A finished hike. Until the recorder ships, the fake backend seeds sample rows per account (DEV-37). */
@Serializable
data class CompletedActivity(
    val id: String,
    val trailId: String,
    val trailName: String,
    val completedAtEpochMillis: Long,
    val distanceMeters: Int,
    val durationMinutes: Int,
    val elevationMeters: Int,
)

/** Per-account recommendation fixture: one featured trail and rows "more like" an anchor trail. */
@Serializable
data class ForYouFeed(
    val featuredTrailId: String,
    val headline: String,
    val subline: String,
    val anchorTrailId: String,
    val recommendedTrailIds: List<String>,
)

interface ActivityRepository {
    fun observe(): Flow<LoadState<List<CompletedActivity>>>
    suspend fun refresh()
}

interface ForYouRepository {
    fun observe(): Flow<LoadState<ForYouFeed>>
    suspend fun refresh()
}

enum class TrailSyncStatus { SYNCED, PENDING, SYNCING, FINISHING, PARKED }

/** Why parked work cannot proceed. [INCOMPATIBLE] means this build cannot read the stored work, so only an app update resolves it. */
enum class TrailSyncCause { INCOMPATIBLE, OTHER }

data class TrailSync(
    val status: TrailSyncStatus,
    val pendingCount: Int = 0,
    val reason: String? = null,
    val canRetry: Boolean = false,
    val cause: TrailSyncCause = TrailSyncCause.OTHER,
)

data class SavedSnapshot(
    val collections: List<TrailCollection>,
    val memberships: Map<String, Set<String>>,
    val trails: List<Trail>,
    val syncByTrail: Map<String, TrailSync>,
    val syncing: Boolean = false,
    val offline: Boolean = false,
)

/** This immutable identity always describes the entire desired set for one account trail. */
data class SetCollectionsCommand(val id: String, val trailId: String, val collectionIds: Set<String>)

sealed interface SaveOutcome {
    val command: SetCollectionsCommand
    data class Journaled(override val command: SetCollectionsCommand, val mutationId: String) : SaveOutcome
    data class Rejected(override val command: SetCollectionsCommand, val reason: String) : SaveOutcome
    data class Uncertain(override val command: SetCollectionsCommand, val reason: String) : SaveOutcome
}

interface SavedRepository {
    val state: StateFlow<LoadState<SavedSnapshot>>
    suspend fun save(command: SetCollectionsCommand): SaveOutcome
    /** Inspects an earlier frozen command; never creates a new durable mutation. */
    suspend fun reconcile(command: SetCollectionsCommand): SaveOutcome
    suspend fun refresh()
    suspend fun retryPending()
}

interface TrailAccount {
    val accountId: String
    val saved: SavedRepository
    val activities: ActivityRepository
    val forYou: ForYouRepository
    /** Retires the account owner, joins jobs, then closes its drivers. */
    suspend fun close()
}

interface TrailDataFactory {
    val trails: TrailRepository
    val backendConfig: StateFlow<BackendConfig?>
    suspend fun restoreBackendConfig(): BackendConfig
    suspend fun applyBackendConfig(config: BackendConfig)
    suspend fun open(accountId: String): TrailAccount
    suspend fun close()
}
