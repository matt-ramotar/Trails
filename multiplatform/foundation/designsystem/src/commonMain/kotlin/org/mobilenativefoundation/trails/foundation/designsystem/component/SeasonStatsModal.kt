package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme
import trails.multiplatform.foundation.designsystem.generated.resources.Res
import trails.multiplatform.foundation.designsystem.generated.resources.cancel_01_stroke_rounded

@Composable
fun SeasonStatsModal(
    daysOnMountain: Int,
    totalVertical: Int,
    topSpeed: Int,
    powderDays: Int,
    currentStreak: Int,
    resortsVisited: Int,
    statesVisited: Int,
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
                .background(
                    color = Color(0xFF0f172a),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .clickable(onClick = {}, enabled = false)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2024-25 Season Stats",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(Res.drawable.cancel_01_stroke_rounded),
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Stats Grid - Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SeasonStatCard(
                    label = "Days on Mountain",
                    value = daysOnMountain.toString(),
                    modifier = Modifier.weight(1f)
                )
                SeasonStatCard(
                    label = "Total Vertical",
                    value = "${totalVertical.formatWithCommas()} ft",
                    modifier = Modifier.weight(1f)
                )
            }

            // Stats Grid - Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SeasonStatCard(
                    label = "Top Speed",
                    value = "$topSpeed mph",
                    modifier = Modifier.weight(1f)
                )
                SeasonStatCard(
                    label = "Powder Days",
                    value = powderDays.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            // Stats Grid - Row 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SeasonStatCard(
                    label = "Current Streak",
                    value = "$currentStreak days",
                    modifier = Modifier.weight(1f)
                )
                SeasonStatCard(
                    label = "Resorts Visited",
                    value = "$resortsVisited resorts",
                    subtitle = "$statesVisited states",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SeasonStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
            .glassCard(shape = TrailsTheme.shapes.medium)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF38BDF8)
        )
        Text(
            text = value,
            fontSize = 18.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

private fun Int.formatWithCommas(): String {
    return toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
}
