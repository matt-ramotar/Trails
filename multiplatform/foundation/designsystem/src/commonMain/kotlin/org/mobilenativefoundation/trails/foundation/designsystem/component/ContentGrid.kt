package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import kotlin.random.Random

data class ContentGridItem(
    val id: String,
    val type: ContentItemType,
    val thumbnailUrl: String?,
    val duration: Int?, // seconds for videos
    val views: Int,
    val likes: Int,
    val emoji: String? = null,
    val title: String? = null
)

enum class ContentItemType {
    VIDEO,
    PHOTO,
    GUIDE,
    GEAR
}

/** 2-column masonry layout. Item IDs seed the variation in height. */
@Composable
fun ContentGrid(
    items: List<ContentGridItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val baseHeight = 180.dp

    val leftColumnItems = items.filterIndexed { index, _ -> index % 2 == 0 }
    val rightColumnItems = items.filterIndexed { index, _ -> index % 2 == 1 }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            leftColumnItems.forEach { item ->
                val heightMultiplier = getHeightMultiplier(item.id)
                ContentGridItemCard(
                    item = item,
                    height = baseHeight * heightMultiplier,
                    onClick = { onItemClick(item.id) }
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            rightColumnItems.forEach { item ->
                val heightMultiplier = getHeightMultiplier(item.id)
                ContentGridItemCard(
                    item = item,
                    height = baseHeight * heightMultiplier,
                    onClick = { onItemClick(item.id) }
                )
            }
        }
    }
}

/** Item ID determines a stable height multiplier: 1.0f, 1.3f, or 1.6f. */
private fun getHeightMultiplier(itemId: String): Float {
    val seed = itemId.hashCode()
    val random = Random(seed)
    return when (random.nextInt(3)) {
        0 -> 1.0f
        1 -> 1.3f
        else -> 1.6f
    }
}

@Composable
private fun ContentGridItemCard(
    item: ContentGridItem,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A5F),
                        Color(0xFF0F1F35)
                    )
                )
            )
            .clickable(onClick = onClick)
    ) {
        if (item.emoji != null) {
            Text(
                text = item.emoji,
                fontSize = 48.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (item.type == ContentItemType.VIDEO && item.duration != null) {
            DurationBadge(
                duration = item.duration,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }

        if (item.type == ContentItemType.VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .size(28.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = Icons.Outlined.Play.painter,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        when (item.type) {
            ContentItemType.GUIDE -> {
                TypeBadge(
                    icon = Icons.Outlined.Alert,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }
            ContentItemType.GEAR -> {
                TypeBadge(
                    icon = Icons.Outlined.Settings,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }
            else -> {  }
        }
    }
}

@Composable
private fun DurationBadge(
    duration: Int, // in seconds
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = formatDuration(duration),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TypeBadge(
    icon: org.mobilenativefoundation.trails.foundation.designsystem.icon.Icon,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon.painter,
            contentDescription = icon.contentDescription,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
    } else {
        "0:${remainingSeconds.toString().padStart(2, '0')}"
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> "${count / 1000000}M"
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}
