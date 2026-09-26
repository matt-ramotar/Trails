package org.mobilenativefoundation.trails.feature.developertools

import org.mobilenativefoundation.trails.data.developersettings.DeveloperSettings

sealed interface BackendConfigSyncStatus {
    data object Unavailable : BackendConfigSyncStatus
    data object Applying : BackendConfigSyncStatus
    data class Applied(val settings: DeveloperSettings) : BackendConfigSyncStatus
    data class Failed(val message: String) : BackendConfigSyncStatus
}
