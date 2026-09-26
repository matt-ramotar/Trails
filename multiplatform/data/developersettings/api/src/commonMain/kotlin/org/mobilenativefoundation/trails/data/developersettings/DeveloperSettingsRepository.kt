package org.mobilenativefoundation.trails.data.developersettings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Mutations return after settings are persisted and propagate persistence failures and cancellation.
 * [stream] and [current] observe storage asynchronously. Runtime backend application is separate.
 */
interface DeveloperSettingsRepository {
    fun stream(): Flow<DeveloperSettings>

    val current: DeveloperSettings

    /** Read failure is separate from the last applied settings. Never invent defaults on failure. */
    fun readFailures(): Flow<String?> = flowOf(null)
    fun retryRead() = Unit

    suspend fun update(settings: DeveloperSettings)

    suspend fun setOfflineMode(enabled: Boolean)

    suspend fun setConflictsEnabled(enabled: Boolean)

    suspend fun setLatencyRange(minMs: Long, maxMs: Long)

    suspend fun setErrorRate(rate: Float)

    suspend fun setRateLimitPerMinute(limit: Int)

    suspend fun setBackendConflictMode(mode: BackendConflictMode)

    suspend fun setConflictProbability(probability: Float)

    suspend fun setSimulationSeedPreset(seedPreset: BackendSimulationSeedPreset)

    suspend fun resetToDefaults()
}
