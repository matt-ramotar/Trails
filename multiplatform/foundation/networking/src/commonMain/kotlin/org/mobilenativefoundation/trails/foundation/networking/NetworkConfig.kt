package org.mobilenativefoundation.trails.foundation.networking


expect fun getPlatformDevBaseUrl(): String

data class NetworkConfig(
    val baseUrl: String,
    val timeout: Long = 30_000L,
    val enableLogging: Boolean = true,
) {
    companion object {
        fun dev() = NetworkConfig(
            baseUrl = getPlatformDevBaseUrl(),
            enableLogging = true,
        )

        fun prod() = NetworkConfig(
            baseUrl = "https://api.trails.mattramotar.dev",
            enableLogging = false,
        )
    }
}
