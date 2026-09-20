package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme


data class StatItem(
    val label: String,
    val value: String
)

@Composable
fun StatsGrid(
    distance: String,
    vertical: String,
    duration: String,
    topSpeed: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Distance",
            value = distance,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Vertical",
            value = vertical,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Time",
            value = duration,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Top Speed",
            value = topSpeed,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .glassCard(shape = TrailsTheme.shapes.medium)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TrailsTheme.colorScheme.primary,
        )
        Text(
            text = value,
            fontSize = 15.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
