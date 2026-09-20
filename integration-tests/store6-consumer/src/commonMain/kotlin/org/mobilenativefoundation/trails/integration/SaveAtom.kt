package org.mobilenativefoundation.trails.integration

import dev.mattramotar.atom.runtime.Atom
import dev.mattramotar.atom.runtime.EffectInterpreter
import dev.mattramotar.atom.runtime.fsm.Event
import dev.mattramotar.atom.runtime.fsm.Intent
import dev.mattramotar.atom.runtime.fsm.SideEffect
import dev.mattramotar.atom.runtime.fsm.Transition
import dev.mattramotar.atom.runtime.state.StateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException

data class SaveCommand(val id: String, val key: MembershipKey)

sealed interface EnqueueOutcome {
    val command: SaveCommand
    data class Journaled(override val command: SaveCommand, val mutationId: String) : EnqueueOutcome
    data class Rejected(override val command: SaveCommand, val reason: String) : EnqueueOutcome
    data class Uncertain(override val command: SaveCommand, val reason: String) : EnqueueOutcome
}

fun interface SaveCommandSink {
    suspend fun enqueue(command: SaveCommand): EnqueueOutcome
}

sealed interface SaveIntent : Intent {
    data class Save(val command: SaveCommand) : SaveIntent
}
sealed interface SaveEvent : Event {
    data class Requested(val command: SaveCommand) : SaveEvent
    data class Completed(val outcome: EnqueueOutcome) : SaveEvent
}
sealed interface SaveEffect : SideEffect {
    data class Enqueue(val command: SaveCommand) : SaveEffect
}
sealed interface SaveState {
    data object Idle : SaveState
    data class Enqueueing(val command: SaveCommand) : SaveState
    data class Journaled(val command: SaveCommand, val mutationId: String) : SaveState
    data class Rejected(val command: SaveCommand, val reason: String) : SaveState
    data class Uncertain(val command: SaveCommand, val reason: String) : SaveState
}

/** One bounded command, ending at journal admission. It never drains or replays effects. */
class SaveAtom(
    scope: CoroutineScope,
    handle: StateHandle<SaveState>,
    services: SaveCommandSink,
) : Atom<SaveState, SaveIntent, SaveEvent, SaveEffect>(
    scope,
    handle,
    interpreter = EffectInterpreter { effect ->
        when (effect) {
            is SaveEffect.Enqueue -> {
                val outcome = try {
                    services.enqueue(effect.command)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    // A failed return does not prove the transaction did not commit. No automatic retry.
                    EnqueueOutcome.Uncertain(effect.command, failure.message?.take(200) ?: "Local admission outcome is unknown")
                }
                SaveEvent.Completed(outcome)
            }
        }
    },
) {
    override fun eventFor(intent: SaveIntent): SaveEvent = when (intent) {
        is SaveIntent.Save -> SaveEvent.Requested(intent.command)
    }

    override fun reduce(state: SaveState, event: SaveEvent): Transition<SaveState, SaveEffect> = when (event) {
        is SaveEvent.Requested -> if (state is SaveState.Enqueueing || state is SaveState.Uncertain) {
            Transition(to = state)
        } else {
            Transition(to = SaveState.Enqueueing(event.command), effects = listOf(SaveEffect.Enqueue(event.command)))
        }
        is SaveEvent.Completed -> Transition(to = when (val outcome = event.outcome) {
            is EnqueueOutcome.Journaled -> SaveState.Journaled(outcome.command, outcome.mutationId)
            is EnqueueOutcome.Rejected -> SaveState.Rejected(outcome.command, outcome.reason)
            is EnqueueOutcome.Uncertain -> SaveState.Uncertain(outcome.command, outcome.reason)
        })
    }
}
