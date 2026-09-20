package org.mobilenativefoundation.trails.server.fake

import org.mobilenativefoundation.trails.server.BackendConfig

interface BackendControl {
    suspend fun reset()
    fun triggerConflict()
    suspend fun updateConfig(config: BackendConfig)
    suspend fun replayLastRun()
    suspend fun reseedCurrentRun()
    val lastResolvedSeed: Int?
}
