package org.mobilenativefoundation.trails.server

import kotlinx.coroutines.flow.StateFlow

interface BackendConfigProvider {
    val current: BackendConfig
    val flow: StateFlow<BackendConfig>
    fun update(config: BackendConfig)
}
