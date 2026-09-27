package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

enum class SurfaceVariant { Default, Secondary, Tertiary, Transparent }
enum class ChipTone { Default, Accent, Success, Warning, Danger }
enum class ChipSize { Small, Medium, Large }
enum class TrailsShadow { Surface, Overlay, Field }

/** Pinned Native light shadow layers; dark surfaces are flat and overlays carry a soft inner edge. */
@Composable
fun Modifier.trailsShadow(shape: Shape, shadow: TrailsShadow = TrailsShadow.Surface): Modifier {
    if (TrailsTheme.colors.background.luminance() < 0.5f) {
        return if (shadow == TrailsShadow.Overlay) innerShadow(shape, Shadow(1.dp, Color.White.copy(alpha = 0.2f))) else this
    }
    return when (shadow) {
        TrailsShadow.Surface, TrailsShadow.Field -> this
            .dropShadow(shape, Shadow(4.dp, Color.Black.copy(alpha = 0.04f), offset = DpOffset(0.dp, 2.dp)))
            .dropShadow(shape, Shadow(2.dp, Color.Black.copy(alpha = 0.06f), offset = DpOffset(0.dp, 1.dp)))
            .dropShadow(shape, Shadow(1.dp, Color.Black.copy(alpha = 0.06f)))
        TrailsShadow.Overlay -> this
            .dropShadow(shape, Shadow(8.dp, Color.Black.copy(alpha = 0.02f), offset = DpOffset(0.dp, 2.dp)))
            .dropShadow(shape, Shadow(12.dp, Color.Black.copy(alpha = 0.01f), offset = DpOffset(0.dp, (-6).dp)))
            .dropShadow(shape, Shadow(28.dp, Color.Black.copy(alpha = 0.03f), offset = DpOffset(0.dp, 14.dp)))
    }
}

@Composable
private fun surfaceColor(variant: SurfaceVariant): Color = when (variant) {
    SurfaceVariant.Default -> TrailsTheme.colors.surface
    SurfaceVariant.Secondary -> TrailsTheme.colors.soft
    SurfaceVariant.Tertiary -> TrailsTheme.colors.border
    SurfaceVariant.Transparent -> Color.Transparent
}

@Composable
private fun Modifier.surface(variant: SurfaceVariant): Modifier {
    val shape = RoundedCornerShape(24.dp)
    return then(if (variant == SurfaceVariant.Transparent) Modifier else Modifier.trailsShadow(shape))
        .clip(shape).background(surfaceColor(variant))
}

@Composable
fun TrailsSurface(
    modifier: Modifier = Modifier,
    variant: SurfaceVariant = SurfaceVariant.Default,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides TrailsTheme.colors.textPrimary) {
        ProvideTextStyle(TrailsTheme.typography.bodyLarge) {
            Column(modifier.surface(variant).padding(contentPadding), content = content)
        }
    }
}

@Composable
fun TrailsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    variant: SurfaceVariant = SurfaceVariant.Default,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val surfaceModifier = modifier.then(
        if (onClick == null) Modifier else Modifier.heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .trailsPressFeedback(interaction, enabled, shape = RoundedCornerShape(24.dp))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
    ).surface(variant)
    CompositionLocalProvider(LocalContentColor provides TrailsTheme.colors.textPrimary) {
        ProvideTextStyle(TrailsTheme.typography.bodyLarge) {
            Column(surfaceModifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun TrailsListGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    TrailsSurface(modifier, contentPadding = PaddingValues(0.dp), content = content)

@Composable
fun TrailsListItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val action = if (onClick == null) Modifier else Modifier
        .trailsPressFeedback(interaction, enabled)
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
    CompositionLocalProvider(LocalContentColor provides TrailsTheme.colors.textPrimary) {
        ProvideTextStyle(TrailsTheme.typography.titleSmall) {
            Row(
                modifier.fillMaxWidth().heightIn(min = 48.dp).alpha(if (enabled) 1f else 0.5f)
                    .then(action).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                leading?.invoke()
                Column(Modifier.weight(1f), content = content)
                trailing?.invoke()
            }
        }
    }
}

/** Informational Native Chip; selectable chips use [TrailsFilterChip]. */
@Composable
fun TrailsChip(
    label: String,
    modifier: Modifier = Modifier,
    tone: ChipTone = ChipTone.Default,
    size: ChipSize = ChipSize.Medium,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val colors = TrailsTheme.colors
    val foreground = when (tone) {
        ChipTone.Default -> colors.textPrimary
        ChipTone.Accent, ChipTone.Success -> colors.accent
        ChipTone.Warning -> colors.warning
        ChipTone.Danger -> colors.danger
    }
    val background = if (tone == ChipTone.Default) colors.soft else foreground.copy(alpha = 0.15f)
    val (horizontal, vertical, radius) = when (size) {
        ChipSize.Small -> Triple(8.dp, 2.dp, 12.dp)
        ChipSize.Medium -> Triple(12.dp, 4.dp, 16.dp)
        ChipSize.Large -> Triple(16.dp, 6.dp, 24.dp)
    }
    val (fontSize, lineHeight) = when (size) {
        ChipSize.Small -> 12.sp to 16.sp
        ChipSize.Medium -> 14.sp to 20.sp
        ChipSize.Large -> 16.sp to 24.sp
    }
    CompositionLocalProvider(LocalContentColor provides foreground) {
        Row(
            modifier.clip(RoundedCornerShape(radius)).background(background).padding(horizontal, vertical),
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingContent?.invoke()
            Text(label, style = TrailsTheme.typography.labelMedium.copy(fontSize = fontSize, lineHeight = lineHeight, fontWeight = FontWeight.Medium), color = foreground)
        }
    }
}

@Composable
fun TrailsSeparator(modifier: Modifier = Modifier, vertical: Boolean = false) {
    val hairline = with(LocalDensity.current) { 1.toDp() }
    Spacer(modifier.then(if (vertical) Modifier.fillMaxHeight().width(hairline) else Modifier.fillMaxWidth().height(hairline)).background(TrailsTheme.colors.border))
}

@Composable
fun TrailsTextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TrailsButton(text, onClick, modifier, tone = ButtonTone.Ghost, enabled = enabled)
}

@Composable
fun TrailsLinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TrailsPressable(modifier, onClick = onClick) {
        Text(text, Modifier.padding(vertical = 12.dp), color = TrailsTheme.colors.textPrimary, style = TrailsTheme.typography.labelLarge)
    }
}
