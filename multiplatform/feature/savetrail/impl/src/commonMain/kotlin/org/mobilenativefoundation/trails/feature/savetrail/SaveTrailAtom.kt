package org.mobilenativefoundation.trails.feature.savetrail

import dev.mattramotar.atom.runtime.Atom
import dev.mattramotar.atom.runtime.EffectInterpreter
import dev.mattramotar.atom.runtime.fsm.Event
import dev.mattramotar.atom.runtime.fsm.Intent
import dev.mattramotar.atom.runtime.fsm.SideEffect
import dev.mattramotar.atom.runtime.fsm.Transition
import dev.mattramotar.atom.runtime.state.StateHandle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.saved.SaveOutcome
import org.mobilenativefoundation.trails.data.trail.saved.SavedRepository
import org.mobilenativefoundation.trails.data.trail.saved.SetCollectionsCommand
import org.mobilenativefoundation.trails.data.trail.saved.TrailCollection

internal enum class SavePhase { LOADING, UNAVAILABLE, EDITING, CONFIRM_REMOVE, ADMITTING, RECONCILING, FAILED, UNKNOWN, JOURNALED }

internal data class SaveFlowState(
    val trail: Trail,
    val phase: SavePhase = SavePhase.LOADING,
    val collections: List<TrailCollection> = emptyList(),
    val original: Set<String> = emptySet(),
    val selected: Set<String> = emptySet(),
    val command: SetCollectionsCommand? = null,
    val error: String? = null,
) {
    val editable: Boolean get() = phase == SavePhase.EDITING || phase == SavePhase.FAILED
    val canDismiss: Boolean get() = phase !in setOf(SavePhase.ADMITTING, SavePhase.RECONCILING, SavePhase.UNKNOWN)
}

internal sealed interface SaveFlowIntent : Intent {
    data object LoadChoices : SaveFlowIntent
    data class Toggle(val id: String) : SaveFlowIntent
    data class Submit(val commandId: String, val confirmedRemoval: Boolean = false) : SaveFlowIntent
    data object KeepEditing : SaveFlowIntent
    data object Reconcile : SaveFlowIntent
}

internal sealed interface SaveFlowEvent : Event {
    data class Action(val intent: SaveFlowIntent) : SaveFlowEvent
    data class Choices(val collections: List<TrailCollection>, val membership: Set<String>) : SaveFlowEvent
    data class ChoicesFailed(val message: String) : SaveFlowEvent
    data class Completed(val outcome: SaveOutcome) : SaveFlowEvent
}

internal sealed interface SaveFlowEffect : SideEffect {
    data class LoadChoices(val trailId: String) : SaveFlowEffect
    data class Admit(val command: SetCollectionsCommand, val reconcile: Boolean = false) : SaveFlowEffect
}

/** No persisted Atom state, stream collection, drain loop, or remote-settlement wait. */
internal class SaveTrailAtom(
    scope: CoroutineScope,
    handle: StateHandle<SaveFlowState>,
    repository: SavedRepository,
) : Atom<SaveFlowState, SaveFlowIntent, SaveFlowEvent, SaveFlowEffect>(
    scope,
    handle,
    interpreter = EffectInterpreter { effect ->
        when (effect) {
            is SaveFlowEffect.LoadChoices -> try {
                // A loaded account snapshot can precede this trail's persisted projection.
                // Only an explicit entry (including an empty set) establishes its membership.
                if (repository.state.value.data?.memberships?.containsKey(effect.trailId) != true) repository.refresh()
                val projection = repository.state.value
                val data = projection.data
                val membership = data?.memberships?.get(effect.trailId)
                if (data == null || membership == null) SaveFlowEvent.ChoicesFailed(projection.error ?: "Your saved collections for this trail are not available yet.")
                else SaveFlowEvent.Choices(data.collections, membership)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { SaveFlowEvent.ChoicesFailed(failure.message ?: "Couldn’t load your collections") }
            is SaveFlowEffect.Admit -> {
                val outcome = try {
                    if (effect.reconcile) repository.reconcile(effect.command) else repository.save(effect.command)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { SaveOutcome.Uncertain(effect.command, failure.message ?: "Check whether this save was admitted.") }
                SaveFlowEvent.Completed(outcome)
            }
        }
    },
) {
    override fun eventFor(intent: SaveFlowIntent): SaveFlowEvent = SaveFlowEvent.Action(intent)

    override fun reduce(state: SaveFlowState, event: SaveFlowEvent): Transition<SaveFlowState, SaveFlowEffect> = when (event) {
        is SaveFlowEvent.Choices -> if (state.phase == SavePhase.LOADING) Transition(state.copy(
            phase = SavePhase.EDITING, collections = event.collections, original = event.membership,
            selected = event.membership, error = null,
        )) else Transition(state)
        is SaveFlowEvent.ChoicesFailed -> Transition(state.copy(phase = SavePhase.UNAVAILABLE, error = event.message))
        is SaveFlowEvent.Completed -> if (state.command != event.outcome.command) Transition(state) else Transition(
            when (val outcome = event.outcome) {
                is SaveOutcome.Journaled -> state.copy(phase = SavePhase.JOURNALED, error = null)
                is SaveOutcome.Rejected -> state.copy(phase = SavePhase.FAILED, error = outcome.reason)
                is SaveOutcome.Uncertain -> state.copy(phase = SavePhase.UNKNOWN, error = outcome.reason)
            },
        )
        is SaveFlowEvent.Action -> when (val intent = event.intent) {
            SaveFlowIntent.LoadChoices -> if (state.phase in setOf(SavePhase.LOADING, SavePhase.UNAVAILABLE)) Transition(
                state.copy(phase = SavePhase.LOADING, error = null), listOf(SaveFlowEffect.LoadChoices(state.trail.id)),
            ) else Transition(state)
            is SaveFlowIntent.Toggle -> if (state.editable && state.collections.any { it.id == intent.id }) Transition(state.copy(
                phase = SavePhase.EDITING,
                selected = if (intent.id in state.selected) state.selected - intent.id else state.selected + intent.id,
                command = null, error = null,
            )) else Transition(state)
            is SaveFlowIntent.Submit -> when {
                !state.editable && state.phase != SavePhase.CONFIRM_REMOVE -> Transition(state)
                state.selected.isEmpty() && state.original.isEmpty() -> Transition(state)
                state.selected.isEmpty() && !intent.confirmedRemoval -> Transition(state.copy(phase = SavePhase.CONFIRM_REMOVE))
                else -> {
                    val command = state.command ?: SetCollectionsCommand(intent.commandId, state.trail.id, state.selected.toSet())
                    Transition(state.copy(phase = SavePhase.ADMITTING, command = command, error = null), listOf(SaveFlowEffect.Admit(command)))
                }
            }
            SaveFlowIntent.KeepEditing -> if (state.phase == SavePhase.CONFIRM_REMOVE) Transition(state.copy(phase = SavePhase.EDITING)) else Transition(state)
            SaveFlowIntent.Reconcile -> if (state.phase == SavePhase.UNKNOWN && state.command != null) Transition(
                state.copy(phase = SavePhase.RECONCILING, error = null), listOf(SaveFlowEffect.Admit(state.command, reconcile = true)),
            ) else Transition(state)
        }
    }
}
