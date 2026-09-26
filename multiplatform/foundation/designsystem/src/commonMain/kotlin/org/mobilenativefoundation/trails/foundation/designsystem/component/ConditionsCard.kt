package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@Composable
fun ConditionsCard(
    conditions: String,
    temperature: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .glassCard(shape = TrailsTheme.shapes.large)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "Conditions",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = conditions,
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = temperature,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
