package org.mobilenativefoundation.trails.data.developersettings

import org.mobilenativefoundation.trails.data.backend.BackendConfig
import org.mobilenativefoundation.trails.data.backend.ConflictMode
import org.mobilenativefoundation.trails.data.backend.NetworkMode
import org.mobilenativefoundation.trails.data.backend.SimulationSeedPreset
import kotlin.time.Duration.Companion.milliseconds

data class DeveloperSettings(
    val offlineMode: Boolean = false,
    val conflictsEnabled: Boolean = false,
    val latencyMinMs: Long = 50L,
    val latencyMaxMs: Long = 200L,
    val errorRate: Float = 0.0f,
    val rateLimitPerMinute: Int = 0,
    val backendConflictMode: BackendConflictMode = BackendConflictMode.DISABLED,
    val conflictProbability: Float = 0.0f,
    val simulationSeedPreset: BackendSimulationSeedPreset = BackendSimulationSeedPreset.RANDOM,
)

enum class BackendConflictMode {
    DISABLED,
    HTTP_409,
    AUTO_MERGE,
    LAST_WRITE_WINS,
}

enum class BackendSimulationSeedPreset {
    RANDOM,
    SEED_42,
    SEED_1337,
    SEED_9001,
}

fun DeveloperSettings.toBackendConfig(): BackendConfig {
    val conflictMode = if (!conflictsEnabled) {
        ConflictMode.DISABLED
    } else {
        backendConflictMode.toServerConflictMode()
    }

    return BackendConfig(
        networkMode = if (offlineMode) NetworkMode.OFFLINE else NetworkMode.ONLINE,
        latencyRange = latencyMinMs.milliseconds..latencyMaxMs.milliseconds,
        errorRate = errorRate.coerceIn(0.0f, 1.0f),
        rateLimitRequestsPerMinute = rateLimitPerMinute.coerceAtLeast(0),
        conflictMode = conflictMode,
        conflictProbability = conflictProbability.coerceIn(0.0f, 1.0f),
        simulationSeedPreset = simulationSeedPreset.toServerSeedPreset(),
    )
}

private fun BackendConflictMode.toServerConflictMode(): ConflictMode =
    when (this) {
        BackendConflictMode.DISABLED -> ConflictMode.DISABLED
        BackendConflictMode.HTTP_409 -> ConflictMode.HTTP_409
        BackendConflictMode.AUTO_MERGE -> ConflictMode.AUTO_MERGE
        BackendConflictMode.LAST_WRITE_WINS -> ConflictMode.LAST_WRITE_WINS
    }

private fun BackendSimulationSeedPreset.toServerSeedPreset(): SimulationSeedPreset =
    when (this) {
        BackendSimulationSeedPreset.RANDOM -> SimulationSeedPreset.RANDOM
        BackendSimulationSeedPreset.SEED_42 -> SimulationSeedPreset.SEED_42
        BackendSimulationSeedPreset.SEED_1337 -> SimulationSeedPreset.SEED_1337
        BackendSimulationSeedPreset.SEED_9001 -> SimulationSeedPreset.SEED_9001
    }
