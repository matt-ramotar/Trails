package org.mobilenativefoundation.trails.foundation.designsystem.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

internal class IconProvider(
    override val contentDescription: String,
    private val provider: @Composable () -> Painter
) : Icon {
    override val painter: Painter
        @Composable get() = provider()
}