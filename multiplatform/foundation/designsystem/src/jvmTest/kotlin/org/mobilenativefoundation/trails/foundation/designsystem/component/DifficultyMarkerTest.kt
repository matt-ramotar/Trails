package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsExtendedColorsLight

class DifficultyMarkerTest {
    @Test
    fun eachKindMapsToItsR2Colour() {
        val colors = TrailsExtendedColorsLight
        assertEquals(Color(0xFF43A047), colors.difficulty(DifficultyMarkerKind.EASY))
        assertEquals(Color(0xFFF2B82E), colors.difficulty(DifficultyMarkerKind.MODERATE))
        assertEquals(Color(0xFFEE6A45), colors.difficulty(DifficultyMarkerKind.HARD))
        assertEquals(Color(0xFF5E3A27), colors.difficulty(DifficultyMarkerKind.STRENUOUS))
    }
}
