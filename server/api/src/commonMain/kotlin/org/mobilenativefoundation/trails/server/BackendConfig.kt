package org.mobilenativefoundation.trails.server

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

enum class NetworkMode {
    ONLINE,
    OFFLINE,
}

data class BackendConfig(
    // Network simulation
    val networkMode: NetworkMode = NetworkMode.ONLINE,
    val latencyRange: ClosedRange<Duration> = 50.milliseconds..200.milliseconds,
    val errorRate: Float = 0.0f, // 0.0 to 1.0
    val rateLimitRequestsPerMinute: Int = 0, // 0 = disabled

    // Conflict simulation
    val conflictMode: ConflictMode = ConflictMode.DISABLED,
    val conflictProbability: Float = 0.0f,

    // Simulation seed behavior
    val simulationSeedPreset: SimulationSeedPreset = SimulationSeedPreset.RANDOM,

    // Pagination defaults
    val defaultPageSize: Int = 20,
    val maxPageSize: Int = 100,
)

enum class ConflictMode {
    DISABLED,
    HTTP_409,
    AUTO_MERGE,
    LAST_WRITE_WINS,
}

enum class SimulationSeedPreset {
    RANDOM,
    SEED_42,
    SEED_1337,
    SEED_9001,
}
