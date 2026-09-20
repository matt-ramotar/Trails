package org.mobilenativefoundation.trails.data.devsettings

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.trails.db.TrailsDatabaseQueries
import org.mobilenativefoundation.trails.foundation.coroutines.Io
import org.mobilenativefoundation.trails.foundation.logging.Logger

@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class RealDeveloperSettingsRepository(
    @param:Named("AppCoroutineScope") private val appScope: CoroutineScope,
    private val databaseQueries: TrailsDatabaseQueries,
    private val logger: Logger,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Io,
) : DeveloperSettingsRepository {
    private val readRetries = MutableStateFlow(0L)
    private val readError = MutableStateFlow<String?>(null)
    private val persistedSettings = databaseQueries
            .developerSettingsQueries
            .selectSettings {
                    offlineMode,
                    conflictsEnabled,
                    conflictStrategy,
                    latencyMinMs,
                    latencyMaxMs,
                    errorRate,
                    rateLimitPerMinute,
                    backendConflictMode,
                    conflictProbability,
                    simulationSeedPreset,
                ->
                DeveloperSettings(
                    offlineMode = offlineMode != 0L,
                    conflictsEnabled = conflictsEnabled != 0L,
                    conflictStrategy = conflictStrategy.toConflictStrategy(),
                    latencyMinMs = latencyMinMs,
                    latencyMaxMs = latencyMaxMs,
                    errorRate = errorRate.toFloat(),
                    rateLimitPerMinute = rateLimitPerMinute.toInt(),
                    backendConflictMode = backendConflictMode.toBackendConflictMode(),
                    conflictProbability = conflictProbability.toFloat(),
                    simulationSeedPreset = simulationSeedPreset.toBackendSimulationSeedPreset(),
                )
            }
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { settings -> settings ?: DeveloperSettings() }
    private val state: StateFlow<DeveloperSettings?> = readRetries.flatMapLatest {
        persistedSettings.onEach { readError.value = null }.catch { failure ->
            if (failure is CancellationException) throw failure
            logger.error(TAG, "Failed to restore developer settings.", failure)
            readError.value = "Saved runtime settings could not be restored."
        }
    }.stateIn(appScope, SharingStarted.Eagerly, null)

    override fun readFailures(): Flow<String?> = readError
    override fun retryRead() { readRetries.update { it + 1L } }

    override fun stream(): Flow<DeveloperSettings> = state.filterNotNull()

    override val current: DeveloperSettings
        get() = state.value ?: DeveloperSettings()

    override suspend fun update(settings: DeveloperSettings) {
        withContext(dispatcher) {
            try {
                databaseQueries.developerSettingsQueries.upsertSettings(
                    settings.offlineMode.toDbBoolean(),
                    settings.conflictsEnabled.toDbBoolean(),
                    settings.conflictStrategy.name,
                    settings.latencyMinMs,
                    settings.latencyMaxMs,
                    settings.errorRate.toDouble(),
                    settings.rateLimitPerMinute.toLong(),
                    settings.backendConflictMode.name,
                    settings.conflictProbability.toDouble(),
                    settings.simulationSeedPreset.name,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                logger.error(TAG, "Failed to persist developer settings.", throwable)
                throw throwable
            }
        }
    }

    override suspend fun setOfflineMode(enabled: Boolean) {
        update(current.copy(offlineMode = enabled))
    }

    override suspend fun setConflictsEnabled(enabled: Boolean) {
        update(current.copy(conflictsEnabled = enabled))
    }

    override suspend fun setConflictStrategy(strategy: ConflictStrategy) {
        update(current.copy(conflictStrategy = strategy))
    }

    override suspend fun setLatencyRange(minMs: Long, maxMs: Long) {
        update(current.copy(latencyMinMs = minMs, latencyMaxMs = maxMs))
    }

    override suspend fun setErrorRate(rate: Float) {
        update(current.copy(errorRate = rate))
    }

    override suspend fun setRateLimitPerMinute(limit: Int) {
        update(current.copy(rateLimitPerMinute = limit))
    }

    override suspend fun setBackendConflictMode(mode: BackendConflictMode) {
        update(current.copy(backendConflictMode = mode))
    }

    override suspend fun setConflictProbability(probability: Float) {
        update(current.copy(conflictProbability = probability))
    }

    override suspend fun setSimulationSeedPreset(seedPreset: BackendSimulationSeedPreset) {
        update(current.copy(simulationSeedPreset = seedPreset))
    }

    override suspend fun resetToDefaults() {
        update(DeveloperSettings())
    }

    private fun String.toConflictStrategy(): ConflictStrategy =
        when (this) {
            "LAST_WRITE_WINS" -> ConflictStrategy.MERGE
            else -> runCatching { ConflictStrategy.valueOf(this) }.getOrDefault(ConflictStrategy.SERVER_WINS)
        }

    private fun String.toBackendConflictMode(): BackendConflictMode =
        when (this) {
            "FIRST_WRITE",
            "EVERY_WRITE",
            "RANDOM",
            "MANUAL",
            -> BackendConflictMode.HTTP_409

            else -> runCatching { BackendConflictMode.valueOf(this) }
                .getOrDefault(BackendConflictMode.DISABLED)
        }

    private fun String.toBackendSimulationSeedPreset(): BackendSimulationSeedPreset =
        when (this) {
            "DEFAULT",
            "EMPTY",
            "LARGE_DATASET",
            "CONFLICT_TESTING",
            "ERROR_STATES",
            -> BackendSimulationSeedPreset.RANDOM

            else -> runCatching { BackendSimulationSeedPreset.valueOf(this) }
                .getOrDefault(BackendSimulationSeedPreset.RANDOM)
        }

    private fun Boolean.toDbBoolean(): Long = if (this) 1L else 0L

    private companion object {
        private const val TAG = "RealDeveloperSettingsRepository"
    }
}
