package org.mobilenativefoundation.trails.foundation.networking

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun getPlatformEngine(): HttpClientEngine = Darwin.create()
