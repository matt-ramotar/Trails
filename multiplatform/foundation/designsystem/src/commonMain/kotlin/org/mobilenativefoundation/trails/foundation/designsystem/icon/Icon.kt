package org.mobilenativefoundation.trails.foundation.designsystem.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

interface Icon {
    val contentDescription: String

    val painter: Painter
        @Composable get
}