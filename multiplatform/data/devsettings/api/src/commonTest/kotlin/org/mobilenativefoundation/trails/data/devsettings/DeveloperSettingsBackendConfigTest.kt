package org.mobilenativefoundation.trails.data.devsettings

import org.mobilenativefoundation.trails.server.ConflictMode
import org.mobilenativefoundation.trails.server.NetworkMode
import org.mobilenativefoundation.trails.server.SimulationSeedPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class DeveloperSettingsBackendConfigTest {
    @Test
    fun defaultsProduceOnlineNonInjectingRandomConfiguration() {
        val config = DeveloperSettings().toBackendConfig()

        assertEquals(NetworkMode.ONLINE, config.networkMode)
        assertEquals(ConflictMode.DISABLED, config.conflictMode)
        assertEquals(SimulationSeedPreset.RANDOM, config.simulationSeedPreset)
        assertEquals(50.milliseconds..200.milliseconds, config.latencyRange)
        assertEquals(0f, config.errorRate)
        assertEquals(0f, config.conflictProbability)
        assertEquals(0, config.rateLimitRequestsPerMinute)
    }

    @Test
    fun everyConflictModeTranslatesAndDisabledInjectionOverridesSelection() {
        val expected = listOf(
            BackendConflictMode.DISABLED to ConflictMode.DISABLED,
            BackendConflictMode.HTTP_409 to ConflictMode.HTTP_409,
            BackendConflictMode.AUTO_MERGE to ConflictMode.AUTO_MERGE,
            BackendConflictMode.LAST_WRITE_WINS to ConflictMode.LAST_WRITE_WINS,
        )
        for ((setting, serverMode) in expected) {
            val enabled = DeveloperSettings(conflictsEnabled = true, backendConflictMode = setting)
            assertEquals(serverMode, enabled.toBackendConfig().conflictMode)
            assertEquals(ConflictMode.DISABLED, enabled.copy(conflictsEnabled = false).toBackendConfig().conflictMode)
        }
    }

    @Test
    fun everySeedPresetTranslates() {
        val expected = listOf(
            BackendSimulationSeedPreset.RANDOM to SimulationSeedPreset.RANDOM,
            BackendSimulationSeedPreset.SEED_42 to SimulationSeedPreset.SEED_42,
            BackendSimulationSeedPreset.SEED_1337 to SimulationSeedPreset.SEED_1337,
            BackendSimulationSeedPreset.SEED_9001 to SimulationSeedPreset.SEED_9001,
        )
        for ((setting, serverPreset) in expected) {
            assertEquals(serverPreset, DeveloperSettings(simulationSeedPreset = setting).toBackendConfig().simulationSeedPreset)
        }
    }

    @Test
    fun offlineAndNumericBoundsTranslate() {
        val config = DeveloperSettings(
            offlineMode = true, errorRate = 2f, conflictProbability = -1f, rateLimitPerMinute = -1,
        ).toBackendConfig()

        assertEquals(NetworkMode.OFFLINE, config.networkMode)
        assertEquals(1f, config.errorRate)
        assertEquals(0f, config.conflictProbability)
        assertEquals(0, config.rateLimitRequestsPerMinute)
    }
}
