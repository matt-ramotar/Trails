package org.mobilenativefoundation.trails.di.graph.app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.trails.data.devsettings.BackendConflictMode
import org.mobilenativefoundation.trails.data.devsettings.BackendSimulationSeedPreset
import org.mobilenativefoundation.trails.data.devsettings.ConflictStrategy
import org.mobilenativefoundation.trails.data.devsettings.DeveloperSettings
import org.mobilenativefoundation.trails.data.devsettings.DeveloperSettingsRepository
import org.mobilenativefoundation.trails.data.devsettings.toBackendConfig
import org.mobilenativefoundation.trails.foundation.logging.Logger
import org.mobilenativefoundation.trails.server.BackendConfig
import org.mobilenativefoundation.trails.server.fake.BackendControl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class BackendConfigSynchronizerTest {
    @Test
    fun waitsForBackendCompletionBeforeReportingAppliedSettings() = runTest {
        val repository = InMemorySettingsRepository()
        val completion = CompletableDeferred<Unit>()
        val backend = ControlledBackend { completion.await() }
        val synchronizer = BackendConfigSynchronizer(backgroundScope, repository, backend, Logger())

        runCurrent()

        assertEquals(listOf(repository.current.toBackendConfig()), backend.attempts)
        assertIs<BackendConfigSyncStatus.Applying>(synchronizer.status.value)
        completion.complete(Unit)
        runCurrent()
        assertEquals(BackendConfigSyncStatus.Applied(repository.current), synchronizer.status.value)
    }

    @Test
    fun backendFailureStaysVisibleUntilExplicitRetrySucceeds() = runTest {
        val repository = InMemorySettingsRepository()
        var fail = true
        val backend = ControlledBackend { if (fail) error("private diagnostic detail") }
        val synchronizer = BackendConfigSynchronizer(backgroundScope, repository, backend, Logger())

        runCurrent()

        assertEquals(BackendConfigSyncStatus.Failed("Runtime settings could not be applied."), synchronizer.status.value)
        assertEquals(1, backend.attempts.size, "A failure must not produce an unbounded retry loop")
        fail = false
        synchronizer.retry()
        runCurrent()
        assertEquals(2, backend.attempts.size)
        assertEquals(BackendConfigSyncStatus.Applied(repository.current), synchronizer.status.value)
    }

    @Test
    fun retryUsesLatestPersistedSettingsInsteadOfTheFailedSnapshot() = runTest {
        val repository = InMemorySettingsRepository()
        var fail = true
        val backend = ControlledBackend { if (fail) error("unavailable") }
        val synchronizer = BackendConfigSynchronizer(backgroundScope, repository, backend, Logger())
        runCurrent()
        repository.setOfflineMode(true)
        runCurrent()
        assertIs<BackendConfigSyncStatus.Failed>(synchronizer.status.value)

        fail = false
        synchronizer.retry()
        runCurrent()

        assertEquals(repository.current.toBackendConfig(), backend.attempts.last())
        assertEquals(BackendConfigSyncStatus.Applied(repository.current), synchronizer.status.value)
        assertEquals(3, backend.attempts.size)
    }

    @Test
    fun cancellationIsNotReportedAsAnApplicationFailure() = runTest {
        val backend = ControlledBackend { throw CancellationException("application scope ended") }
        val synchronizer = BackendConfigSynchronizer(backgroundScope, InMemorySettingsRepository(), backend, Logger())

        runCurrent()

        assertIs<BackendConfigSyncStatus.Applying>(synchronizer.status.value)
        assertEquals(1, backend.attempts.size)
    }

    @Test
    fun missingBackendStaysUnavailableWhenSettingsChangeOrRetryIsRequested() = runTest {
        val repository = InMemorySettingsRepository()
        val synchronizer = BackendConfigSynchronizer(backgroundScope, repository, null, Logger())
        runCurrent()
        assertIs<BackendConfigSyncStatus.Unavailable>(synchronizer.status.value)

        repository.setOfflineMode(true)
        synchronizer.retry()
        runCurrent()

        assertIs<BackendConfigSyncStatus.Unavailable>(synchronizer.status.value)
    }

    private class ControlledBackend(private val applyConfig: suspend () -> Unit) : BackendControl {
        val attempts = mutableListOf<BackendConfig>()
        override val lastResolvedSeed: Int? = null
        override suspend fun reset() = Unit
        override fun triggerConflict() = Unit
        override suspend fun updateConfig(config: BackendConfig) {
            attempts += config
            applyConfig()
        }
        override suspend fun replayLastRun() = Unit
        override suspend fun reseedCurrentRun() = Unit
    }

    private class InMemorySettingsRepository : DeveloperSettingsRepository {
        private val state = MutableStateFlow(DeveloperSettings())
        override val current: DeveloperSettings get() = state.value
        override fun stream() = state
        override suspend fun update(settings: DeveloperSettings) { state.value = settings }
        override suspend fun setOfflineMode(enabled: Boolean) = update(current.copy(offlineMode = enabled))
        override suspend fun setConflictsEnabled(enabled: Boolean) = update(current.copy(conflictsEnabled = enabled))
        override suspend fun setConflictStrategy(strategy: ConflictStrategy) = update(current.copy(conflictStrategy = strategy))
        override suspend fun setLatencyRange(minMs: Long, maxMs: Long) = update(current.copy(latencyMinMs = minMs, latencyMaxMs = maxMs))
        override suspend fun setErrorRate(rate: Float) = update(current.copy(errorRate = rate))
        override suspend fun setRateLimitPerMinute(limit: Int) = update(current.copy(rateLimitPerMinute = limit))
        override suspend fun setBackendConflictMode(mode: BackendConflictMode) = update(current.copy(backendConflictMode = mode))
        override suspend fun setConflictProbability(probability: Float) = update(current.copy(conflictProbability = probability))
        override suspend fun setSimulationSeedPreset(seedPreset: BackendSimulationSeedPreset) = update(current.copy(simulationSeedPreset = seedPreset))
        override suspend fun resetToDefaults() = update(DeveloperSettings())
    }
}
