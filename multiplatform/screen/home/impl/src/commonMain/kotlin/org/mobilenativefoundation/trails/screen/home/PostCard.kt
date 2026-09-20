package org.mobilenativefoundation.trails.screen.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.mobilenativefoundation.trails.foundation.designsystem.Emojis
import org.mobilenativefoundation.trails.model.domain.feed.BackgroundGradient
import org.mobilenativefoundation.trails.model.domain.feed.Emoji
import org.mobilenativefoundation.trails.model.domain.feed.FeedPost
import org.mobilenativefoundation.trails.foundation.designsystem.component.*
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import org.mobilenativefoundation.trails.foundation.designsystem.util.CountFormatUtils.formatCount

@Composable
fun PostCard(
    post: FeedPost,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        BackgroundGradient(
            gradient = post.backgroundGradient,
            emoji = post.emoji
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC0a1628),
                            Color.Transparent,
                            Color.Transparent,
                            Color(0xF00a1628)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                DifficultyBadge(
                    difficulty = post.difficulty,
                    animated = true
                )
                ConditionsCard(
                    conditions = post.conditions,
                    temperature = post.temperature
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatsGrid(
                    distance = post.distance,
                    vertical = post.vertical,
                    duration = post.duration,
                    topSpeed = post.topSpeed
                )

                RunInfoCard(
                    runName = post.runName,
                    resort = post.resort,
                    region = post.location.split(", ").getOrElse(1) { post.location },
                    liftAccess = post.liftAccess,
                )

                UserInfoRow(
                    displayName = post.displayName,
                    username = post.username,
                    avatar = post.avatar,
                    verified = post.verified,
                    isFollowing = post.isFollowing,
                    onFollowClick = onFollowClick
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            // TODO: Action button with circular avatar of the author of the post with a circle "plus" icon button in red that will follow the author of the post

            val favoriteIcon = if (post.isLiked) Icons.Solid.Favorite else Icons.Outlined.Favorite
            ActionButton(
                icon = favoriteIcon,
                contentDescription = "Like",
                count = post.likes.formatCount(),
                isActive = post.isLiked,
                activeColor = TrailsTheme.colors.attention,
                onClick = onLikeClick
            )
            ActionButton(
                icon = Icons.Outlined.BubbleChat,
                contentDescription = "Comment",
                count = post.comments.formatCount(),
                onClick = onCommentClick
            )
            ActionButton(
                icon = Icons.Outlined.Bookmark,
                contentDescription = "Bookmark",
                count = post.shares.formatCount(),
                onClick = onShareClick // TODO: Refactor to "bookmark" action
            )

            // TODO: Action button with circular avatar of the logged in user that will navigate to the logged in user's profile
        }
    }
}

@Composable
private fun BackgroundGradient(
    emoji: Emoji,
    gradient: BackgroundGradient,
    modifier: Modifier = Modifier
) {
    val colors = when (gradient) {
        BackgroundGradient.CYAN_BLUE_INDIGO -> listOf(
            Color(0xFF0e7490),
            Color(0xFF1e40af),
            Color(0xFF4338ca)
        )

        BackgroundGradient.BLUE_SLATE_GRAY -> listOf(
            Color(0xFF1e40af),
            Color(0xFF1e293b),
            Color(0xFF111827)
        )

        BackgroundGradient.SKY_CYAN_BLUE -> listOf(
            Color(0xFF0c4a6e),
            Color(0xFF0e7490),
            Color(0xFF1e40af)
        )

        BackgroundGradient.BLUE_INDIGO_PURPLE -> listOf(
            Color(0xFF1e40af),
            Color(0xFF4338ca),
            Color(0xFF6b21a8)
        )

        BackgroundGradient.SLATE_BLUE_CYAN -> listOf(
            Color(0xFF0f172a),
            Color(0xFF1e40af),
            Color(0xFF0e7490)
        )

        BackgroundGradient.INDIGO_BLUE_CYAN -> listOf(
            Color(0xFF4338ca),
            Color(0xFF1e40af),
            Color(0xFF0e7490)
        )
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(colors)
                )
        )

        SnowflakeBackground()

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            FloatingEmoji(emoji)
        }
    }
}

@Composable
private fun FloatingEmoji(emoji: Emoji) {
    val infiniteTransition = rememberInfiniteTransition(label = "emoji_float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_offset"
    )

    val painter = when (emoji) {
        Emoji.Skier -> Emojis.Skier
        Emoji.AerialTramway -> Emojis.AerialTramway
        Emoji.SnowCappedMountain -> Emojis.SnowCappedMountain
    }

    Image(
        painter = painter,
        null,
        modifier = Modifier.size(320.dp).offset(y = offsetY.dp),
        alpha = 0.5f
    )
}

@Composable
private fun SnowflakeBackground() {
    // TODO: Implement animated snowflakes
}
