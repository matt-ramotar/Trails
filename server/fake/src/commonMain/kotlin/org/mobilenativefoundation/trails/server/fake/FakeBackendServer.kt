package org.mobilenativefoundation.trails.server.fake

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.trails.server.BackendClock
import org.mobilenativefoundation.trails.server.BackendConfig
import org.mobilenativefoundation.trails.server.BackendConfigProvider
import org.mobilenativefoundation.trails.server.BackendServer
import org.mobilenativefoundation.trails.server.SimulationSeedPreset
import org.mobilenativefoundation.trails.server.SystemBackendClock
import org.mobilenativefoundation.trails.server.fake.internal.seed.SeedDataLoader
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ConflictSimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ErrorSimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.LatencySimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.NetworkGate
import org.mobilenativefoundation.trails.server.fake.internal.simulation.SimulationRandomSource
import org.mobilenativefoundation.trails.server.fake.internal.storage.BackendTables
import org.mobilenativefoundation.trails.server.fake.internal.storage.InMemoryBackendStore
import org.mobilenativefoundation.trails.server.fake.services.FakeFeedService
import org.mobilenativefoundation.trails.server.fake.services.FakePostService
import org.mobilenativefoundation.trails.server.fake.services.FakeResortService
import org.mobilenativefoundation.trails.server.fake.services.FakeRunService
import org.mobilenativefoundation.trails.server.fake.services.FakeUserService
import org.mobilenativefoundation.trails.server.fake.services.FakeWeatherService
import org.mobilenativefoundation.trails.server.services.FeedService
import org.mobilenativefoundation.trails.server.services.PostService
import org.mobilenativefoundation.trails.server.services.ResortService
import org.mobilenativefoundation.trails.server.services.RunService
import org.mobilenativefoundation.trails.server.services.UserService
import org.mobilenativefoundation.trails.server.services.WeatherService
import kotlin.random.Random

class FakeBackendServer(
    initialConfig: BackendConfig = BackendConfig(),
    private val clock: BackendClock = SystemBackendClock,
) : BackendServer, BackendControl {

    private val runMutex = Mutex()
    private val initialResolvedSeed = resolveSeed(initialConfig.simulationSeedPreset)
    private var lastRunSnapshot: SimulationRunSnapshot? = SimulationRunSnapshot(initialConfig, initialResolvedSeed)

    private val configFlow = MutableStateFlow(initialConfig)
    private val configProvider = object : BackendConfigProvider {
        override val current: BackendConfig
            get() = configFlow.value

        override val flow: StateFlow<BackendConfig> = configFlow

        override fun update(config: BackendConfig) {
            configFlow.value = config
        }
    }

    private val store = InMemoryBackendStore()
    private val tables = BackendTables(store)
    private val seedLoader = SeedDataLoader(tables)
    private val randomSource = SimulationRandomSource(initialResolvedSeed)

    private val networkGate = NetworkGate(configProvider)
    private val latencySimulator = LatencySimulator(configProvider, randomSource)
    private val errorSimulator = ErrorSimulator(configProvider, clock, randomSource)
    private val conflictSimulator = ConflictSimulator(
        mode = configProvider.current.conflictMode,
        probability = configProvider.current.conflictProbability,
        randomSource = randomSource,
    )

    override val userService: UserService by lazy {
        FakeUserService(
            tables = tables,
            networkGate = networkGate,
            latencySimulator = latencySimulator,
            errorSimulator = errorSimulator,
            seedLoader = seedLoader,
            configProvider = configProvider,
            clock = clock,
            conflictSimulator = conflictSimulator,
        )
    }

    override val feedService: FeedService by lazy {
        FakeFeedService(
            tables = tables,
            networkGate = networkGate,
            latencySimulator = latencySimulator,
            errorSimulator = errorSimulator,
            seedLoader = seedLoader,
            configProvider = configProvider,
        )
    }

    override val postService: PostService by lazy {
        FakePostService(
            tables = tables,
            networkGate = networkGate,
            latencySimulator = latencySimulator,
            errorSimulator = errorSimulator,
            conflictSimulator = conflictSimulator,
            seedLoader = seedLoader,
            configProvider = configProvider,
            clock = clock,
        )
    }

    override val resortService: ResortService by lazy {
        FakeResortService(
            tables = tables,
            networkGate = networkGate,
            latencySimulator = latencySimulator,
            errorSimulator = errorSimulator,
            seedLoader = seedLoader,
            configProvider = configProvider,
        )
    }

    override val runService: RunService by lazy {
        FakeRunService(
            tables = tables,
            networkGate = networkGate,
            latencySimulator = latencySimulator,
            errorSimulator = errorSimulator,
            seedLoader = seedLoader,
            configProvider = configProvider,
        )
    }

    override val weatherService: WeatherService by lazy {
        FakeWeatherService(
            tables = tables,
            networkGate = networkGate,
            latencySimulator = latencySimulator,
            errorSimulator = errorSimulator,
            seedLoader = seedLoader,
            configProvider = configProvider,
        )
    }

    override suspend fun reset() = runMutex.withLock {
        store.clear()
        conflictSimulator.reset()
        errorSimulator.reset()
        seedLoader.reset()
        seedLoader.ensureSeeded()
    }

    override fun triggerConflict() {
        conflictSimulator.triggerNextConflict()
    }

    override suspend fun updateConfig(config: BackendConfig) = runMutex.withLock {
        val current = configProvider.current
        val resolvedSeed = when {
            config.simulationSeedPreset != current.simulationSeedPreset -> resolveSeed(config.simulationSeedPreset)
            else -> lastRunSnapshot?.resolvedSeed ?: resolveSeed(config.simulationSeedPreset)
        }
        applyRun(config, resolvedSeed)
    }

    override suspend fun replayLastRun() = runMutex.withLock {
        val snapshot = lastRunSnapshot ?: return@withLock
        applyRun(snapshot.config, snapshot.resolvedSeed, rememberRun = false)
    }

    override suspend fun reseedCurrentRun() = runMutex.withLock {
        val current = configProvider.current
        val resolvedSeed = resolveSeed(current.simulationSeedPreset)
        applyRun(current, resolvedSeed)
    }

    override val lastResolvedSeed: Int?
        get() = lastRunSnapshot?.resolvedSeed

    private suspend fun applyRun(
        config: BackendConfig,
        resolvedSeed: Int,
        rememberRun: Boolean = true,
    ) {
        configProvider.update(config)
        randomSource.reseed(resolvedSeed)
        conflictSimulator.updateMode(config.conflictMode, config.conflictProbability)
        if (rememberRun) {
            lastRunSnapshot = SimulationRunSnapshot(config, resolvedSeed)
        }
        seedLoader.ensureSeeded()
    }

    private fun resolveSeed(preset: SimulationSeedPreset): Int =
        when (preset) {
            SimulationSeedPreset.RANDOM -> Random.nextInt(1, Int.MAX_VALUE)
            SimulationSeedPreset.SEED_42 -> 42
            SimulationSeedPreset.SEED_1337 -> 1337
            SimulationSeedPreset.SEED_9001 -> 9001
        }
}

private data class SimulationRunSnapshot(
    val config: BackendConfig,
    val resolvedSeed: Int,
)
