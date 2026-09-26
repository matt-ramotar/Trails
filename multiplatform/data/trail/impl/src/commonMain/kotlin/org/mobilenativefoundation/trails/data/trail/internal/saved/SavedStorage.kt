@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class, org.mobilenativefoundation.store6.core.DelicateStoreApi::class)

package org.mobilenativefoundation.trails.data.trail.internal.saved

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mobilenativefoundation.store6.core.StoreKey
import org.mobilenativefoundation.store6.core.StoreNamespace
import org.mobilenativefoundation.store6.mutations.MutationCodec
import org.mobilenativefoundation.store6.mutations.storage.*
import org.mobilenativefoundation.trails.data.trail.storage.db.TrailDataDatabase

@Serializable
internal data class SavedValue(val trailId: String, val collectionIds: Set<String>)

@Serializable
internal data class SavedCommandPayload(val commandId: String, val accountId: String, val value: SavedValue)

internal data class SavedKey(val accountId: String, val trailId: String) : StoreKey {
    override val namespace = StoreNamespace("saved-$accountId")
    override fun canonicalId() = trailId
}

internal object SavedCodec : MutationCodec<SavedValue> {
    override fun encode(value: SavedValue) = Json.encodeToString(SavedValue.serializer(), value.copy(collectionIds = value.collectionIds.sorted().toSet())).encodeToByteArray()
    override fun decode(version: Int, bytes: ByteArray): SavedValue {
        require(version == 1) { "Unsupported saved membership format $version" }
        return Json.decodeFromString(SavedValue.serializer(), bytes.decodeToString())
    }
}

internal object SavedCommandCodec : MutationCodec<SavedCommandPayload> {
    const val ID = "set-trail-collections"
    override fun encode(value: SavedCommandPayload) = Json.encodeToString(SavedCommandPayload.serializer(), value).encodeToByteArray()
    override fun decode(version: Int, bytes: ByteArray): SavedCommandPayload {
        require(version == 1) { "Unsupported save command format $version" }
        return Json.decodeFromString(SavedCommandPayload.serializer(), bytes.decodeToString())
    }
}

internal data class Acceptance(val payload: SavedCommandPayload, val mutationId: String)

/** Receipt and Store6 intent share the same driver, connection, transaction and failure boundary. */
internal class CorrelatedSavedJournal(
    private val accountId: String,
    private val database: TrailDataDatabase,
    private val delegate: MutationJournalStorage,
) : MutationJournalStorage {
    suspend fun receipt(commandId: String): Acceptance? = delegate.transaction {
        database.trailDataQueries.acceptance(accountId, commandId).executeAsOneOrNull()?.let { row ->
            val payload = SavedCommandCodec.decode(row.version.toInt(), row.payload)
            check(payload.accountId == accountId && payload.commandId == commandId)
            Acceptance(payload, row.mutation_id)
        }
    }
    suspend fun knownTrailIds(): Set<String> = delegate.transaction {
        database.trailDataQueries.acceptanceTrailIds(accountId).executeAsList().map {
            SavedCommandCodec.decode(it.version.toInt(), it.payload).value.trailId
        }.toSet()
    }
    override suspend fun <R> transaction(block: (MutationJournalTransaction) -> R): R = delegate.transaction { original ->
        block(object : MutationJournalTransaction by original {
            override fun insertIntent(recordVersion: Int, clientId: String, clientSequence: Long, mutationId: String,
                namespace: String, canonicalId: String, mutatorId: String, mutatorVersion: Int, argsBlob: ByteArray,
                idempotencyRoot: String, createdAt: Long): MutationIntentRecord {
                val row = original.insertIntent(recordVersion, clientId, clientSequence, mutationId, namespace,
                    canonicalId, mutatorId, mutatorVersion, argsBlob, idempotencyRoot, createdAt)
                if (mutatorId == SavedCommandCodec.ID) {
                    val payload = SavedCommandCodec.decode(mutatorVersion, row.argsBlob)
                    require(payload.accountId == accountId && namespace == "saved-$accountId" && payload.value.trailId == canonicalId)
                    database.trailDataQueries.insertAcceptance(accountId, payload.commandId, mutatorVersion.toLong(), row.argsBlob,
                        row.clientId, row.clientSequence, row.mutationId, row.createdAt)
                }
                return row
            }
        })
    }
}
