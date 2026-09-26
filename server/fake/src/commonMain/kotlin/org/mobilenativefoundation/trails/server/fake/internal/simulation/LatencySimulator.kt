package org.mobilenativefoundation.trails.server.fake.internal.simulation

import kotlinx.coroutines.delay
import org.mobilenativefoundation.trails.server.BackendConfigProvider
import kotlin.math.max
import kotlin.math.min

class LatencySimulator(
    private val configProvider: BackendConfigProvider,
    private val randomSource: SimulationRandomSource,
) {
    suspend fun <T> withLatency(block: suspend () -> T): T {
        val range = configProvider.current.latencyRange
        val startMs = range.start.inWholeMilliseconds
        val endMs = range.endInclusive.inWholeMilliseconds
        val minMs = min(startMs, endMs)
        val maxMs = max(startMs, endMs)

        if (minMs > 0 || maxMs > 0) {
            val delayMs = randomSource.nextLong(minMs.coerceAtLeast(0), maxMs.coerceAtLeast(0) + 1)
            if (delayMs > 0) {
                delay(delayMs)
            }
        }

        return block()
    }
}
