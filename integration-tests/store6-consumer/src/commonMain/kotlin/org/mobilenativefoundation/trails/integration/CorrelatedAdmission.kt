@file:OptIn(
    org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class,
    org.mobilenativefoundation.store6.core.DelicateStoreApi::class,
)

package org.mobilenativefoundation.trails.integration

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mobilenativefoundation.store6.mutations.MutationCodec
import org.mobilenativefoundation.store6.mutations.storage.MutationIntentRecord
import org.mobilenativefoundation.store6.mutations.storage.MutationJournalStorage
import org.mobilenativefoundation.store6.mutations.storage.MutationJournalTransaction
import org.mobilenativefoundation.trails.integration.db.FixtureDatabase

/** Version 1 journal arguments predate command correlation; their membership remains replayable. */
@Serializable
data class SavedCommandPayload(
    val commandId: String?,
    val accountId: String?,
    val membership: Membership,
)

object SavedCommandCodec : MutationCodec<SavedCommandPayload> {
    const val MUTATOR_ID = "desired-membership"
    const val VERSION = 2

    override fun encode(value: SavedCommandPayload): ByteArray {
        require(!value.commandId.isNullOrBlank() && !value.accountId.isNullOrBlank())
        return Json.encodeToString(SavedCommandPayload.serializer(), value).encodeToByteArray()
    }

    override fun decode(version: Int, bytes: ByteArray): SavedCommandPayload = when (version) {
        1 -> SavedCommandPayload(null, null, MembershipCodec.decode(1, bytes))
        VERSION -> Json.decodeFromString(SavedCommandPayload.serializer(), bytes.decodeToString()).also {
            require(!it.commandId.isNullOrBlank() && !it.accountId.isNullOrBlank())
        }
        else -> error("Unsupported saved-command argument version $version")
    }
}

internal data class AdmissionReceipt(val payload: SavedCommandPayload, val mutationId: String)

/**
 * Adds admission metadata to Store6's own transaction. [database] must use the same driver as
 * [delegate]. The receipt outlives Store6 pruning; it never drives transport or queue phases.
 */
internal class CorrelatedJournalStorage(
    private val account: String,
    private val database: FixtureDatabase,
    private val delegate: MutationJournalStorage,
) : MutationJournalStorage {
    suspend fun receipt(commandId: String): AdmissionReceipt? = delegate.transaction {
        database.commandAcceptanceQueries.acceptance(account, commandId).executeAsOneOrNull()?.let { row ->
            require(row.payload_version in 1L..Int.MAX_VALUE.toLong())
            val payload = SavedCommandCodec.decode(row.payload_version.toInt(), row.payload)
            check(payload.accountId == account && payload.commandId == commandId) { "Admission receipt identity is invalid" }
            AdmissionReceipt(payload, row.mutation_id)
        }
    }

    override suspend fun <R> transaction(block: (MutationJournalTransaction) -> R): R =
        delegate.transaction { transaction ->
            block(object : MutationJournalTransaction by transaction {
                override fun insertIntent(
                    recordVersion: Int,
                    clientId: String,
                    clientSequence: Long,
                    mutationId: String,
                    namespace: String,
                    canonicalId: String,
                    mutatorId: String,
                    mutatorVersion: Int,
                    argsBlob: ByteArray,
                    idempotencyRoot: String,
                    createdAt: Long,
                ): MutationIntentRecord {
                    val row = transaction.insertIntent(
                        recordVersion, clientId, clientSequence, mutationId, namespace, canonicalId,
                        mutatorId, mutatorVersion, argsBlob, idempotencyRoot, createdAt,
                    )
                    if (mutatorId == SavedCommandCodec.MUTATOR_ID) {
                        val payload = SavedCommandCodec.decode(mutatorVersion, row.argsBlob)
                        // Legacy version-1 rows contain no app command identity to reconstruct.
                        if (payload.commandId != null) {
                            require(payload.accountId == account && namespace == "saved-$account")
                            require(canonicalId == "${payload.membership.listId}:${payload.membership.trailId}")
                            database.commandAcceptanceQueries.insertAcceptance(
                                account, payload.commandId, mutatorVersion.toLong(), row.argsBlob,
                                row.clientId, row.clientSequence, row.mutationId, row.createdAt,
                            )
                        }
                    }
                    return row
                }
            })
        }
}
