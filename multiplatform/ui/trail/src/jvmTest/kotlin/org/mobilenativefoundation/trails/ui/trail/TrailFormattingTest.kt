package org.mobilenativefoundation.trails.ui.trail

import kotlin.test.Test
import kotlin.test.assertEquals

class TrailFormattingTest {
    @Test
    fun subDayDurationsKeepTheirExistingLabels() {
        assertEquals("45 min", trailDuration(45))
        assertEquals("1 h 0 min", trailDuration(60))
        assertEquals("2 h 15 min", trailDuration(135))
        assertEquals("7 h 0 min", trailDuration(420))
        assertEquals("23 h 59 min", trailDuration(1439))
    }

    @Test
    fun trekDurationsRollOverIntoDaysAndWholeHours() {
        assertEquals("1 day", trailDuration(1440))
        assertEquals("1 day 1 h", trailDuration(1500))
        assertEquals("4 days", trailDuration(1440 * 4))
        assertEquals("4 days 6 h", trailDuration(1440 * 4 + 360))
    }
}
