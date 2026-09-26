package org.mobilenativefoundation.trails.server.fake.internal.simulation

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.trails.server.BackendClock
import org.mobilenativefoundation.trails.server.BackendConfigProvider
import org.mobilenativefoundation.trails.server.error.ServerError

class ErrorSimulator(
    private val configProvider: BackendConfigProvider,
    private val clock: BackendClock,
    private val randomSource: SimulationRandomSource,
) {
    private val mutex = Mutex()
    private val requestTimestamps = ArrayDeque<Long>()

    suspend fun shouldError(): ServerError? = mutex.withLock {
        val config = configProvider.current

        if (config.rateLimitRequestsPerMinute > 0) {
            val now = clock.nowMs()
            val oneMinuteAgo = now - 60_000
            while (requestTimestamps.isNotEmpty() && requestTimestamps.first() < oneMinuteAgo) {
                requestTimestamps.removeFirst()
            }
            if (requestTimestamps.size >= config.rateLimitRequestsPerMinute) {
                return ServerError.RateLimited(retryAfterSeconds = 60)
            }
            requestTimestamps.addLast(now)
        }

        if (config.errorRate > 0 && randomSource.nextFloat() < config.errorRate) {
            return randomError()
        }

        return null
    }

    fun reset() {
        requestTimestamps.clear()
    }

    private suspend fun randomError(): ServerError {
        val errors = listOf(
            ServerError.InternalError("Simulated server error"),
            ServerError.ServiceUnavailable,
            ServerError.Timeout("Simulated timeout"),
        )
        return errors[randomSource.nextInt(errors.size)]
    }
}
