package org.mobilenativefoundation.trails.ui.trail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.mobilenativefoundation.trails.data.trail.saved.SavedSnapshot
import org.mobilenativefoundation.trails.data.trail.saved.TrailSync
import org.mobilenativefoundation.trails.data.trail.saved.TrailSyncCause
import org.mobilenativefoundation.trails.data.trail.saved.TrailSyncStatus
import org.mobilenativefoundation.trails.foundation.designsystem.component.StatusKind
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsStatusLine

data class SyncStatus(val kind: StatusKind, val message: String, val canRetry: Boolean = false)

/** Display copy for one trail's sync state. [detailed] adds the local-save clause used on detail and in the sheet. */
fun syncStatus(sync: TrailSync?, offline: Boolean, detailed: Boolean = false): SyncStatus? = when (sync?.status) {
    null, TrailSyncStatus.SYNCED -> null
    TrailSyncStatus.PENDING -> when {
        sync.canRetry -> SyncStatus(StatusKind.FAILED, "Sync needs attention · Local save is kept", canRetry = true)
        detailed && offline -> SyncStatus(StatusKind.PENDING, "Saved here · Waiting for a connection")
        detailed -> SyncStatus(StatusKind.PENDING, "Saved on this device · Waiting to sync")
        offline -> SyncStatus(StatusKind.PENDING, "Waiting for a connection")
        else -> SyncStatus(StatusKind.PENDING, "Waiting to sync")
    }
    TrailSyncStatus.SYNCING -> SyncStatus(StatusKind.PENDING, "Syncing…")
    TrailSyncStatus.FINISHING -> SyncStatus(StatusKind.PENDING, "Finishing your save")
    TrailSyncStatus.PARKED -> {
        val trimmedReason = sync.reason.orEmpty().trim().trimEnd('.')
        val fallback = "Sync is paused · Saved here"
        val message = when {
            // Only the data layer's typed cause asks for an update; wording in a reason never does.
            sync.cause == TrailSyncCause.INCOMPATIBLE -> "App update needed to sync · Saved here"
            trimmedReason.isBlank() -> fallback
            "$trimmedReason · Saved here".length <= 44 -> "$trimmedReason · Saved here"
            else -> fallback
        }
        SyncStatus(StatusKind.ATTENTION, message)
    }
}

@Composable
fun TrailSyncNotice(sync: TrailSync?, offline: Boolean, syncing: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier, detailed: Boolean = true) {
    val status = syncStatus(sync, offline, detailed) ?: return
    val retryable = status.canRetry && !syncing
    TrailsStatusLine(status.kind, status.message, modifier, actionLabel = if (retryable) "Try again" else null, onAction = if (retryable) onRetry else null)
}

@Composable
fun SavedSyncNotice(snapshot: SavedSnapshot, onRetry: () -> Unit) {
    val relevant = snapshot.syncByTrail.values.filter { it.status != TrailSyncStatus.SYNCED }
    if (relevant.isEmpty()) return
    val attention = relevant.firstOrNull { it.status == TrailSyncStatus.PARKED || it.canRetry }
    val finishing = relevant.firstOrNull { it.status == TrailSyncStatus.FINISHING }
    when {
        attention != null -> TrailSyncNotice(attention, snapshot.offline, snapshot.syncing, onRetry, detailed = false)
        finishing != null -> TrailSyncNotice(finishing, snapshot.offline, snapshot.syncing, onRetry, detailed = false)
        snapshot.syncing -> TrailsStatusLine(StatusKind.PENDING, "Syncing your saved trails…")
        else -> TrailsStatusLine(
            if (snapshot.offline) StatusKind.OFFLINE else StatusKind.PENDING,
            "${relevant.size} ${if (relevant.size == 1) "change" else "changes"} waiting to sync",
        )
    }
}
