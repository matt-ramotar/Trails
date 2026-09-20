package org.mobilenativefoundation.trails.server.fake.internal.seed

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.trails.server.fake.internal.storage.BackendTables

internal class SeedDataLoader(
    private val tables: BackendTables,
    private val seedDefaultData: Boolean = true,
) {
    private var seeded = false
    private val mutex = Mutex()

    suspend fun ensureSeeded() = mutex.withLock {
        if (!seedDefaultData) return@withLock
        if (seeded) return@withLock
        loadDefaultSeed()
        seeded = true
    }

    suspend fun reset() = mutex.withLock {
        clearAllTables()
        seeded = false
    }

    private suspend fun clearAllTables() {
        tables.users().clear()
        tables.sessions().clear()
        tables.follows().clear()
        tables.posts().clear()
        tables.postEngagements().clear()
        tables.comments().clear()
        tables.resorts().clear()
        tables.runs().clear()
        tables.weather().clear()
        tables.userFavorites().clear()
    }

    private suspend fun loadDefaultSeed() {
        val seedData = DefaultSeedData()
        tables.users().putAll(seedData.users)
        tables.resorts().putAll(seedData.resorts)
        tables.runs().putAll(seedData.runs)
        tables.weather().putAll(seedData.weather)
        tables.posts().putAll(seedData.posts)
        tables.follows().putAll(seedData.follows)
    }

    // Keep alternate seed helpers out for now; deterministic stochastic simulation is handled by run seed presets.
}
