package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons
import org.mobilenativefoundation.trails.foundation.designsystem.theme.LocalSpacing
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@Composable
fun SeasonHighlightsCard(
    totalVertical: Int, // in feet
    topSpeed: Int, // in mph
    powderDays: Int,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg)
            .glassCard(shape = RoundedCornerShape(spacing.lgPlus))
            .padding(spacing.lgPlus),
        verticalArrangement = Arrangement.spacedBy(spacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Season Highlights",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .clickable(onClick = onViewAllClick)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View All",
                    color = TrailsTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    painter = Icons.Outlined.ArrowRight.painter,
                    contentDescription = Icons.Outlined.ArrowRight.contentDescription,
                    tint = TrailsTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatColumn(
                label = "Total Vertical",
                value = formatNumber(totalVertical),
                unit = "ft",
                gradientColors = listOf(
                    Color(0xFF06b6d4),
                    Color(0xFF3b82f6)
                ),
                modifier = Modifier.weight(1f)
            )

            StatColumn(
                label = "Top Speed",
                value = topSpeed.toString(),
                unit = "mph",
                gradientColors = listOf(
                    Color(0xFFfb923c),
                    Color(0xFFef4444)
                ),
                modifier = Modifier.weight(1f)
            )

            StatColumn(
                label = "Powder Days",
                value = powderDays.toString(),
                unit = "days",
                gradientColors = listOf(
                    Color(0xFF3b82f6),
                    Color(0xFF8b5cf6)
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    unit: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        brush = Brush.linearGradient(gradientColors),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                ) {
                    append(value)
                }
            }
        )

        Text(
            text = unit,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun formatNumber(number: Int): String {
    val str = number.toString()
    if (str.length <= 3) return str

    val reversed = str.reversed()
    val chunked = reversed.chunked(3).joinToString(",")
    return chunked.reversed()
}
