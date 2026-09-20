package org.mobilenativefoundation.trails.screen.explore

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.feat.filters.FilterSection
import org.mobilenativefoundation.trails.foundation.designsystem.theme.TrailsTheme

@OptIn(ExperimentalTestApi::class)
class ExploreUiTest {
    private val trail = Trail("half-dome", "Half Dome", "Yosemite National Park, USA", "Granite summit.", TrailDifficulty.HARD, 22700, 1463, 660, 4.9, 2486, setOf(TrailFeature.WATERFALL, TrailFeature.SUMMIT), 2)

    @Test
    fun sortMenuSendsTheSelectedOrderAndFilterChipsOpenTheirSection() = runDesktopComposeUiTest {
        val sent = mutableListOf<ExploreIntent>()
        val state = ExploreState(text = "", query = TrailQuery(), results = LoadState(listOf(trail), loading = false), saved = LoadState(loading = false)) { sent += it }
        setContent { TrailsTheme { ExploreUi().Content(state, Modifier) } }
        onNodeWithText("Sort by Most popular").performClick()
        onNodeWithText("Highest rated").performClick()
        onNodeWithText("Length ⌄").performClick()
        assertEquals(ExploreIntent.Sort(TrailSort.HIGHEST_RATED), sent.filterIsInstance<ExploreIntent.Sort>().single())
        assertEquals(FilterSection.LENGTH, sent.filterIsInstance<ExploreIntent.Filters>().single().section)
    }
}
