package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.flow.first
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** Measured Native Tabs indicator follows variable-width labels, including scaled text and RTL. */
@Composable
fun <T> TrailsSegmentedControl(options: List<T>, selected: T, onSelected: (T) -> Unit, optionLabel: (T) -> String, modifier: Modifier = Modifier) {
    if (options.isEmpty()) return
    val colors = TrailsTheme.colors
    val shape = RoundedCornerShape(24.dp)
    val measurements = remember { mutableStateMapOf<T, Rect>() }
    val density = LocalDensity.current
    val motionEnabled = trailsMotionEnabled()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val minimumWidth = ((maxWidth - 6.dp - 4.dp * (options.size - 1)) / options.size).coerceAtLeast(48.dp)
        Box(
            Modifier.fillMaxWidth().clip(shape).background(colors.soft).horizontalScroll(rememberScrollState()).padding(3.dp),
            contentAlignment = AbsoluteAlignment.TopLeft,
        ) {
            measurements[selected]?.let { bounds ->
                val spec = if (motionEnabled) spring<androidx.compose.ui.unit.Dp>(dampingRatio = 1.732f, stiffness = 1200f) else tween(0)
                val x by animateDpAsState(with(density) { bounds.left.toDp() }, spec)
                val width by animateDpAsState(with(density) { bounds.width.toDp() }, spec)
                val height by animateDpAsState(with(density) { bounds.height.toDp() }, spec)
                Box(Modifier.align(AbsoluteAlignment.TopLeft).absoluteOffset(x = x).width(width).height(height).trailsShadow(shape).background(colors.surface, shape).testTag("trails-segment-indicator"))
            }
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    val isSelected = option == selected
                    val interaction = remember { MutableInteractionSource() }
                    Box(
                        Modifier.widthIn(min = minimumWidth).heightIn(min = 48.dp)
                            .onGloballyPositioned { measurements[option] = it.boundsInParent() }
                            .clip(shape).trailsFocusRing(interaction, shape).selectable(isSelected, interactionSource = interaction, indication = null, role = Role.Tab, onClick = { onSelected(option) })
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(optionLabel(option), style = TrailsTheme.typography.labelLarge, color = if (isSelected) colors.textPrimary else colors.textSecondary)
                    }
                }
            }
        }
    }
}

