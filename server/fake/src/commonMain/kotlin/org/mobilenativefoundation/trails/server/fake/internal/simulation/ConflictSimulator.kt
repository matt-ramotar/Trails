package org.mobilenativefoundation.trails.server.fake.internal.simulation

import org.mobilenativefoundation.trails.server.ConflictMode
import org.mobilenativefoundation.trails.server.error.ServerError

class ConflictSimulator(
    private var mode: ConflictMode,
    private var probability: Float,
    private val randomSource: SimulationRandomSource,
) {
    private var forcedConflictPending = false

    fun updateMode(mode: ConflictMode, probability: Float = 0.0f) {
        this.mode = mode
        this.probability = probability
    }

    suspend fun evaluate(
        resourceType: String,
        id: String,
        clientVersion: Long,
        serverVersion: Long,
    ): ConflictOutcome {
        val staleConflict = clientVersion < serverVersion
        val forcedConflict = if (forcedConflictPending) {
            forcedConflictPending = false
            true
        } else {
            false
        }
        val probabilisticConflict = probability > 0f && randomSource.nextFloat() < probability

        val hasConflict = staleConflict || forcedConflict || probabilisticConflict
        if (!hasConflict || (mode == ConflictMode.DISABLED && !staleConflict)) {
            return ConflictOutcome.Proceed
        }

        return when (mode) {
            ConflictMode.DISABLED, ConflictMode.HTTP_409 -> ConflictOutcome.Reject(
                ServerError.Conflict(resourceType, id, clientVersion, serverVersion),
            )
            ConflictMode.AUTO_MERGE -> ConflictOutcome.AutoMerge
            ConflictMode.LAST_WRITE_WINS -> ConflictOutcome.LastWriteWins
        }
    }

    fun triggerNextConflict() {
        forcedConflictPending = true
    }

    fun reset() {
        forcedConflictPending = false
    }
}

sealed interface ConflictOutcome {
    data object Proceed : ConflictOutcome
    data object AutoMerge : ConflictOutcome
    data object LastWriteWins : ConflictOutcome
    data class Reject(val error: ServerError.Conflict) : ConflictOutcome
}
