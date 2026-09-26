package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@Composable
fun ProfileActionButtons(
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onMessageClick: () -> Unit,
    onShareClick: () -> Unit,
    onEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isOwnProfile) {
            // Own profile: Edit + Settings + Share
            GlassActionButton(
                text = "Edit Profile",
                icon = Icons.Outlined.PencilEdit,
                onClick = onEditClick,
                modifier = Modifier.weight(1f)
            )

            GlassActionButton(
                text = "Settings",
                icon = Icons.Outlined.Settings,
                onClick = onSettingsClick,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Other user's profile: Follow + Message
            GradientActionButton(
                text = if (isFollowing) "Following" else "Follow",
                isActive = !isFollowing,
                onClick = onFollowClick,
                modifier = Modifier.weight(1f)
            )

            GlassActionButton(
                text = "Message",
                icon = Icons.Outlined.Mail,
                onClick = onMessageClick,
                modifier = Modifier.weight(1f)
            )
        }

        // Share button (always visible)
        GlassActionButton(
            icon = Icons.Outlined.Share,
            onClick = onShareClick,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun GradientActionButton(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .shadow(
                elevation = if (isActive) 8.dp else 0.dp,
                shape = RoundedCornerShape(999.dp)
            )
            .then(
                if (isActive) {
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF06b6d4), // Cyan
                                Color(0xFF3b82f6)  // Blue
                            )
                        ),
                        shape = RoundedCornerShape(999.dp)
                    )
                } else {
                    Modifier.background(
                        color = Color(0xFF374151),
                        shape = RoundedCornerShape(999.dp)
                    )
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun GlassActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: org.mobilenativefoundation.trails.foundation.designsystem.icon.Icon? = null
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .glassCard(shape = RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = if (text != null) 24.dp else 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    painter = icon.painter,
                    contentDescription = icon.contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (text != null) {
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
