package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

/** Action hierarchy: Hero is the one action a screen exists for, Commit applies or confirms, Secondary sits beside a primary. */
enum class ButtonTone { Hero, Commit, Secondary, Ghost }

/** [accessibilityLabel] puts a modal invoker's label on the actionable node without a duplicate child. */
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
    val typography = TrailsTheme.typography
    val container = when (tone) {
        ButtonTone.Hero -> colors.citron
        ButtonTone.Commit -> colors.dark
        ButtonTone.Secondary -> colors.soft
        ButtonTone.Ghost -> Color.Transparent
    }
    val content = when (tone) {
        ButtonTone.Hero -> colors.onCitron
        ButtonTone.Commit -> colors.onDark
        ButtonTone.Secondary -> colors.textPrimary
        ButtonTone.Ghost -> colors.textPrimary
    }
    Button(
        onClick,
        modifier.heightIn(min = 52.dp).then(
            if (accessibilityLabel == null) Modifier
            else Modifier.semantics { this.text = AnnotatedString(accessibilityLabel) },
        ),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(TrailsTheme.radii.pill),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = if (tone == ButtonTone.Hero) colors.citron.copy(alpha = 0.55f) else colors.soft,
            disabledContentColor = colors.textSecondary,
        ),
    ) {
        if (loading) {
            // A loading button is disabled, so its spinner must follow the disabled content colour.
            CircularProgressIndicator(Modifier.size(18.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        } else if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, Modifier.size(18.dp), tint = content)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            modifier = if (accessibilityLabel == null) Modifier else Modifier.clearAndSetSemantics {},
            style = typography.labelLarge,
        )
    }
}

/** White 48 dp circle with a soft shadow for icon actions over photos, maps and headers. */
@Composable
fun TrailsIconCircle(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TrailsTheme.colors.textPrimary,
    container: Color = TrailsTheme.colors.surface,
) {
    IconButton(
        onClick,
        modifier
            .size(48.dp)
            .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.16f), spotColor = Color.Black.copy(alpha = 0.16f))
            .clip(CircleShape)
            .background(container),
    ) { Icon(icon, contentDescription, Modifier.size(22.dp), tint = tint) }
}
