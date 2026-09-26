package org.mobilenativefoundation.trails.screen.explore

import kotlin.test.Test
import kotlin.test.assertEquals
import org.mobilenativefoundation.trails.data.trail.catalog.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.catalog.TrailFeature
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.data.trail.catalog.TrailSort

class ExploreQueryRestoreTest {
    @Test
    fun checkpointedTextAndRegionAreClearedWithoutLosingOtherSelectors() {
        val checkpoint = TrailQuery(
            text = "lake",
            region = "Yosemite National Park, USA",
            difficulties = setOf(TrailDifficulty.MODERATE),
            minMeters = 1000,
            maxMeters = 20000,
            features = setOf(TrailFeature.LAKE),
            sort = TrailSort.SHORTEST,
        )

        assertEquals(checkpoint.normalized().copy(text = "", region = null), restoredExploreSelectors(checkpoint))
    }

    @Test
    fun defaultCheckpointRestoresTheDefaultQuery() {
        assertEquals(TrailQuery(), restoredExploreSelectors(TrailQuery()))
    }

    @Test
    fun clearingFiltersDropsEverySelectorButKeepsTheChosenOrder() {
        val selectors = TrailQuery(
            difficulties = setOf(TrailDifficulty.MODERATE),
            minMeters = 1000,
            maxMeters = 20000,
            features = setOf(TrailFeature.LAKE),
            dogFriendly = true,
            sort = TrailSort.SHORTEST,
        )

        assertEquals(TrailQuery(sort = TrailSort.SHORTEST), clearedExploreSelectors(selectors))
        assertEquals(TrailQuery(), clearedExploreSelectors(TrailQuery()))
    }
}
