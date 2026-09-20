package org.mobilenativefoundation.trails.di.graph.app

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mobilenativefoundation.trails.data.devsettings.DeveloperSettingsRepository
import org.mobilenativefoundation.trails.data.devsettings.toBackendConfig
import org.mobilenativefoundation.trails.foundation.logging.Logger
import org.mobilenativefoundation.trails.server.fake.BackendControl

@SingleIn(AppScope::class)
@Inject
class BackendConfigSynchronizer(
    @Named("AppCoroutineScope") appScope: CoroutineScope,
    private val developerSettingsRepository: DeveloperSettingsRepository,
    private val backendControl: BackendControl?,
    private val logger: Logger,
) {
    private val retryRequests = MutableStateFlow(0L)
    private val mutableStatus = MutableStateFlow<BackendConfigSyncStatus>(
        if (backendControl == null) BackendConfigSyncStatus.Unavailable else BackendConfigSyncStatus.Applying,
    )
    val status: StateFlow<BackendConfigSyncStatus> = mutableStatus.asStateFlow()

    init {
        appScope.launch {
            developerSettingsRepository.readFailures().collect { message ->
                if (message != null) mutableStatus.value = BackendConfigSyncStatus.Failed(message)
            }
        }
        appScope.launch {
            combine(
                developerSettingsRepository.stream().distinctUntilChanged(),
                developerSettingsRepository.readFailures(),
                retryRequests,
            ) { settings, readFailure, _ -> settings to readFailure }
                .collectLatest { (settings, readFailure) ->
                    if (readFailure != null) {
                        mutableStatus.value = BackendConfigSyncStatus.Failed(readFailure)
                        return@collectLatest
                    }
                    val control = backendControl ?: return@collectLatest
                    mutableStatus.value = BackendConfigSyncStatus.Applying
                    try {
                        control.updateConfig(settings.toBackendConfig())
                        mutableStatus.value = BackendConfigSyncStatus.Applied(settings)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        mutableStatus.value = BackendConfigSyncStatus.Failed("Runtime settings could not be applied.")
                        logger.error(TAG, "Failed to sync backend config.", failure)
                    }
                }
        }
    }

    fun retry() {
        developerSettingsRepository.retryRead()
        retryRequests.update { it + 1L }
    }

    private companion object {
        private const val TAG = "BackendConfigSync"
    }
}
