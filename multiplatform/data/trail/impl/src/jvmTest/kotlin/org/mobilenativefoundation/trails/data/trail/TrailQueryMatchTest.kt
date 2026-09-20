package org.mobilenativefoundation.trails.data.trail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class TrailQueryMatchTest {
    @Test
    fun strenuousFilterSelectsExactlyTheEightTreks() {
        val query = TrailQuery(difficulties = setOf(TrailDifficulty.STRENUOUS)).normalized()
        val strenuous = worldTrails.filter { it.matches(query) }
        assertEquals(worldTrails.takeLast(8).map { it.id }, strenuous.map { it.id })
        assertTrue(strenuous.all { it.durationMinutes >= 1440 })
    }

    @Test
    fun activitySelectorsMatchAnySelectedActivityAndCombineWithDogFriendly() {
        val backpacking = TrailQuery(activities = setOf(TrailActivity.BACKPACKING)).normalized()
        assertEquals(worldTrails.takeLast(8).map { it.id }, worldTrails.filter { it.matches(backpacking) }.map { it.id })
        val dogFriendly = TrailQuery(dogFriendly = true, activities = setOf(TrailActivity.HIKING, TrailActivity.BACKPACKING)).normalized()
        val matched = worldTrails.filter { it.matches(dogFriendly) }
        assertEquals(12, matched.size)
        assertTrue(matched.all { TrailFeature.DOG_FRIENDLY in it.features })
    }

    @Test
    fun elevationGainBoundsAreInclusive() {
        val query = TrailQuery(minElevationGain = 1463, maxElevationGain = 1463).normalized()
        assertEquals(listOf("half-dome"), worldTrails.filter { it.matches(query) }.map { it.id })
    }

    @Test
    fun mostPopularKeepsCatalogOrderAndTheOtherSortsReorderIt() {
        assertEquals(worldTrails.map { it.id }, worldTrails.sortedWith(TrailSort.MOST_POPULAR.comparator()).map { it.id })
        assertEquals("diamond-head-summit-trail", worldTrails.sortedWith(TrailSort.SHORTEST.comparator()).first().id)
        assertEquals("tour-du-mont-blanc", worldTrails.sortedWith(TrailSort.LONGEST.comparator()).first().id)
        assertEquals(4.9, worldTrails.sortedWith(TrailSort.HIGHEST_RATED.comparator()).first().rating)
    }

    @Test
    fun defaultSelectorsKeepTheExistingCacheKey() {
        assertEquals("{}", Json.encodeToString(TrailQuery.serializer(), TrailQuery().normalized()))
    }

    @Test
    fun legacyTrailJsonDecodesWithR2Defaults() {
        val legacy = """{"id":"x","name":"X","region":"R","description":"d","difficulty":"EASY","distanceMeters":1000,"elevationMeters":10,"durationMinutes":20,"rating":4.0,"reviewCount":1,"features":[],"photoIndex":0}"""
        assertEquals(emptySet(), Json.decodeFromString(Trail.serializer(), legacy).activities)
    }
}
