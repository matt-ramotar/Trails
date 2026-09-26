package org.mobilenativefoundation.trails.feat.savetrail

import dev.mattramotar.atom.runtime.Admission
import dev.mattramotar.atom.runtime.state.InMemoryStateHandle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.data.trail.*

/** Finite UI admission semantics only; real persistence/retirement lives in data-layer tests. */
class SaveTrailAtomTest {
    @Test
    fun repeatedSubmitWhileAdmissionRunsCallsSaveOnce() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val repository = RecordingSavedRepository()
        repository.saveResult = { command -> release.await(); SaveOutcome.Journaled(command, "mutation-one") }
        withAtom(repository) { atom ->
            atom.awaitPhase(SavePhase.EDITING)
            atom.dispatchAndReduce(SaveFlowIntent.Toggle("weekend"))
            atom.dispatchAndReduce(SaveFlowIntent.Submit("first"))
            atom.awaitPhase(SavePhase.ADMITTING)
            atom.dispatchAndReduce(SaveFlowIntent.Submit("duplicate"))
            release.complete(Unit)
            atom.awaitPhase(SavePhase.JOURNALED)
            assertEquals(listOf(SetCollectionsCommand("first", trail.id, setOf("weekend"))), repository.saves)
        }
    }

    @Test
    fun uncertainAdmissionFreezesPayloadAndReconcilesWithoutSavingAgain() = runBlocking {
        val repository = RecordingSavedRepository()
        repository.saveResult = { SaveOutcome.Uncertain(it, "Receipt read unavailable") }
        withAtom(repository) { atom ->
            atom.awaitPhase(SavePhase.EDITING)
            atom.dispatchAndReduce(SaveFlowIntent.Toggle("weekend"))
            atom.dispatchAndReduce(SaveFlowIntent.Submit("frozen-command"))
            val uncertain = atom.awaitPhase(SavePhase.UNKNOWN)
            atom.dispatchAndReduce(SaveFlowIntent.Toggle("favorites"))
            atom.dispatchAndReduce(SaveFlowIntent.Submit("different-command"))
            assertEquals(uncertain, atom.state.value)
            atom.dispatchAndReduce(SaveFlowIntent.Reconcile)
            val result = atom.awaitPhase(SavePhase.JOURNALED)
            assertEquals(uncertain.command, result.command)
            assertEquals(setOf("weekend"), result.selected)
            assertEquals(listOf(uncertain.command), repository.saves)
            assertEquals(repository.saves, repository.reconciliations)
        }
    }

    @Test
    fun editingAfterDefiniteRejectionUsesNewIdentityForChangedPayload() = runBlocking {
        val repository = RecordingSavedRepository()
        repository.saveResult = { command ->
            if (command.id == "rejected") SaveOutcome.Rejected(command, "No admission occurred")
            else SaveOutcome.Journaled(command, "mutation-two")
        }
        withAtom(repository) { atom ->
            atom.awaitPhase(SavePhase.EDITING)
            atom.dispatchAndReduce(SaveFlowIntent.Toggle("weekend"))
            atom.dispatchAndReduce(SaveFlowIntent.Submit("rejected"))
            atom.awaitPhase(SavePhase.FAILED)
            atom.dispatchAndReduce(SaveFlowIntent.Toggle("favorites"))
            atom.dispatchAndReduce(SaveFlowIntent.Submit("fresh-command"))
            atom.awaitPhase(SavePhase.JOURNALED)
            assertEquals(
                listOf(
                    SetCollectionsCommand("rejected", trail.id, setOf("weekend")),
                    SetCollectionsCommand("fresh-command", trail.id, setOf("weekend", "favorites")),
                ),
                repository.saves,
            )
        }
    }

    @Test
    fun closingOldAtomCannotDeliverItsLateOutcomeToReplacementRepository() = runBlocking {
        val releaseOld = CompletableDeferred<Unit>()
        val oldRepository = RecordingSavedRepository()
        oldRepository.saveResult = { command ->
            // Models a service-owned finite operation finishing after its UI lease closes.
            withContext(NonCancellable) { releaseOld.await() }
            SaveOutcome.Journaled(command, "old-account-mutation")
        }
        val old = newAtom(oldRepository)
        try {
            old.awaitPhase(SavePhase.EDITING)
            old.dispatchAndReduce(SaveFlowIntent.Toggle("weekend"))
            old.dispatchAndReduce(SaveFlowIntent.Submit("old-account-command"))
            withTimeout(5_000) { while (oldRepository.saves.isEmpty()) kotlinx.coroutines.yield() }
            old.close()
            val replacementRepository = RecordingSavedRepository()
            withAtom(replacementRepository) { replacement ->
                replacement.awaitPhase(SavePhase.EDITING)
                releaseOld.complete(Unit)
                withTimeout(5_000) { old.awaitDisposed() }
                assertTrue(replacementRepository.saves.isEmpty())
                assertEquals(SavePhase.EDITING, replacement.state.value.phase)
                replacement.dispatchAndReduce(SaveFlowIntent.Toggle("favorites"))
                replacement.dispatchAndReduce(SaveFlowIntent.Submit("replacement-command"))
                replacement.awaitPhase(SavePhase.JOURNALED)
                assertEquals(listOf("old-account-command"), oldRepository.saves.map { it.id })
                assertEquals(listOf("replacement-command"), replacementRepository.saves.map { it.id })
            }
        } finally {
            releaseOld.complete(Unit)
            old.close()
            withTimeout(5_000) { old.awaitDisposed() }
        }
    }

    @Test
    fun projectionFailureAfterJournaledOutcomeDoesNotRelabelAdmission() = runBlocking {
        val repository = RecordingSavedRepository()
        withAtom(repository) { atom ->
            atom.awaitPhase(SavePhase.EDITING)
            atom.dispatchAndReduce(SaveFlowIntent.Toggle("weekend"))
            atom.dispatchAndReduce(SaveFlowIntent.Submit("durable-command"))
            val admitted = atom.awaitPhase(SavePhase.JOURNALED)
            repository.state.value = LoadState(loading = false, error = "Subsequent inspection failed")
            atom.dispatchAndReduce(SaveFlowIntent.LoadChoices)
            atom.dispatchAndReduce(SaveFlowIntent.Submit("accidental-resubmit"))
            assertEquals(admitted, atom.state.value)
            assertEquals(1, repository.saves.size)
        }
    }

    @Test
    fun unknownMembershipCannotBecomeAnEmptySubmission() = runBlocking {
        val repository = RecordingSavedRepository()
        repository.state.value = LoadState(loading = false, error = "Account projection unavailable")
        withAtom(repository) { atom ->
            atom.awaitPhase(SavePhase.UNAVAILABLE)
            atom.dispatchAndReduce(SaveFlowIntent.Toggle("weekend"))
            atom.dispatchAndReduce(SaveFlowIntent.Submit("must-not-admit"))
            assertTrue(repository.saves.isEmpty())
            assertEquals(SavePhase.UNAVAILABLE, atom.state.value.phase)
        }
    }

    @Test
    fun missingTrailMembershipInLoadedSnapshotStaysUnavailableUntilRetryReadsIt() = runBlocking {
        val repository = RecordingSavedRepository()
        repository.state.value = LoadState(data = snapshot.copy(memberships = emptyMap()), loading = false)
        withAtom(repository) { atom ->
            val result = withTimeout(5_000) { atom.state.first { it.phase != SavePhase.LOADING } }
            assertEquals(SavePhase.UNAVAILABLE, result.phase)
            assertEquals(1, repository.refreshes)
            atom.dispatchAndReduce(SaveFlowIntent.Toggle("favorites"))
            atom.dispatchAndReduce(SaveFlowIntent.Submit("unknown-membership-must-not-admit"))
            assertTrue(repository.saves.isEmpty())

            repository.refreshResult = {
                repository.state.value = LoadState(data = snapshot.copy(memberships = mapOf(trail.id to setOf("weekend"))), loading = false)
            }
            atom.dispatchAndReduce(SaveFlowIntent.LoadChoices)
            val ready = atom.awaitPhase(SavePhase.EDITING)
            assertEquals(setOf("weekend"), ready.original)
            assertEquals(setOf("weekend"), ready.selected)
            assertEquals(2, repository.refreshes)
            assertTrue(repository.saves.isEmpty())
        }
    }

    @Test
    fun missingTrailMembershipIsRefreshedBeforeEditingExistingCollections() = runBlocking {
        val repository = RecordingSavedRepository()
        repository.state.value = LoadState(data = snapshot.copy(memberships = emptyMap()), loading = false)
        repository.refreshResult = {
            repository.state.value = LoadState(data = snapshot.copy(memberships = mapOf(trail.id to setOf("weekend"))), loading = false)
        }
        withAtom(repository) { atom ->
            val ready = atom.awaitPhase(SavePhase.EDITING)
            assertEquals(1, repository.refreshes)
            assertEquals(setOf("weekend"), ready.original)
            assertEquals(setOf("weekend"), ready.selected)
            atom.dispatchAndReduce(SaveFlowIntent.Toggle("favorites"))
            atom.dispatchAndReduce(SaveFlowIntent.Submit("preserve-known-weekend"))
            atom.awaitPhase(SavePhase.JOURNALED)
            assertEquals(listOf(SetCollectionsCommand("preserve-known-weekend", trail.id, setOf("weekend", "favorites"))), repository.saves)
        }
    }

    @Test
    fun removingKnownMembershipRequiresConfirmationButNeverSavedEmptyCannotSubmit() = runBlocking {
        val repository = RecordingSavedRepository()
        withAtom(repository) { atom ->
            atom.awaitPhase(SavePhase.EDITING)
            atom.dispatchAndReduce(SaveFlowIntent.Submit("empty-new-save"))
            assertTrue(repository.saves.isEmpty())
            assertEquals(SavePhase.EDITING, atom.state.value.phase)
        }
        repository.state.value = LoadState(data = snapshot.copy(memberships = mapOf(trail.id to setOf("weekend"))), loading = false)
        withAtom(repository) { atom ->
            atom.awaitPhase(SavePhase.EDITING)
            atom.dispatchAndReduce(SaveFlowIntent.Toggle("weekend"))
            atom.dispatchAndReduce(SaveFlowIntent.Submit("remove"))
            assertEquals(SavePhase.CONFIRM_REMOVE, atom.state.value.phase)
            assertTrue(repository.saves.isEmpty())
            atom.dispatchAndReduce(SaveFlowIntent.Submit("confirmed-remove", confirmedRemoval = true))
            atom.awaitPhase(SavePhase.JOURNALED)
            assertEquals(listOf(SetCollectionsCommand("confirmed-remove", trail.id, emptySet())), repository.saves)
        }
    }

    private fun CoroutineScope.newAtom(repository: SavedRepository): SaveTrailAtom =
        SaveTrailAtom(this, InMemoryStateHandle(SaveFlowState(trail)), repository).also {
            it.onStart()
            assertIs<Admission.Accepted>(it.intent(SaveFlowIntent.LoadChoices))
        }

    private suspend fun CoroutineScope.withAtom(repository: SavedRepository, block: suspend (SaveTrailAtom) -> Unit) {
        val atom = newAtom(repository)
        try { block(atom) } finally { atom.close(); withTimeout(5_000) { atom.awaitDisposed() } }
    }

    private suspend fun SaveTrailAtom.awaitPhase(phase: SavePhase): SaveFlowState =
        withTimeout(5_000) { state.first { it.phase == phase } }

    private suspend fun SaveTrailAtom.dispatchAndReduce(intent: SaveFlowIntent) {
        val accepted = assertIs<Admission.Accepted>(this.intent(intent))
        withTimeout(5_000) { status.first { it.lastReducedSequence >= accepted.sequence } }
    }

    private class RecordingSavedRepository : SavedRepository {
        override val state = MutableStateFlow(LoadState(data = snapshot, loading = false))
        val saves = mutableListOf<SetCollectionsCommand>()
        val reconciliations = mutableListOf<SetCollectionsCommand>()
        var refreshes = 0
        var refreshResult: suspend () -> Unit = {}
        var saveResult: suspend (SetCollectionsCommand) -> SaveOutcome = { SaveOutcome.Journaled(it, "mutation-one") }
        override suspend fun save(command: SetCollectionsCommand): SaveOutcome { saves += command; return saveResult(command) }
        override suspend fun reconcile(command: SetCollectionsCommand): SaveOutcome {
            reconciliations += command
            return SaveOutcome.Journaled(command, "mutation-one")
        }
        override suspend fun refresh() { refreshes++; refreshResult() }
        override suspend fun retryPending() = Unit
    }

    private companion object {
        val trail = Trail("alpine-lake-loop", "Alpine Lake Loop", "Alpine", "A lake walk.", TrailDifficulty.MODERATE, 6400, 320, 120, 4.9, 128, setOf(TrailFeature.LAKE), 0)
        val snapshot = SavedSnapshot(listOf(TrailCollection("weekend", "Weekend adventures"), TrailCollection("favorites", "My favorites")), mapOf(trail.id to emptySet()), listOf(trail), emptyMap())
    }
}
