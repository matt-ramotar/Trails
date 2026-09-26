package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import trails.multiplatform.foundation.designsystem.generated.resources.Res
import trails.multiplatform.foundation.designsystem.generated.resources.cancel_01_stroke_rounded
import trails.multiplatform.foundation.designsystem.generated.resources.search_01_stroke_rounded
import trails.multiplatform.foundation.designsystem.generated.resources.trails_compass_mark

@Composable
fun TrailsCompass(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = Color.Unspecified,
) {
    Icon(
        painterResource(Res.drawable.trails_compass_mark),
        contentDescription,
        modifier,
        tint = if (tint == Color.Unspecified) TrailsTheme.colors.accent else tint,
    )
}

/** Lowercase Manrope branding. The screen supplies its own accessible heading. */
@Composable
fun TrailsBrand(modifier: Modifier = Modifier) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    Row(modifier.clearAndSetSemantics {}, verticalAlignment = Alignment.CenterVertically) {
        TrailsCompass(Modifier.size(24.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "trails",
            color = colors.accent,
            style = typography.displayLarge.copy(fontSize = 32.sp, lineHeight = 39.sp, letterSpacing = (-1.28).sp),
        )
    }
}

/** Compatibility wrapper. Use [TrailsButton] to choose a [ButtonTone] directly. */
@Composable
fun TrailsControlsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    secondary: Boolean = false,
    accessibilityLabel: String? = null,
) = TrailsButton(
    text, onClick, modifier,
    tone = if (secondary) ButtonTone.Secondary else ButtonTone.Commit,
    enabled = enabled, loading = loading, accessibilityLabel = accessibilityLabel,
)

@Composable
fun TrailsFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        label = { Text(text, style = typography.labelMedium) },
        leadingIcon = leadingIcon,
        shape = RoundedCornerShape(TrailsTheme.radii.pill),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.soft,
            labelColor = colors.textPrimary,
            selectedContainerColor = colors.accent,
            selectedLabelColor = colors.onAccent,
        ),
    )
}

@Composable
fun TrailsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Find a trail or a place",
) {
    val colors = TrailsTheme.colors
    val typography = TrailsTheme.typography
    val shape = RoundedCornerShape(TrailsTheme.radii.pill)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    BasicTextField(
        value,
        onValueChange,
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(colors.soft, shape)
            .border(if (focused) 2.dp else 0.dp, if (focused) colors.accent else Color.Transparent, shape)
            .semantics { contentDescription = "Search trails" },
        textStyle = typography.bodyMedium.copy(color = colors.textPrimary),
        singleLine = true,
        interactionSource = interaction,
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(); keyboard?.hide() }),
        decorationBox = { input ->
            Row(Modifier.padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(Res.drawable.search_01_stroke_rounded), null, Modifier.size(20.dp), tint = colors.textSecondary)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
                    if (value.isEmpty()) Text(placeholder, color = colors.textSecondary, style = typography.bodyMedium)
                    input()
                }
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(48.dp)) {
                        Icon(painterResource(Res.drawable.cancel_01_stroke_rounded), "Clear search", Modifier.size(20.dp), tint = colors.textSecondary)
                    }
                }
            }
        },
    )
}
