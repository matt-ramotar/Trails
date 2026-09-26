package org.mobilenativefoundation.trails.data.trail.internal.catalog

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.mobilenativefoundation.trails.data.backend.BackendConfig
import org.mobilenativefoundation.trails.data.backend.NetworkMode
import org.mobilenativefoundation.trails.data.trail.account.RealTrailDataFactory
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.data.trail.catalog.TrailSort
import org.mobilenativefoundation.trails.data.trail.storage.PlatformTrailDatabaseDriverFactory
import org.mobilenativefoundation.trails.data.trail.storage.TrailDatabaseDriverFactory
import org.mobilenativefoundation.trails.data.trail.storage.db.TrailDataDatabase

/** Sort reorders one cached membership; it must never key a separate fetch. */
class TrailSortCacheTest {
    private val online = BackendConfig(latencyRange = 0.milliseconds..0.milliseconds)

    @Test
    fun changingSortOfflineReordersTheCachedPageWithoutAFetchOrASecondCacheRow() = runBlocking {
        val directory = Files.createTempDirectory("trails-sort").toFile()
        val drivers = PlatformTrailDatabaseDriverFactory(directory)
        val runtime = RealTrailDataFactory(drivers, this)
        try {
            runtime.applyBackendConfig(online)
            val recommended = withTimeout(10_000) {
                runtime.trails.observeQuery(TrailQuery()).first { it.data != null && !it.loading }
            }.data!!
            assertTrue(recommended.size > 1)
            val requestsAfterSeed = runtime.backendEvidence().requests

            // Offline proves the sorted read is served from the cached membership alone.
            runtime.applyBackendConfig(online.copy(networkMode = NetworkMode.OFFLINE))
            val shortest = withTimeout(10_000) {
                runtime.trails.observeQuery(TrailQuery(sort = TrailSort.SHORTEST)).first { it.data != null && !it.loading }
            }
            assertNull(shortest.error)
            assertEquals(recommended.sortedBy { it.distanceMeters }.map { it.id }, shortest.data!!.map { it.id })
            assertEquals(recommended.toSet(), shortest.data!!.toSet(), "Sorting must not change the membership")
            assertEquals(requestsAfterSeed, runtime.backendEvidence().requests, "A sort change must not reach the backend")
            assertEquals(1, queryCacheRows(drivers), "Every sort must share the one cached query row")
        } finally { runtime.close(); directory.deleteRecursively() }
    }

    private fun queryCacheRows(drivers: TrailDatabaseDriverFactory): Int {
        val driver = drivers.open("trails-m1-catalog.db")
        return try { TrailDataDatabase(driver).trailDataQueries.allCache("query").executeAsList().size } finally { driver.close() }
    }
}
