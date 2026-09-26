package org.mobilenativefoundation.trails.screen.navigate

import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.data.trail.catalog.Trail
import org.mobilenativefoundation.trails.data.trail.catalog.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.catalog.TrailFeature
import org.mobilenativefoundation.trails.data.trail.saved.SavedSnapshot
import org.mobilenativefoundation.trails.data.trail.saved.TrailCollection

class NavigateTrailChoiceTest {
    private val trail = Trail("preikestolen", "Preikestolen", "Lysefjord, Norway", "Cliff.", TrailDifficulty.MODERATE, 8000, 350, 240, 4.8, 900, setOf(TrailFeature.SUMMIT), 2)
    private val snapshot = SavedSnapshot(
        collections = listOf(TrailCollection("weekend", "Weekend adventures")),
        memberships = mapOf("preikestolen" to setOf("weekend"), "half-dome" to emptySet()),
        trails = listOf(trail), syncByTrail = emptyMap(),
    )

    @Test
    fun lastOpenedWinsThenFirstSavedThenHalfDome() {
        assertEquals("trolltunga", navigateTrailId("trolltunga", snapshot))
        assertEquals("preikestolen", navigateTrailId(null, snapshot))
        assertEquals("half-dome", navigateTrailId(null, snapshot.copy(memberships = emptyMap())))
        assertEquals("half-dome", navigateTrailId(null, null))
    }
}
