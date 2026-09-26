package org.mobilenativefoundation.trails.server.fake.internal.simulation

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class SimulationRandomSource(
    seed: Int,
) {
    private val mutex = Mutex()
    private var random = Random(seed)
    private var currentSeed: Int = seed

    suspend fun reseed(seed: Int) = mutex.withLock {
        currentSeed = seed
        random = Random(seed)
    }

    suspend fun nextFloat(): Float = mutex.withLock {
        random.nextFloat()
    }

    suspend fun nextLong(from: Long, until: Long): Long = mutex.withLock {
        random.nextLong(from, until)
    }

    suspend fun nextInt(until: Int): Int = mutex.withLock {
        random.nextInt(until)
    }

    suspend fun currentSeed(): Int = mutex.withLock { currentSeed }
}
