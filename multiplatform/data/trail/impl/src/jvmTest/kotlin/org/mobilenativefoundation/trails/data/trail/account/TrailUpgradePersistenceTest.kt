package org.mobilenativefoundation.trails.data.trail.account

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.mobilenativefoundation.trails.data.backend.BackendConfig
import org.mobilenativefoundation.trails.data.backend.NetworkMode
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.data.trail.saved.SaveOutcome
import org.mobilenativefoundation.trails.data.trail.saved.SetCollectionsCommand
import org.mobilenativefoundation.trails.data.trail.saved.TrailSyncStatus
import org.mobilenativefoundation.trails.data.trail.storage.PlatformTrailDatabaseDriverFactory

class TrailUpgradePersistenceTest {
    @Test
    fun previousBuildOfflineSaveSurvivesUpgradeAndAppliesExactlyOnce() = runBlocking {
        val manifest = Json.parseToJsonElement(resource("manifest.json").decodeToString()).jsonObject
        assertEquals("f096775268af503c509da32fc7f5c21ef127a137", manifest.string("producerRevision"))
        val directory = Files.createTempDirectory("trails-upgrade")
        val acceptance = manifest.getValue("acceptance").jsonObject
        val accountId = acceptance.string("accountId")
        val command = SetCollectionsCommand(
            acceptance.string("commandId"),
            manifest.string("trailId"),
            manifest.getValue("collectionIds").jsonArray.map { it.jsonPrimitive.content }.toSet(),
        )
        var runtime: RealTrailDataFactory? = null
        try {
            manifest.getValue("files").jsonObject.forEach { (name, expectedHash) ->
                val bytes = resource(name)
                assertEquals(expectedHash.jsonPrimitive.content, sha256(bytes), "Fixture changed: $name")
                Files.write(directory.resolve(name), bytes)
            }
            assertAcceptance(directory, manifest)

            val upgraded = RealTrailDataFactory(PlatformTrailDatabaseDriverFactory(directory.toFile()), this)
            runtime = upgraded
            assertEquals(NetworkMode.OFFLINE, upgraded.restoreBackendConfig().networkMode)
            val catalog = withTimeout(10_000) {
                upgraded.trails.observeQuery(TrailQuery()).first { it.data != null }.data!!
            }
            assertTrue(catalog.any { it.id == command.trailId })
            val detail = withTimeout(10_000) {
                upgraded.trails.observeTrail(command.trailId).first { it.data != null }.data!!
            }
            assertEquals(command.trailId, detail.id)

            val account = upgraded.open(accountId)
            val saved = withTimeout(10_000) {
                account.saved.state.first {
                    it.data?.memberships?.get(command.trailId) == command.collectionIds &&
                        (it.data?.syncByTrail?.get(command.trailId)?.pendingCount ?: 0) > 0
                }.data!!
            }
            assertTrue(saved.offline)
            assertEquals(setOf("favorites", "weekend"), saved.collections.map { it.id }.toSet())
            assertFalse(withTimeout(10_000) { account.activities.observe().first { it.data != null }.data!! }.isEmpty())
            assertNotNull(withTimeout(10_000) { account.forYou.observe().first { it.data != null }.data })
            val originalMutationId = acceptance.string("mutationId")
            assertEquals(originalMutationId, assertIs<SaveOutcome.Journaled>(account.saved.reconcile(command)).mutationId)
            assertEquals(originalMutationId, assertIs<SaveOutcome.Journaled>(account.saved.save(command)).mutationId)
            assertEquals(0L, upgraded.backendEvidence().applications)
            assertEquals(0L, upgraded.backendEvidence().receipts)
            assertAcceptance(directory, manifest)

            upgraded.applyBackendConfig(BackendConfig(latencyRange = 0.milliseconds..0.milliseconds))
            account.saved.retryPending()
            withTimeout(10_000) {
                account.saved.state.first { it.data?.syncByTrail?.get(command.trailId)?.status == TrailSyncStatus.SYNCED }
            }
            assertEquals(1L, upgraded.backendEvidence().applications)
            assertEquals(1L, upgraded.backendEvidence().receipts)
            assertEquals(originalMutationId, assertIs<SaveOutcome.Journaled>(account.saved.save(command)).mutationId)
            upgraded.close()
            runtime = null
            assertAcceptance(directory, manifest)

            val reopened = RealTrailDataFactory(PlatformTrailDatabaseDriverFactory(directory.toFile()), this)
            runtime = reopened
            reopened.restoreBackendConfig()
            val reopenedAccount = reopened.open(accountId)
            val confirmed = withTimeout(10_000) {
                reopenedAccount.saved.state.first {
                    it.data?.memberships?.get(command.trailId) == command.collectionIds &&
                        it.data?.syncByTrail?.get(command.trailId)?.status == TrailSyncStatus.SYNCED
                }.data!!
            }
            assertEquals(command.collectionIds, confirmed.memberships[command.trailId])
            assertEquals(originalMutationId, assertIs<SaveOutcome.Journaled>(reopenedAccount.saved.reconcile(command)).mutationId)
            reopenedAccount.saved.retryPending()
            assertEquals(1L, reopened.backendEvidence().applications)
            assertEquals(1L, reopened.backendEvidence().receipts)
        } finally {
            runtime?.close()
            directory.toFile().deleteRecursively()
        }
    }

    private fun assertAcceptance(directory: Path, manifest: JsonObject) {
        val expected = manifest.getValue("acceptance").jsonObject
        val journalName = manifest.getValue("files").jsonObject.keys.single { it.endsWith("-journal.db") }
        DriverManager.getConnection("jdbc:sqlite:${directory.resolve(journalName)}").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT * FROM command_acceptance").use { rows ->
                    assertTrue(rows.next())
                    assertEquals(expected.string("accountId"), rows.getString("account"))
                    assertEquals(expected.string("commandId"), rows.getString("command_id"))
                    assertEquals(expected.getValue("version").jsonPrimitive.long, rows.getLong("version"))
                    assertEquals(expected.string("payload"), rows.getBytes("payload").decodeToString())
                    assertEquals(expected.string("clientId"), rows.getString("client_id"))
                    assertEquals(expected.getValue("clientSequence").jsonPrimitive.long, rows.getLong("client_sequence"))
                    assertEquals(expected.string("mutationId"), rows.getString("mutation_id"))
                    assertEquals(expected.getValue("admittedAt").jsonPrimitive.long, rows.getLong("admitted_at"))
                    assertFalse(rows.next())
                }
                statement.executeQuery("SELECT installation_id FROM account_identity").use { rows ->
                    assertTrue(rows.next())
                    assertEquals(manifest.string("installationId"), rows.getString("installation_id"))
                    assertFalse(rows.next())
                }
            }
        }
    }

    private fun resource(name: String): ByteArray =
        checkNotNull(javaClass.getResourceAsStream("/upgrade-fixtures/f096775/$name")) { "Missing upgrade fixture: $name" }
            .use { it.readBytes() }

    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
}
