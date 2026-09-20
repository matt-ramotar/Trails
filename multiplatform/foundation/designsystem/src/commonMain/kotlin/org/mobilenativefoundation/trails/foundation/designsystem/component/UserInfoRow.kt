package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@Composable
fun UserInfoRow(
    displayName: String,
    username: String,
    avatar: String,
    verified: Boolean,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(8.dp, CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                TrailsTheme.colorScheme.primary,
                                Color(0xFF2563EB)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(avatar, contentDescription = null, contentScale = ContentScale.Crop)
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    if (verified) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(
                                    color = TrailsTheme.colors.badgeRare,
                                    shape = CircleShape
                                )
                                .padding(1.dp)
                            ,
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Solid.Check.painter, Icons.Solid.Check.contentDescription, tint = TrailsTheme.colorScheme.background)
                        }
                    }
                }
                Text(
                    text = "@$username",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .then(
                    if (!isFollowing) {
                        Modifier.background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    TrailsTheme.colorScheme.error,
                                    TrailsTheme.colorScheme.error,
                                )
                            ),
                            shape = RoundedCornerShape(999.dp)
                        )
                    } else {
                        Modifier.glassCard(shape = RoundedCornerShape(999.dp))
                    }
                )
                .clickable(onClick = onFollowClick)
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(
                text = if (isFollowing) "Following" else "Follow",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
