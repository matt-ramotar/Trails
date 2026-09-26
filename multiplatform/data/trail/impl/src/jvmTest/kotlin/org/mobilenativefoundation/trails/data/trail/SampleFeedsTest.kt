package org.mobilenativefoundation.trails.data.trail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SampleFeedsTest {
    private val now = 1_790_000_000_000L

    @Test
    fun sampleActivitiesUseCatalogRoutesAndCountBackFromNow() {
        val activities = sampleActivities(now)
        assertEquals(6, activities.size)
        assertEquals(6, activities.map { it.id }.toSet().size)
        activities.forEach { activity ->
            val trail = worldTrails.single { it.id == activity.trailId }
            assertEquals(trail.name, activity.trailName)
            assertEquals(trail.distanceMeters, activity.distanceMeters)
            assertEquals(trail.durationMinutes, activity.durationMinutes)
            assertEquals(trail.elevationMeters, activity.elevationMeters)
            assertTrue(activity.completedAtEpochMillis < now)
        }
        assertEquals("half-dome", activities.first().trailId)
        assertEquals(now - 86_400_000L, activities.first().completedAtEpochMillis)
        assertTrue(activities.zipWithNext().all { (newer, older) -> newer.completedAtEpochMillis > older.completedAtEpochMillis })
    }

    @Test
    fun forYouFixtureResolvesInTheCatalog() {
        val ids = worldTrails.map { it.id }.toSet()
        assertTrue(sampleForYou.featuredTrailId in ids)
        assertTrue(sampleForYou.anchorTrailId in ids)
        assertTrue(sampleForYou.recommendedTrailIds.all { it in ids })
        assertEquals(sampleForYou.recommendedTrailIds.size, sampleForYou.recommendedTrailIds.toSet().size)
        assertTrue(sampleForYou.anchorTrailId !in sampleForYou.recommendedTrailIds)
        assertTrue(sampleForYou.featuredTrailId != sampleForYou.anchorTrailId)
    }
}
