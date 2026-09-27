package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** ControlField composition: one action owns the checkbox and its wrapping label. */
@Composable
fun TrailsCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp)
            .trailsFocusRing(interaction, RoundedCornerShape(24.dp), enabled)
            .toggleable(
                value = checked, enabled = enabled, role = Role.Checkbox,
                interactionSource = interaction, indication = null,
                onValueChange = onCheckedChange,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrailsCheckbox(checked, enabled = enabled, pressed = pressed)
        Text(
            label, style = TrailsTheme.typography.bodyLarge,
            color = TrailsTheme.colors.textPrimary.copy(alpha = if (enabled) 1f else 0.5f),
            modifier = Modifier.weight(1f),
        )
    }
}
