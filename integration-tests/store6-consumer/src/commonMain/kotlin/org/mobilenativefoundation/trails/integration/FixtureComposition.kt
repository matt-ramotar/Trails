@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class)

package org.mobilenativefoundation.trails.integration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import dev.mattramotar.atom.runtime.Admission
import dev.mattramotar.atom.runtime.AtomLifecycle
import dev.mattramotar.atom.runtime.RuntimeStatus
import dev.mattramotar.atom.runtime.compose.AtomCompositionLocals
import dev.mattramotar.atom.runtime.compose.atom
import dev.mattramotar.atom.runtime.factory.AtomFactoryRegistry
import dev.mattramotar.atom.runtime.factory.Atoms
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import kotlin.reflect.KClass
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store6.core.Freshness
import org.mobilenativefoundation.store6.core.StoreResult

class FixtureScope private constructor()

@DependencyGraph(FixtureScope::class)
interface FixtureGraph {
    val presenter: FixturePresenter
    val ui: FixtureUi

    @DependencyGraph.Factory
    interface Factory {
        fun create(@Provides services: FixtureServices): FixtureGraph
    }
}

fun fixtureGraph(services: FixtureServices) = createGraphFactory<FixtureGraph.Factory>().create(services)

sealed interface FixtureIntent : CircuitUiEvent {
    data object Save : FixtureIntent
    data object Offline : FixtureIntent
    data object Reconnect : FixtureIntent
    data object Inspect : FixtureIntent
}

data class FixtureState(
    val save: SaveState,
    val durable: DurableStatus,
    val saved: Boolean?,
    val message: String?,
    val send: (FixtureIntent) -> Unit,
) : CircuitUiState

@Inject
class FixturePresenter(private val services: FixtureServices) : Presenter<FixtureState> {
    @Composable
    override fun present(): FixtureState {
        val save = atom<SaveAtom>(key = services.account)
        val saveState by save.state.collectAsState()
        val runtime by save.status.collectAsState()
        val durable by services.status.collectAsState()
        val key = remember(services.account) { MembershipKey(services.account, "weekend", "eagle-peak") }
        // A fresh observation after inspection avoids assuming active-stream convergence at ACK.
        var saved by remember(services) { mutableStateOf<Boolean?>(null) }
        var readError by remember(services) { mutableStateOf<String?>(null) }
        LaunchedEffect(services, durable.revision) {
            try {
                services.memberships.stream(key, Freshness.LocalOnly).collect { result ->
                    when (result) {
                        is StoreResult.Data -> { saved = result.value.saved; readError = null }
                        is StoreResult.Error -> readError = result.error.toString().take(200)
                        else -> Unit
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                readError = failure.message?.take(200) ?: "Saved data is unavailable"
            }
        }
        LaunchedEffect(saveState) {
            if (saveState is SaveState.Journaled || saveState is SaveState.Uncertain) services.inspect()
        }
        var message by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        val displayedSave = if ((runtime is RuntimeStatus.Closed || runtime is RuntimeStatus.Failed) && saveState is SaveState.Enqueueing) {
            SaveState.Uncertain((saveState as SaveState.Enqueueing).command, "The save owner stopped. Inspect durable state before retrying.")
        } else saveState
        return FixtureState(displayedSave, durable, saved, message ?: readError) { intent ->
            when (intent) {
                FixtureIntent.Save -> message = when (save.intent(SaveIntent.Save(SaveCommand("save-${Random.nextLong().toULong().toString(16)}", key)))) {
                    is Admission.Accepted -> null
                    Admission.Full -> "The transient command queue is full."
                    Admission.Closed -> "The save owner has closed."
                }
                FixtureIntent.Offline -> scope.launch {
                    services.applyOffline(true)
                }
                FixtureIntent.Reconnect -> scope.launch {
                    services.reconnect()
                }
                FixtureIntent.Inspect -> scope.launch { services.inspect() }
            }
        }
    }
}

@Inject
class FixtureUi : Ui<FixtureState> {
    @Composable
    override fun Content(state: FixtureState, modifier: Modifier) {
        Column(modifier.padding(24.dp)) {
            Text("Store6 and Atom consumer fixture")
            Text("Eagle Peak · Weekend")
            Text("Saved projection: ${state.saved}")
            Text("Pending: ${state.durable.pending}; parked: ${state.durable.parked}")
            Text("Fake backend Offline: ${state.durable.offline}")
            Text("Transient save: ${state.save}")
            state.message?.let { Text(it) }
            state.durable.operationError?.let { Text("Account operation: $it") }
            state.durable.inspectionError?.let { Text("Status unavailable; counts are last known: $it") }
            Button(onClick = { state.send(FixtureIntent.Offline) }) { Text("Apply Offline") }
            Button(enabled = state.save !is SaveState.Enqueueing && state.save !is SaveState.Uncertain, onClick = { state.send(FixtureIntent.Save) }) {
                Text("Save to Weekend")
            }
            Button(onClick = { state.send(FixtureIntent.Reconnect) }) { Text("Reconnect and drain") }
            Button(onClick = { state.send(FixtureIntent.Inspect) }) { Text("Check durable status") }
        }
    }
}

@Composable
fun FixtureComposition(services: FixtureServices, content: @Composable (FixtureGraph) -> Unit) {
    val graph = remember(services) { fixtureGraph(services) }
    val registry = remember(services) {
        val entry = Atoms.factory<SaveAtom, SaveState, Unit>(
            create = { scope, handle, _ -> SaveAtom(scope, handle, services) },
            initial = { SaveState.Idle },
            serializer = null,
        )
        object : AtomFactoryRegistry {
            override fun entryFor(type: KClass<out AtomLifecycle>) = if (type == SaveAtom::class) entry else null
        }
    }
    // The provider must activate staged reservations after composition is remembered.
    AtomCompositionLocals(factories = registry) {
        content(graph)
    }
}
