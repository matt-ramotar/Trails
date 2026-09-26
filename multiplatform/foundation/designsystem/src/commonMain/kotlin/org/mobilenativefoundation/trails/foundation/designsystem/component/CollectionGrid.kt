package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CollectionItem(
    val id: String,
    val emoji: String,
    val name: String,
    val itemCount: Int,
    val itemType: String, // e.g., "skis", "items", "places", "guides", "runs", "spots"
    val gradientStart: String, // hex color
    val gradientEnd: String // hex color
)

@Composable
fun CollectionGrid(
    collections: List<CollectionItem>,
    onCollectionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false // Disable scroll since it's inside a parent scroll
    ) {
        items(collections) { collection ->
            CollectionCard(
                emoji = collection.emoji,
                name = collection.name,
                itemCount = collection.itemCount,
                itemType = collection.itemType,
                gradientStart = parseHexColor(collection.gradientStart),
                gradientEnd = parseHexColor(collection.gradientEnd),
                onClick = { onCollectionClick(collection.id) }
            )
        }
    }
}

@Composable
private fun CollectionCard(
    emoji: String,
    name: String,
    itemCount: Int,
    itemType: String,
    gradientStart: Color,
    gradientEnd: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .glassCard(shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(gradientStart, gradientEnd)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 24.sp
            )
        }

        Text(
            text = name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1
        )

        Text(
            text = "$itemCount $itemType",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun parseHexColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    return Color(cleanHex.toLong(16) or 0xFF000000)
}
