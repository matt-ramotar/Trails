package org.mobilenativefoundation.trails.di.graph.app

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.mobilenativefoundation.trails.data.devsettings.DeveloperSettingsRepository
import org.mobilenativefoundation.trails.data.devsettings.toBackendConfig
import org.mobilenativefoundation.trails.data.trail.TrailDataFactory
import org.mobilenativefoundation.trails.server.BackendConfig
import org.mobilenativefoundation.trails.server.BackendServer
import org.mobilenativefoundation.trails.server.fake.BackendControl
import org.mobilenativefoundation.trails.server.fake.FakeBackendServer

@BindingContainer
object BackendBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideBackendServer(
        developerSettingsRepository: DeveloperSettingsRepository,
    ): BackendServer = FakeBackendServer(
        initialConfig = developerSettingsRepository.current.toBackendConfig(),
    )

    @Provides
    fun provideBackendControl(server: BackendServer, trailData: TrailDataFactory): BackendControl? {
        val control = server as? BackendControl ?: return null
        return object : BackendControl by control {
            override suspend fun updateConfig(config: BackendConfig) {
                control.updateConfig(config)
                trailData.applyBackendConfig(config)
            }
        }
    }
}
