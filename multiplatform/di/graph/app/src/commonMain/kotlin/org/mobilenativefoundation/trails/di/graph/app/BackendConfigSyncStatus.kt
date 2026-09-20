package org.mobilenativefoundation.trails.di.graph.app

import org.mobilenativefoundation.trails.data.devsettings.DeveloperSettings

sealed interface BackendConfigSyncStatus {
    data object Unavailable : BackendConfigSyncStatus
    data object Applying : BackendConfigSyncStatus
    data class Applied(val settings: DeveloperSettings) : BackendConfigSyncStatus
    data class Failed(val message: String) : BackendConfigSyncStatus
}
