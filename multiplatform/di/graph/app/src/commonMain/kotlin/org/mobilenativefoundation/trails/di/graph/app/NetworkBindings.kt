package org.mobilenativefoundation.trails.di.graph.app

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import io.ktor.client.*
import org.mobilenativefoundation.trails.foundation.networking.HttpClientProvider
import org.mobilenativefoundation.trails.foundation.networking.NetworkConfig

@BindingContainer
object NetworkBindings {
    @Provides
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig.dev()

    @Provides
    fun provideHttpClient(provider: HttpClientProvider): HttpClient = provider.client
}