package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** Trails action colors mapped onto Native's large Button. */
enum class ButtonTone { Hero, Commit, Secondary, Ghost }

@Composable
fun TrailsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ButtonTone = ButtonTone.Commit,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: Painter? = null,
    accessibilityLabel: String? = null,
) {
    val colors = TrailsTheme.colors
    val container = when (tone) {
        ButtonTone.Hero -> colors.citron
        ButtonTone.Commit -> colors.dark
        ButtonTone.Secondary -> colors.soft
        ButtonTone.Ghost -> Color.Transparent
    }
    val content = when (tone) {
        ButtonTone.Hero -> colors.onCitron
        ButtonTone.Commit -> colors.onDark
        ButtonTone.Secondary, ButtonTone.Ghost -> colors.textPrimary
    }
    val actionable = enabled && !loading
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier.heightIn(min = 56.dp).alpha(if (actionable) 1f else 0.5f)
            .trailsPressFeedback(interaction, actionable, highlight = true, shape = RoundedCornerShape(32.dp)).clip(RoundedCornerShape(32.dp))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, enabled = actionable, role = Role.Button, onClick = onClick)
            .semantics {
                accessibilityLabel?.let { this.text = AnnotatedString(it) }
                if (loading) stateDescription = "Loading"
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        // Keep the spinner composed so Native's 100 ms exit can finish when loading clears.
        TrailsSpinner(Modifier.clearAndSetSemantics {}, size = 16.dp, color = content, visible = loading)
        if (loading) {
            Spacer(Modifier.width(10.dp))
        } else if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, Modifier.size(20.dp), tint = content)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text,
            modifier = if (accessibilityLabel == null) Modifier else Modifier.clearAndSetSemantics {},
            style = TrailsTheme.typography.labelLarge.copy(fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
            color = content,
        )
    }
}

@Composable
fun TrailsIconCircle(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TrailsTheme.colors.textPrimary,
    container: Color = TrailsTheme.colors.surface,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier.size(48.dp).trailsPressFeedback(interaction, shape = CircleShape)
            .trailsShadow(CircleShape).clip(CircleShape).background(container)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(24.dp), tint = tint)
    }
}

@Composable
fun TrailsIconButton(icon: Painter, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier.size(48.dp).trailsPressFeedback(interaction, shape = CircleShape).clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(24.dp), tint = TrailsTheme.colors.textPrimary)
    }
}
