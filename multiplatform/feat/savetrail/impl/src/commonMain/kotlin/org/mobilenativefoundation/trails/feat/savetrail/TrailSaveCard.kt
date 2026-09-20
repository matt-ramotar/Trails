package org.mobilenativefoundation.trails.feat.savetrail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

fun TrailDifficulty.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
fun trailDistance(meters: Int): String = "${meters / 1000}.${(meters % 1000) / 100} km"
fun trailDuration(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes < 1440 -> "${minutes / 60} h ${minutes % 60} min"
    else -> {
        val days = minutes / 1440
        val hours = (minutes % 1440) / 60
        val dayLabel = "$days ${if (days == 1) "day" else "days"}"
        if (hours == 0) dayLabel else "$dayLabel $hours h"
    }
}

data class SyncStatus(val kind: StatusKind, val message: String, val canRetry: Boolean = false)

/** R2 copy for one trail's sync state. [detailed] adds the local-save clause used on detail and in the sheet. */
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

fun TrailDifficulty.markerKind(): DifficultyMarkerKind = when (this) {
    TrailDifficulty.EASY -> DifficultyMarkerKind.EASY
    TrailDifficulty.MODERATE -> DifficultyMarkerKind.MODERATE
    TrailDifficulty.HARD -> DifficultyMarkerKind.HARD
    TrailDifficulty.STRENUOUS -> DifficultyMarkerKind.STRENUOUS
}

/** Derived from the catalog's feature tags and duration; nothing is authored per route. */
fun Trail.highlight(): String? = when {
    durationMinutes >= 1440 -> "Multi-day"
    TrailFeature.WATERFALL in features -> "Waterfall"
    TrailFeature.SUMMIT in features -> "Summit views"
    TrailFeature.LAKE in features -> "Lakeside"
    TrailFeature.FOREST in features -> "Forest"
    else -> null
}

/** R2 facts line shared by cards, rows and the detail rating row; announced as one line. */
@Composable
fun TrailFactsRow(
    trail: Trail,
    modifier: Modifier = Modifier,
    showCount: Boolean = true,
    trailing: String = trailDistance(trail.distanceMeters),
) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Row(
        modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Star.painter, contentDescription = null, Modifier.size(14.dp), tint = colors.textPrimary)
        Text(if (showCount) "${trail.rating} (${trail.reviewCount})" else "${trail.rating}", style = typography.bodyMedium, color = colors.textSecondary)
        Text("·", style = typography.bodyMedium, color = colors.textSecondary)
        DifficultyMarker(trail.difficulty.markerKind())
        Text(trail.difficulty.displayName(), style = typography.bodyMedium, color = colors.textSecondary)
        Text("·", style = typography.bodyMedium, color = colors.textSecondary)
        Text(trailing, style = typography.bodyMedium, color = colors.textSecondary)
    }
}

@Composable
private fun HighlightChip(text: String, modifier: Modifier = Modifier) {
    val colors = TrailsTheme.colors
    val shape = RoundedCornerShape(TrailsTheme.radii.pill)
    Row(
        modifier.shadow(4.dp, shape).clip(shape).background(colors.surface).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Eye.painter, contentDescription = null, Modifier.size(14.dp), tint = colors.textPrimary)
        Text(text, style = TrailsTheme.typography.labelMedium, color = colors.textPrimary)
    }
}

/** A shared save affordance with separate card navigation and heart touch targets. */
@Composable
fun TrailSaveCard(trail: Trail, snapshot: SavedSnapshot?, onOpen: () -> Unit, onSave: (() -> Unit) -> Unit, modifier: Modifier = Modifier) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val saved = snapshot?.memberships?.get(trail.id)?.isNotEmpty()
    val sync = snapshot?.syncByTrail?.get(trail.id)
    Column(modifier.fillMaxWidth().clickable(onClick = onOpen), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().height(233.dp).clip(RoundedCornerShape(TrailsTheme.radii.lg))) {
            TrailPhoto(trail.id, modifier = Modifier.fillMaxSize())
            trail.highlight()?.let { HighlightChip(it, Modifier.align(Alignment.TopStart).padding(12.dp)) }
            TrailBookmark(trail.name, saved, onSave, Modifier.align(Alignment.TopEnd).padding(12.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(trail.name, style = typography.titleSmall.copy(fontSize = 18.sp, lineHeight = 24.sp), color = colors.textPrimary)
            Text(trail.region, style = typography.bodyMedium, color = colors.textSecondary)
            TrailFactsRow(trail)
            syncStatus(sync, snapshot?.offline == true)?.let { TrailsStatusLine(it.kind, it.message) }
        }
    }
}

@Composable
fun TrailBookmark(name: String, saved: Boolean?, onClick: (() -> Unit) -> Unit, modifier: Modifier = Modifier) {
    val colors = TrailsTheme.colors
    val focusReturn = rememberTrailsFocusReturnTarget()
    val label = if (saved == true) "Edit saved collections for $name" else "Save $name"
    TrailsIconCircle(
        icon = if (saved == true) Icons.Outlined.HeartFilled.painter else Icons.Outlined.Favorite.painter,
        contentDescription = null,
        onClick = { onClick(focusReturn::restore) },
        modifier = modifier.then(focusReturn.modifier).semantics {
            text = AnnotatedString(label)
            stateDescription = when (saved) { true -> "Saved"; false -> "Not saved"; null -> "Saved status unavailable" }
        },
        tint = if (saved == true) colors.accent else colors.textPrimary,
    )
}

@Composable
fun M1Loading(label: String, modifier: Modifier = Modifier) {
    val colors = TrailsTheme.colors
    Row(modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(24.dp), color = colors.accent, strokeWidth = 2.dp)
        Text(label, style = TrailsTheme.typography.bodyLarge, color = colors.textSecondary)
    }
}

@Composable
fun M1Heading(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.semantics { heading() }, style = TrailsTheme.typography.displayLarge, color = TrailsTheme.colors.textPrimary)
}
