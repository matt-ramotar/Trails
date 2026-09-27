package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** HeroUI Native Slider geometry with Compose range, keyboard, and accessibility behavior. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailsSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String,
    valueDescription: String,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = TrailsTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val dragged by interaction.collectIsDraggedAsState()
    val motionEnabled = trailsMotionEnabled()
    val thumbScale by animateFloatAsState(
        if (pressed || dragged) 0.9f else 1f,
        if (motionEnabled) spring(dampingRatio = 0.75f, stiffness = 400f) else tween(0),
    )
    val shape = RoundedCornerShape(TrailsTheme.radii.pill)
    Slider(
        value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps,
        enabled = enabled, onValueChangeFinished = onValueChangeFinished, interactionSource = interaction,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
            // Put the label on the native SeekBar, rather than an independent description child.
            text = AnnotatedString(label)
            stateDescription = valueDescription
        },
        thumb = {
            // The 28 x 20 visual is inset inside the native 48 dp interaction and semantics bounds.
            Box(Modifier.size(width = 28.dp, height = 48.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(width = 28.dp, height = 20.dp)
                        .alpha(if (enabled) 1f else 0.5f)
                        .border(if (focused) 2.dp else 0.dp, colors.textPrimary, shape)
                        .background(colors.accent, shape).padding(2.dp),
                ) {
                    Box(Modifier.matchParentSize().scale(thumbScale).trailsShadow(shape, TrailsShadow.Field).background(colors.onAccent, shape))
                }
            }
        },
        track = { state ->
            Canvas(Modifier.fillMaxWidth().height(20.dp).alpha(if (enabled) 1f else 0.5f)) {
                val radius = CornerRadius(size.height / 2f)
                // Extend beneath the visual thumb so it stays wholly inside the rail at both ends.
                val thumbHalf = 14.dp.toPx()
                val railWidth = size.width + 2 * thumbHalf
                drawRoundRect(colors.soft, topLeft = Offset(-thumbHalf, 0f), size = Size(railWidth, size.height), cornerRadius = radius)
                val activeWidth = size.width * state.coercedValueAsFraction + 2 * thumbHalf
                val start = if (layoutDirection == LayoutDirection.Rtl) size.width + thumbHalf - activeWidth else -thumbHalf
                drawRoundRect(colors.accent, topLeft = Offset(start, 0f), size = Size(activeWidth, size.height), cornerRadius = radius)
            }
        },
    )
}

/** HeroUI ControlField: one labeled row owns the switch action and its state. */
@Composable
fun TrailsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    val colors = TrailsTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).alpha(if (enabled) 1f else 0.5f)
            .trailsFocusRing(interaction, RoundedCornerShape(8.dp), enabled)
            .toggleable(checked, interactionSource = interaction, indication = null, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = TrailsTheme.typography.titleSmall, color = if (enabled) colors.textPrimary else colors.textSecondary)
            description?.let { Text(it, style = TrailsTheme.typography.bodySmall, color = colors.textSecondary) }
        }
        TrailsSwitchIndicator(checked, pressed)
    }
}

@Composable
private fun TrailsSwitchIndicator(checked: Boolean, pressed: Boolean) {
    val colors = TrailsTheme.colors
    val motionEnabled = trailsMotionEnabled()
    val position by animateFloatAsState(if (checked) 1f else 0f, if (motionEnabled) spring(dampingRatio = 1.06066f, stiffness = 800f) else tween(0))
    val pressedScale by animateFloatAsState(if (pressed && motionEnabled) 0.96f else 1f, tween(if (motionEnabled) 150 else 0))
    val trackColor by animateColorAsState(if (checked) colors.accent else colors.soft, tween(if (motionEnabled) 175 else 0, easing = TrailsEase))
    val thumbColor by animateColorAsState(if (checked) colors.onAccent else colors.surface, tween(if (motionEnabled) 175 else 0, easing = TrailsEase))
    val shape = RoundedCornerShape(TrailsTheme.radii.pill)
    Box(
        Modifier.size(width = 48.dp, height = 24.dp).scale(pressedScale)
            .clip(shape).background(trackColor).clearAndSetSemantics {},
    ) {
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = 2.dp + 16.dp * position)
                .size(width = 28.dp, height = 20.dp).trailsShadow(shape, TrailsShadow.Field).background(thumbColor, shape),
        )
    }
}

/** HeroUI Checkbox indicator; a containing toggleable row supplies the single accessible action. */
@Composable
fun TrailsCheckbox(checked: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, pressed: Boolean = false) {
    val colors = TrailsTheme.colors
    val shape = RoundedCornerShape(8.dp)
    val motionEnabled = trailsMotionEnabled()
    val indicatorProgress by animateFloatAsState(if (checked) 1f else 0f, tween(if (motionEnabled) 100 else 0))
    val indicatorRadius by animateDpAsState(if (checked) 0.dp else 8.dp, tween(if (motionEnabled) 50 else 0))
    val pressedScale by animateFloatAsState(if (pressed && motionEnabled) 0.96f else 1f, tween(if (motionEnabled) 150 else 0))
    Box(modifier.size(48.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(24.dp).alpha(if (enabled) 1f else 0.5f).scale(pressedScale)
                .trailsShadow(shape, TrailsShadow.Field).clip(shape).background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.matchParentSize().graphicsLayer {
                    alpha = indicatorProgress
                    translationX = (-4).dp.toPx() * (1f - indicatorProgress)
                    scaleX = 0.8f + 0.2f * indicatorProgress
                    scaleY = scaleX
                }.background(colors.accent, RoundedCornerShape(indicatorRadius)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Solid.Check.painter, null, Modifier.size(16.dp), tint = colors.onAccent)
            }
        }
    }
}
