package org.mobilenativefoundation.trails.feature.developertools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsAlert
import org.mobilenativefoundation.trails.foundation.designsystem.component.StatusKind
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsSegmentedControl
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsSelect
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsSlider
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsSwitchRow
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsChip
import org.mobilenativefoundation.trails.foundation.designsystem.component.ChipTone
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsListGroup
import org.mobilenativefoundation.trails.foundation.designsystem.component.TrailsSeparator
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import kotlin.math.roundToInt

internal enum class DeveloperToolsSection { Network, Sync, Session }

@Composable
internal fun DeveloperToolsTabs(selected: DeveloperToolsSection, onSelected: (DeveloperToolsSection) -> Unit) {
    TrailsSegmentedControl(DeveloperToolsSection.entries, selected, onSelected, optionLabel = { it.name })
}

@Composable
internal fun DeveloperToolsStatus(label: String) {
    TrailsChip(
        label,
        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        tone = when (label) {
            "Applied" -> ChipTone.Success
            "Failed" -> ChipTone.Danger
            else -> ChipTone.Default
        },
    )
}

@Composable
internal fun DeveloperToolsNotice(message: String, isError: Boolean = false) {
    TrailsAlert(message, if (isError) StatusKind.FAILED else StatusKind.INFO)
}

@Composable
internal fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    TrailsListGroup(Modifier.fillMaxWidth(), content = content)
}

@Composable
internal fun SettingsDivider() {
    TrailsSeparator(Modifier.padding(horizontal = TrailsTheme.spacing.lg))
}

@Composable
internal fun SettingsToggle(label: String, hint: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    TrailsSwitchRow(label, checked, onCheckedChange, Modifier.padding(TrailsTheme.spacing.lg), description = hint, enabled = enabled)
}

@Composable
internal fun SettingsSlider(
    label: String,
    valueText: String,
    valueDescription: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    val colors = TrailsTheme.colors
    Column(Modifier.fillMaxWidth().padding(TrailsTheme.spacing.lg)) {
        Text(label, style = TrailsTheme.typography.titleSmall, color = if (enabled) colors.textPrimary else colors.textSecondary)
        Text(valueText, style = TrailsTheme.typography.bodyMedium, color = if (enabled) colors.accent else colors.textSecondary)
        TrailsSlider(
            value = value,
            onValueChange = { raw ->
                val steps = ((raw.coerceIn(valueRange) - valueRange.start) / step).roundToInt()
                onValueChange((valueRange.start + steps * step).coerceIn(valueRange))
            },
            valueRange = valueRange, label = label, valueDescription = valueDescription, enabled = enabled,
            steps = ((valueRange.endInclusive - valueRange.start) / step).roundToInt() - 1,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}

@Composable
internal fun <T : Enum<T>> SettingsSelect(
    label: String,
    hint: String,
    selected: T,
    options: List<T>,
    enabled: Boolean,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
) {
    val colors = TrailsTheme.colors
    Column(Modifier.fillMaxWidth().padding(TrailsTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = TrailsTheme.typography.titleSmall, color = if (enabled) colors.textPrimary else colors.textSecondary)
        Text(hint, style = TrailsTheme.typography.bodySmall, color = colors.textSecondary)
        TrailsSelect(label, selected, options, optionLabel, onSelected, enabled = enabled)
    }
}
