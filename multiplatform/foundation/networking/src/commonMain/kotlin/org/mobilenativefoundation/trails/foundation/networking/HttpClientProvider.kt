package org.mobilenativefoundation.trails.foundation.networking

import io.ktor.client.*
import io.ktor.client.engine.HttpClientEngine

interface HttpClientProvider {
    val client: HttpClient
}

expect fun getPlatformEngine(): HttpClientEngine
