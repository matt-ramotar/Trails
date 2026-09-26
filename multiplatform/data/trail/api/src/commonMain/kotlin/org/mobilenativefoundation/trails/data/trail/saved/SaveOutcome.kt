package org.mobilenativefoundation.trails.data.trail.saved

sealed interface SaveOutcome {
    val command: SetCollectionsCommand
    data class Journaled(override val command: SetCollectionsCommand, val mutationId: String) : SaveOutcome
    data class Rejected(override val command: SetCollectionsCommand, val reason: String) : SaveOutcome
    data class Uncertain(override val command: SetCollectionsCommand, val reason: String) : SaveOutcome
}
