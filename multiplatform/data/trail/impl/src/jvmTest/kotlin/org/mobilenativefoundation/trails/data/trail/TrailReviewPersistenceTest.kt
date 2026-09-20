@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class)

package org.mobilenativefoundation.trails.data.trail

import app.cash.sqldelight.db.SqlDriver
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mobilenativefoundation.trails.data.trail.db.M1Database
import org.mobilenativefoundation.trails.server.BackendConfig
import org.mobilenativefoundation.trails.server.NetworkMode
import kotlin.time.Duration.Companion.milliseconds

class TrailReviewPersistenceTest {
    private val online = BackendConfig(latencyRange = 0.milliseconds..0.milliseconds)
    private val offline = online.copy(networkMode = NetworkMode.OFFLINE)

    @Test
    fun oldTrailPayloadWithoutReviewRemainsReadable() {
        val legacy = withoutReview(Json.encodeToJsonElement(Trail.serializer(), worldTrails.first()).jsonObject)
        val decoded = Json.decodeFromJsonElement(Trail.serializer(), legacy)
        assertEquals(worldTrails.first().id, decoded.id)
        assertEquals(worldTrails.first().description, decoded.description)
        assertNull(excerpt(decoded), "An old cached trail must not claim a review it has not fetched")
    }

    @Test
    fun legacyBackendReviewsRefreshBySelectedIdAndSurviveOfflineReopenWithoutChangingSaves() = runBlocking {
        val directory = Files.createTempDirectory("trails-m1-review-upgrade").toFile()
        val drivers = PlatformM1DriverFactory(directory)
        fun runtime() = RealTrailDataFactory(drivers, this, AccountRecoveryPolicy(automatic = false))
        val command = SetCollectionsCommand("review-refresh-pending", worldTrails.first().id, setOf("weekend", "favorites"))
        var current: RealTrailDataFactory? = null
        try {
            val original = runtime().also { current = it }
            original.applyBackendConfig(online)
            withTimeout(10_000) { original.trails.observeQuery(TrailQuery()).first { it.data != null } }
            val account = original.open("alice")
            account.saved.save(SetCollectionsCommand("review-refresh-confirmed", command.trailId, setOf("weekend")))
            account.saved.retryPending()
            withTimeout(10_000) { account.saved.state.first { it.data?.syncByTrail?.get(command.trailId)?.status == TrailSyncStatus.SYNCED } }
            original.applyBackendConfig(offline)
            account.saved.save(command)
            withTimeout(10_000) { account.saved.state.first { it.data?.memberships?.get(command.trailId) == command.collectionIds } }
            val before = original.backendEvidence()
            assertEquals(1L, before.receipts)
            original.close()
            current = null

            // Model a pre-excerpt install while retaining the actual backend receipt and queue.
            val backendDriver = drivers.open("trails-m1-backend.db")
            val receiptBefore = try {
                val database = M1Database(backendDriver)
                database.m1Queries.backendTrails().executeAsList().forEach { row ->
                    val old = withoutReview(Json.parseToJsonElement(row.payload.decodeToString()).jsonObject)
                    replacePayload(backendDriver, "UPDATE backend_trail SET payload = ? WHERE id = ?", old.toString().encodeToByteArray(), row.id)
                }
                readReceiptBytes(backendDriver)
            } finally { backendDriver.close() }
            val catalogDriver = drivers.open("trails-m1-catalog.db")
            try {
                val database = M1Database(catalogDriver)
                listOf("query", "trail").forEach { namespace ->
                    database.m1Queries.allCache(namespace).executeAsList().forEach { row ->
                        val page = Json.parseToJsonElement(row.payload.decodeToString()).jsonObject
                        val old = JsonObject(page + ("trails" to JsonArray((page.getValue("trails") as JsonArray).map { withoutReview(it.jsonObject) })))
                        database.m1Queries.writeCache(namespace, row.canonical_id, old.toString().encodeToByteArray())
                    }
                }
            } finally { catalogDriver.close() }

            val upgraded = runtime().also { current = it }
            assertEquals(NetworkMode.OFFLINE, upgraded.restoreBackendConfig().networkMode)
            val restored = upgraded.open("alice")
            assertEquals(command.collectionIds, restored.saved.state.value.data?.memberships?.get(command.trailId))
            assertEquals(before, upgraded.backendEvidence())
            val oldDetail = withTimeout(10_000) { upgraded.trails.observeTrail(command.trailId).first { it.data != null } }.data!!
            assertNull(excerpt(oldDetail))

            upgraded.applyBackendConfig(online)
            upgraded.trails.refreshQuery(TrailQuery())
            val refreshed = withTimeout(10_000) { upgraded.trails.observeQuery(TrailQuery()).first { it.data != null && !it.loading } }.data!!
            val selected = refreshed.single { it.id == command.trailId }
            val second = refreshed.single { it.id == worldTrails[1].id }
            assertEquals(worldTrails.first().reviewExcerpt, excerpt(selected))
            assertNotNull(excerpt(second))
            assertNotEquals(excerpt(selected), excerpt(second), "A different trail must not borrow the first trail's review")
            upgraded.trails.refreshTrail(second.id)
            val secondDetail = withTimeout(10_000) { upgraded.trails.observeTrail(second.id).first { it.data != null && !it.loading } }.data!!
            assertEquals(excerpt(second), excerpt(secondDetail))
            assertEquals(before.applications, upgraded.backendEvidence().applications)
            assertEquals(before.receipts, upgraded.backendEvidence().receipts)
            assertEquals(before.pushAttempts, upgraded.backendEvidence().pushAttempts)
            upgraded.applyBackendConfig(offline)
            upgraded.close()
            current = null

            val reopened = runtime().also { current = it }
            assertEquals(NetworkMode.OFFLINE, reopened.restoreBackendConfig().networkMode)
            val cached = withTimeout(10_000) { reopened.trails.observeTrail(second.id).first { it.data != null } }.data!!
            assertEquals(excerpt(second), excerpt(cached))
            val saved = reopened.open("alice").saved
            assertEquals(command.collectionIds, saved.state.value.data?.memberships?.get(command.trailId))
            assertTrue(saved.reconcile(command) is SaveOutcome.Journaled)
            assertEquals(before.applications, reopened.backendEvidence().applications)
            reopened.close()
            current = null
            val verifiedDriver = drivers.open("trails-m1-backend.db")
            try { assertEquals(receiptBefore, readReceiptBytes(verifiedDriver)) }
            finally { verifiedDriver.close() }
        } finally { current?.close(); directory.deleteRecursively() }
    }

    private fun excerpt(trail: Trail): String? =
        Json.encodeToJsonElement(Trail.serializer(), trail).jsonObject["reviewExcerpt"]?.jsonPrimitive?.content

    private fun withoutReview(trail: JsonObject) = JsonObject(trail - "reviewExcerpt")

    private fun replacePayload(driver: SqlDriver, statement: String, payload: ByteArray, id: String) {
        driver.execute(null, statement, 2) { bindBytes(0, payload); bindString(1, id) }.value
    }

    private fun readReceiptBytes(driver: SqlDriver): List<List<String>> = driver.executeQuery(null,
        "SELECT account, installation, idempotency_key, trail_id, version, hex(request), hex(result) FROM backend_receipt ORDER BY account, installation, idempotency_key",
        { cursor ->
            val rows = mutableListOf<List<String>>()
            while (cursor.next().value) rows += (0..6).map { cursor.getString(it).orEmpty() }
            app.cash.sqldelight.db.QueryResult.Value(rows)
        }, 0,
    ).value
}
