package org.mobilenativefoundation.trails.server.error

sealed class ServerError : Exception() {
    data class BadRequest(override val message: String) : ServerError()

    data class Unauthorized(
        val reason: String = "Invalid or expired token",
    ) : ServerError()

    data class Forbidden(val resource: String) : ServerError()

    data class NotFound(
        val resourceType: String,
        val id: String,
    ) : ServerError()

    data class Conflict(
        val resourceType: String,
        val id: String,
        val clientVersion: Long,
        val serverVersion: Long,
    ) : ServerError()

    data class RateLimited(val retryAfterSeconds: Int) : ServerError()

    data class InternalError(override val message: String) : ServerError()

    data object ServiceUnavailable : ServerError()

    data class Timeout(val operationName: String) : ServerError()

    data object NetworkUnavailable : ServerError()
}