/** Native Select visuals with Compose's focusable popup, back dismissal, and selected semantics. */
@Composable
fun <T> TrailsSelect(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val colors = TrailsTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    var expanded by remember { mutableStateOf(false) }
    var popupMounted by remember { mutableStateOf(false) }
    var positioned by remember { mutableStateOf(false) }
    var aboveAnchor by remember { mutableStateOf(false) }
    var anchorWidth by remember { mutableIntStateOf(0) }
    val focusReturn = rememberTrailsFocusReturnTarget()
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val viewportInset = with(density) { 12.dp.roundToPx() }
    val anchorGap = with(density) { 8.dp.roundToPx() }
    var maximumPopupSize by remember(windowSize, viewportInset) {
        mutableStateOf(IntSize((windowSize.width - 2 * viewportInset).coerceAtLeast(1), (windowSize.height - 2 * viewportInset).coerceAtLeast(1)))
    }
    val progress = remember { Animatable(0f) }
    val motionEnabled = trailsMotionEnabled()
    val isOpen = expanded && enabled
    LaunchedEffect(enabled) { if (!enabled) expanded = false }
    LaunchedEffect(isOpen, motionEnabled) {
        if (isOpen) {
            popupMounted = true
            snapshotFlow { positioned }.first { it }
            if (motionEnabled) progress.animateTo(1f, tween(200, easing = TrailsEaseOut)) else progress.snapTo(1f)
        } else if (popupMounted) {
            if (motionEnabled) progress.animateTo(0f, tween(150, easing = TrailsEaseOut)) else progress.snapTo(0f)
            popupMounted = false
            positioned = false
            withFrameNanos { }
            if (enabled) focusReturn.restore()
        }
    }
    val indicatorRotation by animateFloatAsState(if (expanded && enabled) 0f else 180f, if (motionEnabled) spring(dampingRatio = 1.1068f, stiffness = 250f) else tween(0))
    Box(modifier) {
        Row(
            focusReturn.modifier.then(if (compact) Modifier else Modifier.fillMaxWidth()).heightIn(min = 48.dp)
                .onSizeChanged { anchorWidth = it.width }
                .alpha(if (enabled) 1f else 0.5f).trailsShadow(shape, TrailsShadow.Field).clip(shape).background(colors.surface)
                .trailsFocusRing(interaction, shape, enabled)
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button) { expanded = true }
                .semantics { text = AnnotatedString(label); stateDescription = optionLabel(selected) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                optionLabel(selected), Modifier.weight(1f, fill = !compact).testTag("trails-select-label").clearAndSetSemantics {},
                style = TrailsTheme.typography.bodyLarge, color = colors.textPrimary,
            )
            Icon(Icons.Outlined.ArrowUp.painter, null, Modifier.size(16.dp).testTag("trails-select-indicator").rotate(indicatorRotation), tint = colors.textSecondary)
        }
        if (popupMounted) {
            val overlayShape = RoundedCornerShape(24.dp)
            val selectedFocus = remember { FocusRequester() }
            val provider = remember(anchorGap, viewportInset, windowSize) {
                TrailsSelectPopupPositionProvider(anchorGap, viewportInset) { above, maximumSize, ready ->
                    aboveAnchor = above
                    maximumPopupSize = maximumSize
                    positioned = ready
                }
            }
            Popup(
                popupPositionProvider = provider,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                val popupFocus = LocalFocusManager.current
                LaunchedEffect(positioned) {
                    if (positioned && isOpen && options.isNotEmpty()) selectedFocus.requestFocus()
                }
                val maximumWidth = with(density) { maximumPopupSize.width.toDp() }
                val maximumHeight = with(density) { maximumPopupSize.height.toDp() }
                val minimumWidth = with(density) { anchorWidth.toDp() }.coerceAtMost(maximumWidth)
                val translateDistance = with(density) { 8.dp.toPx() }
                Column(
                    Modifier.widthIn(min = minimumWidth, max = maximumWidth).width(IntrinsicSize.Max)
                        .heightIn(max = maximumHeight)
                        .graphicsLayer {
                            alpha = progress.value
                            // Native exit only fades/translates; it must not shrink the menu.
                            val scale = if (isOpen) 0.97f + 0.03f * progress.value else 1f
                            scaleX = scale
                            scaleY = scale
                            translationY = (if (aboveAnchor) translateDistance else -translateDistance) * (1f - progress.value)
                        }
                        .trailsShadow(overlayShape, TrailsShadow.Overlay).clip(overlayShape).background(colors.surface)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) false
                            else when (event.key) {
                                Key.Escape -> { expanded = false; true }
                                Key.DirectionDown -> { if (isOpen) popupFocus.moveFocus(FocusDirection.Next); true }
                                Key.DirectionUp -> { if (isOpen) popupFocus.moveFocus(FocusDirection.Previous); true }
                                else -> false
                            }
                        }
                        .verticalScroll(rememberScrollState()).selectableGroup()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .testTag("trails-select-popup"),
                ) {
                    val initialFocusIndex = options.indexOf(selected).coerceAtLeast(0)
                    options.forEachIndexed { index, option ->
                        val itemInteraction = remember { MutableInteractionSource() }
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .then(if (index == initialFocusIndex) Modifier.focusRequester(selectedFocus) else Modifier)
                                .trailsFocusRing(itemInteraction, RectangleShape, isOpen)
                                .selectable(option == selected, interactionSource = itemInteraction, indication = null, enabled = isOpen, role = Role.RadioButton) {
                                    expanded = false
                                    onSelected(option)
                                }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(optionLabel(option), Modifier.weight(1f), style = TrailsTheme.typography.titleSmall, color = colors.textPrimary)
                            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                                if (option == selected) Icon(Icons.Solid.Check.painter, null, Modifier.size(20.dp), tint = colors.accent)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Native centered placement, viewport inset, and above-anchor fallback. */
internal class TrailsSelectPopupPositionProvider(
    private val anchorGap: Int,
    private val viewportInset: Int,
    private val onPositioned: (above: Boolean, maximumSize: IntSize, ready: Boolean) -> Unit = { _, _, _ -> },
) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val spaceBelow = (windowSize.height - viewportInset - anchorBounds.bottom - anchorGap).coerceAtLeast(0)
        val spaceAbove = (anchorBounds.top - anchorGap - viewportInset).coerceAtLeast(0)
        val maximumSize = IntSize(
            (windowSize.width - 2 * viewportInset).coerceAtLeast(1),
            maxOf(spaceAbove, spaceBelow).coerceAtLeast(1),
        )
        val above = popupContentSize.height > spaceBelow && (popupContentSize.height <= spaceAbove || spaceAbove > spaceBelow)
        val x = (anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2)
            .coerceIn(viewportInset, (windowSize.width - viewportInset - popupContentSize.width).coerceAtLeast(viewportInset))
        val desiredY = if (above) anchorBounds.top - anchorGap - popupContentSize.height else anchorBounds.bottom + anchorGap
        val y = desiredY.coerceIn(viewportInset, (windowSize.height - viewportInset - popupContentSize.height).coerceAtLeast(viewportInset))
        onPositioned(above, maximumSize, popupContentSize.width <= maximumSize.width && popupContentSize.height <= maximumSize.height)
        return IntOffset(x, y)
    }
}
