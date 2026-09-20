package org.mobilenativefoundation.trails.server.fake

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.trails.server.BackendClock
import org.mobilenativefoundation.trails.server.BackendConfig
import org.mobilenativefoundation.trails.server.BackendConfigProvider
import org.mobilenativefoundation.trails.server.ConflictMode
import org.mobilenativefoundation.trails.server.NetworkMode
import org.mobilenativefoundation.trails.server.SimulationSeedPreset
import org.mobilenativefoundation.trails.server.error.ServerError
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ConflictOutcome
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ConflictSimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ErrorSimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.LatencySimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.SimulationRandomSource
import org.mobilenativefoundation.trails.server.model.ProfileUpdate
import org.mobilenativefoundation.trails.server.model.SkiRunPostRecord
import org.mobilenativefoundation.trails.server.model.UpdatePostRequest
import org.mobilenativefoundation.trails.server.pagination.CursorRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SimulationContractTest {
    @Test
    fun fixedSeeds_reproduceMixedLatencyErrorAndConflictSequences() = runTest {
        val traces = listOf(42, 1337, 9001).map { seed ->
            val first = simulationTrace(SimulationRandomSource(seed))
            val second = simulationTrace(SimulationRandomSource(seed))
            assertEquals(first, second)
            assertTrue(first.all { it.latencyMs in 10L..30L })
            assertTrue(first.any { it.error == null })
            assertTrue(first.any { it.error != null })
            assertTrue(first.any { it.conflict is ConflictOutcome.Reject })
            assertTrue(first.any { it.conflict is ConflictOutcome.Proceed })
            first
        }
        assertEquals(3, traces.distinct().size)
    }

    @Test
    fun reseed_restartsTheSharedSimulationSequence() = runTest {
        val random = SimulationRandomSource(42)
        val first = simulationTrace(random)
        assertNotEquals(first, simulationTrace(random))

        random.reseed(42)
        assertEquals(42, random.currentSeed())
        assertEquals(first, simulationTrace(random))

        random.reseed(1337)
        assertEquals(1337, random.currentSeed())
        assertEquals(simulationTrace(SimulationRandomSource(1337)), simulationTrace(random))
    }

    @Test
    fun errorInjection_respectsZeroAndCertainProbability() = runTest {
        val config = MutableConfigProvider(BackendConfig(errorRate = 0f))
        val errors = ErrorSimulator(config, FixedClock, SimulationRandomSource(42))
        repeat(10) { assertNull(errors.shouldError()) }

        config.update(config.current.copy(errorRate = 1f))
        repeat(10) {
            val error = assertNotNull(errors.shouldError())
            assertTrue(
                error is ServerError.InternalError || error is ServerError.ServiceUnavailable || error is ServerError.Timeout,
            )
        }
    }

    @Test
    fun latencyInjection_supportsZeroFixedAndReversedRanges() = runTest {
        val config = MutableConfigProvider(FAST_CONFIG)
        val latency = LatencySimulator(config, SimulationRandomSource(42))
        val start = currentTime
        assertEquals("done", latency.withLatency { "done" })
        assertEquals(start, currentTime)

        config.update(config.current.copy(latencyRange = 20.milliseconds..20.milliseconds))
        latency.withLatency { }
        assertEquals(start + 20, currentTime)

        config.update(config.current.copy(latencyRange = 30.milliseconds..10.milliseconds))
        val beforeReversed = currentTime
        latency.withLatency { }
        assertTrue(currentTime - beforeReversed in 10L..30L)
    }

    @Test
    fun disabledInjection_allowsCurrentVersionsButRejectsRealStaleVersions() = runTest {
        val conflicts = ConflictSimulator(ConflictMode.DISABLED, 1f, SimulationRandomSource(42))
        conflicts.triggerNextConflict()
        assertEquals(ConflictOutcome.Proceed, conflicts.evaluate("post", "post_1", 2, 2))

        val rejected = assertIs<ConflictOutcome.Reject>(conflicts.evaluate("post", "post_1", 1, 2))
        assertEquals(ServerError.Conflict("post", "post_1", 1, 2), rejected.error)
    }

    @Test
    fun forcedConflict_isOneShotAndReportsTheActualServerVersion() = runTest {
        val conflicts = ConflictSimulator(ConflictMode.HTTP_409, 0f, SimulationRandomSource(42))
        assertEquals(ConflictOutcome.Proceed, conflicts.evaluate("post", "post_1", 2, 2))
        conflicts.triggerNextConflict()

        val rejected = assertIs<ConflictOutcome.Reject>(conflicts.evaluate("post", "post_1", 2, 2))
        assertEquals(ServerError.Conflict("post", "post_1", 2, 2), rejected.error)
        assertEquals(ConflictOutcome.Proceed, conflicts.evaluate("post", "post_1", 2, 2))

        conflicts.triggerNextConflict()
        conflicts.reset()
        assertEquals(ConflictOutcome.Proceed, conflicts.evaluate("post", "post_1", 2, 2))
    }

    @Test
    fun postService_rejectsStaleWritesWithInjectionDisabledOrWithHttp409Mode() = runTest {
        for (mode in listOf(ConflictMode.DISABLED, ConflictMode.HTTP_409)) {
            val server = FakeBackendServer(FAST_CONFIG.copy(conflictMode = mode), FixedClock)
            val token = server.userService.login("matt@example.com", "password").accessToken
            val original = authoredPost(server)
            val accepted = server.postService.updatePost(
                token, original.id, UpdatePostRequest(caption = "server edit", version = original.version),
            )
            assertEquals("server edit", accepted.caption)
            assertEquals(original.version + 1, accepted.version)

            val rejected = assertFailsWith<ServerError.Conflict> {
                server.postService.updatePost(
                    token, original.id, UpdatePostRequest(caption = "stale edit", version = original.version),
                )
            }
            assertEquals(accepted.version, rejected.serverVersion)
            val persisted = server.postService.getPost(original.id)
            assertEquals("server edit", persisted.caption)
            assertEquals(accepted.version, persisted.version)
        }
    }

    @Test
    fun forcedPostConflict_leavesServerUnchangedAndAllowsTheNextRetry() = runTest {
        val server = FakeBackendServer(FAST_CONFIG.copy(conflictMode = ConflictMode.HTTP_409), FixedClock)
        val token = server.userService.login("matt@example.com", "password").accessToken
        val original = authoredPost(server)
        server.triggerConflict()

        val rejected = assertFailsWith<ServerError.Conflict> {
            server.postService.updatePost(
                token, original.id, UpdatePostRequest(caption = "client intent", version = original.version),
            )
        }
        assertEquals(original.version, rejected.serverVersion)
        assertEquals(original.caption, server.postService.getPost(original.id).caption)
        val accepted = server.postService.updatePost(
            token, original.id, UpdatePostRequest(caption = "client intent", version = original.version),
        )
        assertEquals("client intent", accepted.caption)
        assertEquals(original.version + 1, accepted.version)
        assertEquals(accepted.caption, server.postService.getPost(original.id).caption)
    }

    @Test
    fun explicitServerMergeModes_preserveTheirResolutionSemantics() = runTest {
        for (mode in listOf(ConflictMode.AUTO_MERGE, ConflictMode.LAST_WRITE_WINS)) {
            val server = FakeBackendServer(FAST_CONFIG.copy(conflictMode = mode), FixedClock)
            val token = server.userService.login("matt@example.com", "password").accessToken
            val original = authoredPost(server)
            val current = server.postService.updatePost(
                token, original.id,
                UpdatePostRequest(caption = "server edit", styleId = "server_style", version = original.version),
            )
            val resolved = server.postService.updatePost(
                token, original.id,
                UpdatePostRequest(caption = "client edit", styleId = "client_style", version = original.version),
            )

            assertEquals(current.version + 1, resolved.version)
            if (mode == ConflictMode.AUTO_MERGE) {
                assertEquals("server edit | client edit", resolved.caption)
                assertEquals(current.style.backgroundGradientId, resolved.style.backgroundGradientId)
            } else {
                assertEquals("client edit", resolved.caption)
                assertEquals("client_style", resolved.style.backgroundGradientId.value)
            }
            assertEquals(resolved, server.postService.getPost(original.id, token))
        }
    }

    @Test
    fun seedPresetsAndReplay_preserveResolvedSeeds() = runTest {
        for ((preset, seed) in listOf(
            SimulationSeedPreset.SEED_42 to 42,
            SimulationSeedPreset.SEED_1337 to 1337,
            SimulationSeedPreset.SEED_9001 to 9001,
        )) {
            val server = FakeBackendServer(FAST_CONFIG.copy(simulationSeedPreset = preset), FixedClock)
            assertEquals(seed, server.lastResolvedSeed)
            server.reseedCurrentRun()
            assertEquals(seed, server.lastResolvedSeed)
            server.replayLastRun()
            assertEquals(seed, server.lastResolvedSeed)
        }
        val randomServer = FakeBackendServer(FAST_CONFIG.copy(simulationSeedPreset = SimulationSeedPreset.RANDOM), FixedClock)
        val seed = assertNotNull(randomServer.lastResolvedSeed)
        randomServer.updateConfig(FAST_CONFIG.copy(simulationSeedPreset = SimulationSeedPreset.RANDOM, errorRate = 0.2f))
        assertEquals(seed, randomServer.lastResolvedSeed)
        randomServer.replayLastRun()
        assertEquals(seed, randomServer.lastResolvedSeed)
    }

    @Test
    fun settingsReplay_repeatsSequentialNetworkSimulationWithoutRewindingServerData() = runTest {
        val server = FakeBackendServer(FAST_CONFIG, FixedClock)
        val token = server.userService.login("matt@example.com", "password").accessToken
        val original = authoredPost(server)
        val edited = server.postService.updatePost(
            token, original.id, UpdatePostRequest(caption = "retained edit", version = original.version),
        )
        server.userService.updateProfile(token, ProfileUpdate(displayName = "Retained profile"))
        val config = FAST_CONFIG.copy(latencyRange = 10.milliseconds..30.milliseconds, errorRate = 0.45f)
        server.updateConfig(config)
        val first = serverTrace(server)
        assertTrue(first.any { it.error != null })
        assertTrue(first.any { it.error == null })

        server.replayLastRun()
        assertEquals(first, serverTrace(server))
        server.reseedCurrentRun()
        assertEquals(first, serverTrace(server))

        server.updateConfig(FAST_CONFIG)
        val persisted = server.postService.getPost(original.id, token)
        assertEquals(edited.caption, persisted.caption)
        assertEquals(edited.version, persisted.version)
        assertEquals("Retained profile", assertNotNull(server.userService.getCurrentUser(token).profile).displayName)
    }

    @Test
    fun replay_preservesRateLimitHistoryBecauseItDoesNotRewindTheServer() = runTest {
        val server = FakeBackendServer(FAST_CONFIG.copy(rateLimitRequestsPerMinute = 1), FixedClock)
        server.feedService.getTrendingFeed(CursorRequest(limit = 1))
        server.replayLastRun()

        assertFailsWith<ServerError.RateLimited> {
            server.feedService.getTrendingFeed(CursorRequest(limit = 1))
        }
    }

    @Test
    fun offlineMode_blocksRequestsBeforeLatencyOrServerMutation() = runTest {
        val server = FakeBackendServer(FAST_CONFIG, FixedClock)
        val token = server.userService.login("matt@example.com", "password").accessToken
        val original = authoredPost(server)
        server.updateConfig(FAST_CONFIG.copy(networkMode = NetworkMode.OFFLINE, latencyRange = 20.milliseconds..20.milliseconds))
        val start = currentTime

        assertFailsWith<ServerError.NetworkUnavailable> {
            server.postService.updatePost(token, original.id, UpdatePostRequest(caption = "offline edit", version = original.version))
        }
        assertEquals(start, currentTime)
        server.updateConfig(FAST_CONFIG)
        assertEquals(original, server.postService.getPost(original.id))
    }

    private suspend fun TestScope.simulationTrace(random: SimulationRandomSource): List<SimulationSample> {
        val config = MutableConfigProvider(FAST_CONFIG.copy(latencyRange = 10.milliseconds..30.milliseconds, errorRate = 0.45f))
        val errors = ErrorSimulator(config, FixedClock, random)
        val latency = LatencySimulator(config, random)
        val conflicts = ConflictSimulator(ConflictMode.HTTP_409, 0.5f, random)
        return List(24) {
            val start = currentTime
            latency.withLatency { }
            SimulationSample(currentTime - start, errors.shouldError(), conflicts.evaluate("post", "post_1", 1, 1))
        }
    }

    private suspend fun TestScope.serverTrace(server: FakeBackendServer): List<NetworkSample> = List(24) {
        val start = currentTime
        val error = try {
            server.feedService.getTrendingFeed(CursorRequest(limit = 1))
            null
        } catch (error: ServerError) {
            error
        }
        NetworkSample(currentTime - start, error)
    }

    private suspend fun authoredPost(server: FakeBackendServer): SkiRunPostRecord =
        server.feedService.getUserFeed("author_matt", CursorRequest(limit = 1)).items.single() as SkiRunPostRecord

    private data class SimulationSample(val latencyMs: Long, val error: ServerError?, val conflict: ConflictOutcome)
    private data class NetworkSample(val latencyMs: Long, val error: ServerError?)

    private class MutableConfigProvider(initial: BackendConfig) : BackendConfigProvider {
        private val state = MutableStateFlow(initial)
        override val current: BackendConfig get() = state.value
        override val flow: StateFlow<BackendConfig> = state
        override fun update(config: BackendConfig) { state.value = config }
    }

    private object FixedClock : BackendClock {
        override fun nowMs(): Long = 1736596800000L
    }

    private companion object {
        private val FAST_CONFIG = BackendConfig(
            latencyRange = 0.milliseconds..0.milliseconds,
            simulationSeedPreset = SimulationSeedPreset.SEED_42,
        )
    }
}
