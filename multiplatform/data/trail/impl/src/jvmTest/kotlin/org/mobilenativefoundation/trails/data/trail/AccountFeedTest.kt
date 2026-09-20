package org.mobilenativefoundation.trails.data.trail

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.mobilenativefoundation.trails.server.BackendConfig
import org.mobilenativefoundation.trails.server.NetworkMode
import kotlin.time.Duration.Companion.milliseconds

class AccountFeedTest {
    private val online = BackendConfig(latencyRange = 0.milliseconds..0.milliseconds)

    @Test
    fun feedsSeedOncePerAccountAndSurviveAnOfflineReopen() = runBlocking {
        val directory = Files.createTempDirectory("trails-feeds").toFile()
        var first: RealTrailDataFactory? = null
        var reopened: RealTrailDataFactory? = null
        try {
            first = RealTrailDataFactory(PlatformM1DriverFactory(directory), this)
            first.applyBackendConfig(online)
            val account = first.open("alice")
            val activities = withTimeout(10_000) { account.activities.observe().first { it.data != null } }.data!!
            assertEquals(6, activities.size)
            assertEquals("half-dome", activities.first().trailId)
            val feed = withTimeout(10_000) { account.forYou.observe().first { it.data != null } }.data!!
            assertEquals("trolltunga", feed.featuredTrailId)
            first.applyBackendConfig(online.copy(networkMode = NetworkMode.OFFLINE))
            first.close()

            reopened = RealTrailDataFactory(PlatformM1DriverFactory(directory), this)
            assertEquals(NetworkMode.OFFLINE, reopened.restoreBackendConfig().networkMode)
            val again = reopened.open("alice")
            val cached = withTimeout(10_000) { again.activities.observe().first { it.data != null } }
            assertEquals(activities, cached.data)
            assertTrue(cached.offline)
            assertEquals(feed, withTimeout(10_000) { again.forYou.observe().first { it.data != null } }.data)
            reopened.applyBackendConfig(online)
            val beforeActivityRefresh = reopened.backendEvidence().requests
            again.activities.refresh()
            assertTrue(reopened.backendEvidence().requests > beforeActivityRefresh, "Activity refresh must reach the backend")
            assertEquals(activities, withTimeout(10_000) { again.activities.observe().first { it.data != null && !it.loading && it.error == null } }.data)
            val beforeFeedRefresh = reopened.backendEvidence().requests
            again.forYou.refresh()
            assertTrue(reopened.backendEvidence().requests > beforeFeedRefresh, "For You refresh must reach the backend")
            assertEquals(feed, withTimeout(10_000) { again.forYou.observe().first { it.data != null && !it.loading && it.error == null } }.data)
            reopened.applyBackendConfig(online.copy(networkMode = NetworkMode.OFFLINE))
            val cold = reopened.open("bob")
            val unavailable = withTimeout(10_000) { cold.activities.observe().first { it.error != null } }
            assertNull(unavailable.data)
            assertTrue(unavailable.offline)
            reopened.applyBackendConfig(online)
            cold.activities.refresh()
            val seeded = withTimeout(10_000) { cold.activities.observe().first { it.data != null } }.data!!
            assertNotNull(seeded.firstOrNull())
            assertTrue(seeded.first().completedAtEpochMillis >= activities.first().completedAtEpochMillis)
        } finally {
            withContext(NonCancellable) {
                try { reopened?.close() }
                finally {
                    try { first?.close() }
                    finally { directory.deleteRecursively() }
                }
            }
        }
    }
}
