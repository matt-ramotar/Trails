package org.mobilenativefoundation.trails.app.runtime.graph.app

import org.mobilenativefoundation.trails.feature.developertools.*

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.trails.data.developersettings.BackendConflictMode
import org.mobilenativefoundation.trails.data.developersettings.BackendSimulationSeedPreset
import org.mobilenativefoundation.trails.data.developersettings.DeveloperSettings
import org.mobilenativefoundation.trails.data.developersettings.DeveloperSettingsRepository
import org.mobilenativefoundation.trails.data.developersettings.toBackendConfig
import org.mobilenativefoundation.trails.foundation.logging.Logger
import org.mobilenativefoundation.trails.data.backend.BackendConfig
import org.mobilenativefoundation.trails.data.trail.account.TrailDataFactory
import org.mobilenativefoundation.trails.data.trail.account.TrailAccount
import org.mobilenativefoundation.trails.data.trail.catalog.TrailRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class BackendConfigSynchronizerTest {
    @Test
    fun waitsForBackendCompletionBeforeReportingAppliedSettings() = runTest {
        val repository = InMemorySettingsRepository()
        val completion = CompletableDeferred<Unit>()
        val backend = ControlledTrailData { completion.await() }
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
        val backend = ControlledTrailData { if (fail) error("private diagnostic detail") }
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
        val backend = ControlledTrailData { if (fail) error("unavailable") }
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
        val backend = ControlledTrailData { throw CancellationException("application scope ended") }
        val synchronizer = BackendConfigSynchronizer(backgroundScope, InMemorySettingsRepository(), backend, Logger())

        runCurrent()

        assertIs<BackendConfigSyncStatus.Applying>(synchronizer.status.value)
        assertEquals(1, backend.attempts.size)
    }

    private class ControlledTrailData(private val applyConfig: suspend () -> Unit) : TrailDataFactory {
        val attempts = mutableListOf<BackendConfig>()
        override val trails: TrailRepository get() = error("Catalog is not used by the synchronizer")
        override val backendConfig = MutableStateFlow<BackendConfig?>(null)
        override suspend fun restoreBackendConfig(): BackendConfig = requireNotNull(backendConfig.value)
        override suspend fun open(accountId: String): TrailAccount = error("Accounts are not used by the synchronizer")
        override suspend fun close() = Unit
        override suspend fun applyBackendConfig(config: BackendConfig) {
            attempts += config
            applyConfig()
            backendConfig.value = config
        }
    }

    private class InMemorySettingsRepository : DeveloperSettingsRepository {
        private val state = MutableStateFlow(DeveloperSettings())
        override val current: DeveloperSettings get() = state.value
        override fun stream() = state
        override suspend fun update(settings: DeveloperSettings) { state.value = settings }
        override suspend fun setOfflineMode(enabled: Boolean) = update(current.copy(offlineMode = enabled))
        override suspend fun setConflictsEnabled(enabled: Boolean) = update(current.copy(conflictsEnabled = enabled))
        override suspend fun setLatencyRange(minMs: Long, maxMs: Long) = update(current.copy(latencyMinMs = minMs, latencyMaxMs = maxMs))
        override suspend fun setErrorRate(rate: Float) = update(current.copy(errorRate = rate))
        override suspend fun setRateLimitPerMinute(limit: Int) = update(current.copy(rateLimitPerMinute = limit))
        override suspend fun setBackendConflictMode(mode: BackendConflictMode) = update(current.copy(backendConflictMode = mode))
        override suspend fun setConflictProbability(probability: Float) = update(current.copy(conflictProbability = probability))
        override suspend fun setSimulationSeedPreset(seedPreset: BackendSimulationSeedPreset) = update(current.copy(simulationSeedPreset = seedPreset))
        override suspend fun resetToDefaults() = update(DeveloperSettings())
    }
}
