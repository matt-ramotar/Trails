package org.mobilenativefoundation.trails.data.trail.account

import kotlinx.coroutines.flow.StateFlow
import org.mobilenativefoundation.trails.data.backend.BackendConfig
import org.mobilenativefoundation.trails.data.trail.catalog.TrailRepository

interface TrailDataFactory {
    val trails: TrailRepository
    val backendConfig: StateFlow<BackendConfig?>
    suspend fun restoreBackendConfig(): BackendConfig
    suspend fun applyBackendConfig(config: BackendConfig)
    suspend fun open(accountId: String): TrailAccount
    suspend fun close()
}
