package org.mobilenativefoundation.trails.data.trail

import kotlin.math.abs
import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorldTrailsTest {
    @Test
    fun catalogHasFiftyDistinctValidTrails() {
        assertEquals(50, worldTrails.size)
        assertEquals(50, worldTrails.map { it.id }.toSet().size)
        assertEquals(50, worldTrails.map { it.name }.toSet().size)
        val faultReceiverId = Regex("[a-z0-9][a-z0-9-]{0,99}")
        worldTrails.forEach { trail ->
            assertTrue(faultReceiverId.matches(trail.id), trail.id)
            assertTrue(trail.name.isNotBlank(), trail.id)
            assertTrue(trail.region.contains(", "), trail.id)
            assertFalse(trail.region.contains("  "), trail.id)
            assertTrue(trail.description.isNotBlank(), trail.id)
            assertTrue(trail.distanceMeters > 0, trail.id)
            assertEquals(0, trail.distanceMeters % 100, trail.id)
            assertTrue(trail.elevationMeters > 0, trail.id)
            assertTrue(trail.durationMinutes > 0, trail.id)
            assertTrue(trail.rating in 4.5..4.9, trail.id)
            assertTrue(abs(trail.rating * 10 - round(trail.rating * 10)) < 0.000001, trail.id)
            assertTrue(trail.reviewCount in 100..9999, trail.id)
            assertTrue(trail.photoIndex in 0..2, trail.id)
            assertFalse(trail.reviewExcerpt.isNullOrBlank(), trail.id)
            assertTrue(TrailActivity.HIKING in trail.activities, trail.id)
            assertEquals(trail.durationMinutes >= 1440, TrailActivity.BACKPACKING in trail.activities, trail.id)
        }
        assertEquals(50, worldTrails.map { it.reviewExcerpt }.toSet().size)
        assertEquals(50, worldTrails.map { it.description }.toSet().size)
    }

    @Test
    fun recommendedOrderStartsWithPinnedDayHikesAndEndsWithEightTreks() {
        assertEquals(listOf("half-dome", "trolltunga"), worldTrails.take(2).map { it.id })
        assertTrue(worldTrails.take(42).all { it.durationMinutes < 1440 })
        assertTrue(worldTrails.takeLast(8).all { it.durationMinutes >= 1440 })
        assertEquals(8, worldTrails.count { it.durationMinutes >= 1440 })
        assertTrue(worldTrails.takeLast(8).all { it.durationMinutes % 1440 == 0 })
        assertTrue(worldTrails.take(42).none { it.difficulty == TrailDifficulty.STRENUOUS })
        assertTrue(worldTrails.takeLast(8).all { it.difficulty == TrailDifficulty.STRENUOUS })
        assertEquals(
            mapOf(TrailDifficulty.EASY to 5, TrailDifficulty.MODERATE to 16, TrailDifficulty.HARD to 21, TrailDifficulty.STRENUOUS to 8),
            worldTrails.groupingBy { it.difficulty }.eachCount(),
        )
        assertEquals(listOf("half-dome"), worldTrails.filter { it.matches(TrailQuery(text = "half dome").normalized()) }.map { it.id })
    }

    @Test
    fun yosemiteIsTheLargestRegionAndDogTagsRespectUsNationalParks() {
        val yosemite = worldTrails.filter { it.region == "Yosemite National Park, USA" }
        assertEquals(listOf("half-dome", "mist-trail-to-nevada-fall", "upper-yosemite-fall-trail"), yosemite.map { it.id })
        assertEquals(yosemite.size, worldTrails.groupingBy { it.region }.eachCount().values.maxOrNull())
        worldTrails.filter { "National Park, USA" in it.region }.forEach { trail ->
            assertFalse(TrailFeature.DOG_FRIENDLY in trail.features, trail.id)
        }
    }
}
