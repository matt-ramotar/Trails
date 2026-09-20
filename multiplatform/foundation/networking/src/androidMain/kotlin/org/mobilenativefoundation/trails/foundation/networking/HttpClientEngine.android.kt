package org.mobilenativefoundation.trails.foundation.networking

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun getPlatformEngine(): HttpClientEngine = OkHttp.create()
