package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@Composable
fun TrailsLoading(label: String, modifier: Modifier = Modifier) {
    val colors = TrailsTheme.colors
    Row(modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(24.dp), color = colors.accent, strokeWidth = 2.dp)
        Text(label, style = TrailsTheme.typography.bodyLarge, color = colors.textSecondary)
    }
}

@Composable
fun TrailsHeading(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.semantics { heading() }, style = TrailsTheme.typography.displayLarge, color = TrailsTheme.colors.textPrimary)
}
