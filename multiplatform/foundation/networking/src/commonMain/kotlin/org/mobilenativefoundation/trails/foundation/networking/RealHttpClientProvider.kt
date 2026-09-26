package org.mobilenativefoundation.trails.foundation.networking

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds


@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealHttpClientProvider(
    private val config: NetworkConfig,
) : HttpClientProvider {

    override val client: HttpClient by lazy {
        HttpClient(getPlatformEngine()) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }

            install(SSE) {
                reconnectionTime = 3.seconds
                showCommentEvents()
                showRetryEvents()
            }

            if (config.enableLogging) {
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.BODY
                }
            }

            defaultRequest {
                url(config.baseUrl)
                contentType(ContentType.Application.Json)

                AuthTokenHolder.token?.let { token ->
                    headers.append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
    }
}