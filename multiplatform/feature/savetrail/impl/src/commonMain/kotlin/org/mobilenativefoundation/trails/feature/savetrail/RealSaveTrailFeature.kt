@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.mobilenativefoundation.trails.feature.savetrail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.mattramotar.atom.runtime.Admission
import dev.mattramotar.atom.runtime.state.InMemoryStateHandle
import dev.zacsweers.metro.Inject
import kotlin.uuid.Uuid
import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.saved.SavedRepository

@Inject
class RealSaveTrailFeature(private val repository: SavedRepository) : SaveTrailFeature {
    private class Request(val trail: Trail, val onDismiss: () -> Unit)
    private var request by mutableStateOf<Request?>(null)
    private var toast by mutableStateOf<TrailsToastData?>(null)

    @Composable
    override fun Toast(modifier: Modifier) { TrailsToastHost(toast, onDismissed = { toast = null }, modifier) }

    override fun open(trail: Trail, onDismiss: () -> Unit) { if (request == null) request = Request(trail, onDismiss) }

    private fun dismiss(current: Request, returnFocus: Boolean = true) {
        if (request !== current) return
        request = null
        if (returnFocus) current.onDismiss()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(onViewSaved: (collectionId: String?) -> Unit) {
        val current = request ?: return
        key(current) {
            val colors = TrailsTheme.colors
            val typography = TrailsTheme.typography
            val scope = rememberCoroutineScope()
            val atom = remember(current, repository) { SaveTrailAtom(scope, InMemoryStateHandle(SaveFlowState(current.trail)), repository) }
            DisposableEffect(atom) {
                atom.onStart()
                atom.intent(SaveFlowIntent.LoadChoices)
                onDispose { atom.close() }
            }
            val state by atom.state.collectAsState()
            val completion = if (state.phase == SavePhase.JOURNALED) {
                val names = state.collections.filter { it.id in state.selected }.map { it.name }
                when { state.selected.isEmpty() -> "Removed from saved"; names.size == 1 -> "Saved to ${names.single()}"; else -> "Saved to ${names.size} lists" }
            } else null
            val viewTarget = state.selected.singleOrNull()
            LaunchedEffect(completion) {
                if (completion != null) {
                    toast = TrailsToastData(completion, actionLabel = if (state.selected.isEmpty()) null else "View") { toast = null; onViewSaved(viewTarget) }
                    dismiss(current)
                }
            }
            var dispatchError by remember { mutableStateOf<String?>(null) }
            fun send(intent: SaveFlowIntent) {
                dispatchError = when (atom.intent(intent)) {
                    is Admission.Accepted -> null
                    Admission.Full -> "Please wait for your current action to finish."
                    Admission.Closed -> "This save session has closed. Your admitted changes are kept."
                }
            }
            val latestCanDismiss by rememberUpdatedState(state.canDismiss)
            val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden || latestCanDismiss })
            ModalBottomSheet(
                onDismissRequest = { if (state.canDismiss) dismiss(current) },
                sheetState = sheet,
                containerColor = colors.surface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = TrailsTheme.radii.sheet, topEnd = TrailsTheme.radii.sheet),
            ) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
                        .navigationBarsPadding().imePadding().padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TrailsHeading(if (state.phase == SavePhase.CONFIRM_REMOVE) "Remove saved trail?" else "Save to a list")
                    Text(current.trail.name, style = typography.bodyLarge, color = colors.textSecondary)
                    when (state.phase) {
                        SavePhase.LOADING -> TrailsLoading("Opening your collections…")
                        SavePhase.UNAVAILABLE -> {
                            TrailsStatusLine(StatusKind.FAILED, "Couldn’t open your collections")
                            TrailsButton("Try again", { send(SaveFlowIntent.LoadChoices) }, tone = ButtonTone.Commit, modifier = Modifier.fillMaxWidth())
                        }
                        SavePhase.CONFIRM_REMOVE -> {
                            Text("This removes the trail from every collection.", style = typography.bodyMedium, color = colors.textSecondary)
                            TrailsButton("Remove from saved", { send(SaveFlowIntent.Submit(Uuid.random().toString(), confirmedRemoval = true)) }, tone = ButtonTone.Commit, modifier = Modifier.fillMaxWidth())
                            TrailsButton("Keep editing", { send(SaveFlowIntent.KeepEditing) }, tone = ButtonTone.Ghost, modifier = Modifier.fillMaxWidth())
                        }
                        SavePhase.JOURNALED -> Unit
                        else -> {
                            state.collections.forEach { collection ->
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 52.dp).toggleable(
                                        value = collection.id in state.selected, enabled = state.editable, role = Role.Checkbox,
                                        onValueChange = { send(SaveFlowIntent.Toggle(collection.id)) },
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(collection.id in state.selected, onCheckedChange = null, enabled = state.editable, colors = CheckboxDefaults.colors(checkedColor = colors.accent))
                                    Spacer(Modifier.width(12.dp))
                                    Text(collection.name, style = typography.bodyLarge, color = colors.textPrimary)
                                }
                            }
                            when (state.phase) {
                                SavePhase.ADMITTING -> TrailsStatusLine(StatusKind.PENDING, "Saving on this device…")
                                SavePhase.RECONCILING -> TrailsStatusLine(StatusKind.PENDING, "Checking your save…")
                                SavePhase.UNKNOWN -> {
                                    TrailsStatusLine(StatusKind.PENDING, "Checking your save…")
                                    TrailsButton("Check save", { send(SaveFlowIntent.Reconcile) }, tone = ButtonTone.Commit, modifier = Modifier.fillMaxWidth())
                                }
                                else -> {
                                    if (state.phase == SavePhase.FAILED) TrailsStatusLine(StatusKind.FAILED, "Couldn’t save · Your choices are still here")
                                    if (state.collections.isEmpty()) TrailsStatusLine(StatusKind.INFO, "No collections are available for this account")
                                    // One decision names the action and its hierarchy; saving is the hero, retrying and removing commit.
                                    val (label, tone) = when {
                                        state.phase == SavePhase.FAILED -> "Try saving again" to ButtonTone.Commit
                                        state.selected.isEmpty() && state.original.isNotEmpty() -> "Remove from saved" to ButtonTone.Commit
                                        else -> "Save trail" to ButtonTone.Hero
                                    }
                                    TrailsButton(
                                        label,
                                        { send(SaveFlowIntent.Submit(Uuid.random().toString())) },
                                        tone = tone,
                                        leadingIcon = if (tone == ButtonTone.Hero) Icons.Outlined.Favorite.painter else null,
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = state.selected.isNotEmpty() || state.original.isNotEmpty(),
                                    )
                                }
                            }
                        }
                    }
                    dispatchError?.let { TrailsStatusLine(StatusKind.FAILED, it) }
                    if (state.canDismiss && state.phase != SavePhase.CONFIRM_REMOVE && state.phase != SavePhase.JOURNALED) TrailsButton("Cancel", { dismiss(current) }, tone = ButtonTone.Secondary, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
