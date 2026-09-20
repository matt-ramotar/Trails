package org.mobilenativefoundation.trails.foundation.networking

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*

actual fun getPlatformEngine(): HttpClientEngine = Js.create()
