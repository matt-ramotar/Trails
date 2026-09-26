package org.mobilenativefoundation.trails.ui.trail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** Bundled location photography, selected by stable trail ID so legacy caches need no migration. Attribution lives in [TrailPhotoCredit]. */
@Composable
fun TrailPhoto(trailId: String, modifier: Modifier = Modifier, describeImage: Boolean = false) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val photo = trailPhotographs[trailId]
    Box(modifier.background(colors.soft)) {
        if (photo == null) {
            Text("Photo unavailable", Modifier.align(Alignment.Center), color = colors.textSecondary, style = typography.bodySmall)
        } else {
            Image(
                painterResource(photo.image),
                contentDescription = if (describeImage) photo.description else null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alignment = photo.alignment,
            )
        }
    }
}

/** Source and license stay reachable on detail and Welcome; credits also travel with the assets. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun TrailPhotoCredit(trailId: String, modifier: Modifier = Modifier) {
    val photo = trailPhotographs[trailId] ?: return
    val uriHandler = LocalUriHandler.current
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(photo.description, color = colors.textSecondary, style = typography.bodySmall)
        Text("Photo: ${photo.photographer}", color = colors.textSecondary, style = typography.labelSmall)
        FlowRow {
            TextButton({ uriHandler.openUri(photo.sourceUrl) }, Modifier.heightIn(min = 48.dp)) {
                Text("Photo source", color = colors.accent, style = typography.labelMedium)
            }
            TextButton({ uriHandler.openUri(photo.licenseUrl) }, Modifier.heightIn(min = 48.dp)) {
                Text(photo.license, color = colors.accent, style = typography.labelMedium)
            }
        }
    }
}
