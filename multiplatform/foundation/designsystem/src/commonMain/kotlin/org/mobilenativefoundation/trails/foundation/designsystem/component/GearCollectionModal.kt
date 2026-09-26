package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import trails.multiplatform.foundation.designsystem.generated.resources.Res
import trails.multiplatform.foundation.designsystem.generated.resources.cancel_01_stroke_rounded

data class GearItemData(
    val id: String,
    val name: String,
    val type: String,
    val emoji: String,
    val specs: String,
    val rating: Float? = null
)

data class FavoriteSpotData(
    val id: String,
    val name: String,
    val type: String,
    val town: String,
    val rating: Float? = null
)

@Composable
fun GearCollectionModal(
    collectionName: String,
    gearItems: List<GearItemData>,
    favoriteSpots: List<FavoriteSpotData>,
    onAddGear: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .background(
                    color = Color(0xFF0f172a),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .clickable(onClick = {}, enabled = false)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = collectionName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = TrailsTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(onClick = onAddGear)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "+ Add Gear",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(Res.drawable.cancel_01_stroke_rounded),
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (gearItems.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gearItems.forEach { item ->
                        GearItemRow(item = item)
                    }
                }
            }

            if (favoriteSpots.isNotEmpty()) {
                HorizontalDivider(
                    color = Color(0xFF1E293B),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Text(
                    text = "⭐ Favorite Spots",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    favoriteSpots.forEach { spot ->
                        FavoriteSpotRow(spot = spot)
                    }
                }
            }
        }
    }
}

@Composable
private fun GearItemRow(
    item: GearItemData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .glassCard(shape = TrailsTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.emoji,
                fontSize = 32.sp
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.type,
                color = Color(0xFF94A3B8),
                fontSize = 13.sp
            )
            Text(
                text = item.specs,
                color = Color(0xFF38BDF8),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        if (item.rating != null) {
            RatingBadge(rating = item.rating)
        }
    }
}

@Composable
private fun FavoriteSpotRow(
    spot: FavoriteSpotData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(shape = TrailsTheme.shapes.medium)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = spot.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${spot.type} • ${spot.town}",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp
            )
        }

        if (spot.rating != null) {
            RatingBadge(rating = spot.rating)
        }
    }
}

@Composable
private fun RatingBadge(
    rating: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⭐",
            fontSize = 12.sp
        )
        Text(
            text = rating.toString(),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
