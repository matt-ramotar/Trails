package org.mobilenativefoundation.trails.ui.trail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsCard
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsChip
import org.mobilenativefoundation.trails.foundation.designsystem.component.ChipSize
import org.mobilenativefoundation.trails.foundation.designsystem.component.DifficultyMarker
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsIconCircle
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsStatusLine
import org.mobilenativefoundation.trails.foundation.designsystem.component.rememberTrailsFocusReturnTarget
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.saved.SavedSnapshot

/** Trail facts line shared by cards, rows and the detail rating row; announced as one line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrailFactsRow(
    trail: Trail,
    modifier: Modifier = Modifier,
    showCount: Boolean = true,
    trailing: String = trailDistance(trail.distanceMeters),
) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    FlowRow(
        modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
    TrailsChip(
        label = text,
        modifier = modifier,
        size = ChipSize.Small,
        leadingContent = {
            Icon(Icons.Outlined.Eye.painter, contentDescription = null, Modifier.size(14.dp), tint = TrailsTheme.colors.textPrimary)
        },
    )
}

/** A shared save affordance with separate card navigation and heart touch targets. */
@Composable
fun TrailSaveCard(trail: Trail, snapshot: SavedSnapshot?, onOpen: () -> Unit, onSave: (() -> Unit) -> Unit, modifier: Modifier = Modifier) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val saved = snapshot?.memberships?.get(trail.id)?.isNotEmpty()
    val sync = snapshot?.syncByTrail?.get(trail.id)
    TrailsCard(modifier.fillMaxWidth(), onClick = onOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().height(233.dp).clip(RoundedCornerShape(TrailsTheme.radii.lg))) {
                TrailPhoto(trail.id, modifier = Modifier.fillMaxSize())
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(Modifier.weight(1f)) {
                        trail.highlight()?.let { HighlightChip(it) }
                    }
                    TrailBookmark(trail.name, saved, onSave, Modifier.size(48.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(trail.name, style = typography.titleMedium, color = colors.textPrimary)
                Text(trail.region, style = typography.bodyLarge, color = colors.textSecondary)
                TrailFactsRow(trail)
                syncStatus(sync, snapshot?.offline == true)?.let { TrailsStatusLine(it.kind, it.message) }
            }
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
