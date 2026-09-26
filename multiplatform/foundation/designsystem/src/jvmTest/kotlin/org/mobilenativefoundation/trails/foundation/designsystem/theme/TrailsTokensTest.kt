package org.mobilenativefoundation.trails.foundation.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class TrailsTokensTest {
    @Test
    fun lightTokensMatchDefaultPalette() {
        val colors = TrailsExtendedColorsLight
        assertEquals(Color(0xFFFFFFFF), colors.background)
        assertEquals(Color(0xFFFFFFFF), colors.surface)
        assertEquals(Color(0xFF171E14), colors.textPrimary)
        assertEquals(Color(0xFF545A52), colors.textSecondary)
        assertEquals(Color(0xFFE6E8E4), colors.border)
        assertEquals(Color(0xFF1D4B35), colors.accent)
        assertEquals(Color(0xFFA9F184), colors.citron)
        assertEquals(Color(0xFFF4F5F4), colors.soft)
        assertEquals(Color(0xFF0D1F18), colors.dark)
        assertEquals(Color(0xFF171E14), colors.onCitron)
        assertEquals(Color(0xFFFFFFFF), colors.onDark)
        assertEquals(Color(0xFF8A4B12), colors.warning)
        assertEquals(Color(0xFFA5352B), colors.danger)
        assertEquals(Color(0xFF43A047), colors.difficultyEasy)
        assertEquals(Color(0xFFF2B82E), colors.difficultyModerate)
        assertEquals(Color(0xFFEE6A45), colors.difficultyHard)
        assertEquals(Color(0xFF5E3A27), colors.difficultyStrenuous)
    }
}
